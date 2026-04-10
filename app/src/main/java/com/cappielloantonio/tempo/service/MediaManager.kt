package com.cappielloantonio.tempo.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import com.cappielloantonio.tempo.interfaces.MediaIndexCallback
import com.cappielloantonio.tempo.model.Chronology
import com.cappielloantonio.tempo.repository.ChronologyRepository
import com.cappielloantonio.tempo.repository.QueueRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences.isContinuousPlayEnabled
import com.cappielloantonio.tempo.util.Preferences.isInstantMixUsable
import com.cappielloantonio.tempo.util.Preferences.isScrobblingEnabled
import com.cappielloantonio.tempo.util.Preferences.setLastInstantMix
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object MediaManager {
    private const val TAG = "MediaManager"
    private var attachedBrowserRef = WeakReference<MediaBrowser?>(null)
    var justStarted: AtomicBoolean = AtomicBoolean(false)

    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    @JvmStatic
    fun registerPlaybackObserver(
        browserFuture: ListenableFuture<MediaBrowser?>?,
        playbackViewModel: PlaybackViewModel
    ) {
        if (browserFuture == null) return

        Futures.addCallback<MediaBrowser?>(browserFuture, object : FutureCallback<MediaBrowser?> {
            override fun onSuccess(browser: MediaBrowser?) {
                val current = attachedBrowserRef.get()
                if (browser != null && current != browser) {
                    browser.addListener(object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) {
                            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                                || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                                || events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                            ) {
                                val mediaId = if (player.getCurrentMediaItem() != null)
                                    player.getCurrentMediaItem()!!.mediaId
                                else
                                    null

                                val playing = player.getPlaybackState() == Player.STATE_READY
                                        && player.getPlayWhenReady()

                                playbackViewModel.update(mediaId, playing)
                            }
                        }
                    })

                    val mediaId = if (browser.getCurrentMediaItem() != null)
                        browser.getCurrentMediaItem()!!.mediaId
                    else
                        null
                    val playing =
                        browser.getPlaybackState() == Player.STATE_READY && browser.getPlayWhenReady()
                    playbackViewModel.update(mediaId, playing)

                    attachedBrowserRef = WeakReference<MediaBrowser?>(browser)
                } else {
                    val mediaId = if (browser?.getCurrentMediaItem() != null)
                        browser.getCurrentMediaItem()!!.mediaId
                    else
                        null
                    val playing =
                        browser?.getPlaybackState() == Player.STATE_READY && browser?.getPlayWhenReady() == true
                    playbackViewModel.update(mediaId, playing)
                }
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "Failed to get MediaBrowser instance", t)
            }
        }, MoreExecutors.directExecutor())
    }

    fun onBrowserReleased(released: MediaBrowser?) {
        val attached = attachedBrowserRef.get()
        if (attached == released) {
            attachedBrowserRef.clear()
        }
    }

    @JvmStatic
    fun reset(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        if (mediaBrowserListenableFuture.get().isPlaying()) {
                            mediaBrowserListenableFuture.get().pause()
                        }

                        mediaBrowserListenableFuture.get().stop()
                        mediaBrowserListenableFuture.get().clearMediaItems()
                        clearDatabase()
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun hide(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        if (mediaBrowserListenableFuture.get().isPlaying()) {
                            mediaBrowserListenableFuture.get().pause()
                        }
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun check(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        if (mediaBrowserListenableFuture.get().getMediaItemCount() < 1) {
                            val media: MutableList<Child?>? = queueRepository.media
                            if (media != null && media.size >= 1) {
                                init(mediaBrowserListenableFuture, media)
                            }
                        }
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    fun init(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?,
        media: MutableList<Child?>
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        mediaBrowserListenableFuture.get().clearMediaItems()
                        mediaBrowserListenableFuture.get()
                            .setMediaItems(MappingUtil.mapMediaItems(media).filterNotNull())
                        mediaBrowserListenableFuture.get().seekTo(
                            queueRepository.lastPlayedMediaIndex,
                            queueRepository.lastPlayedMediaTimestamp
                        )
                        mediaBrowserListenableFuture.get().prepare()
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    @OptIn(markerClass = [UnstableApi::class])
    fun startQueue(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?,
        media: MutableList<Child?>,
        startIndex: Int
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        val browser = mediaBrowserListenableFuture.get()
                        val items = MappingUtil.mapMediaItems(media).filterNotNull()

                        Handler(Looper.getMainLooper()).post(Runnable {
                            justStarted.set(true)
                            browser.setMediaItems(items, startIndex, 0)
                            browser.prepare()

                            val timelineListener: Player.Listener = object : Player.Listener {
                                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                                    val itemCount = browser.getMediaItemCount()
                                    if (itemCount > 0 && startIndex >= 0 && startIndex < itemCount) {
                                        browser.seekTo(startIndex, 0)
                                        browser.play()
                                        browser.removeListener(this)
                                    } else {
                                        Log.d(
                                            TAG,
                                            "Cannot start playback: itemCount=" + itemCount + ", startIndex=" + startIndex
                                        )
                                    }
                                }
                            }
                            browser.addListener(timelineListener)
                        })

                        backgroundExecutor.execute(Runnable {
                            Log.d(TAG, "Background: enqueuing to database")
                            enqueueDatabase(media, true, 0)
                        })
                    }
                } catch (e: ExecutionException) {
                    Log.e(TAG, "Error in startQueue: " + e.message, e)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error in startQueue: " + e.message, e)
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun startQueue(mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?, media: Child?) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        val browser = mediaBrowserListenableFuture.get()
                        justStarted.set(true)
                        browser.setMediaItem(MappingUtil.mapMediaItem(media))
                        browser.prepare()
                        browser.play()
                        enqueueDatabase(media, true, 0)
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun playDownloadedMediaItem(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?,
        mediaItem: MediaItem?
    ) {
        if (mediaBrowserListenableFuture != null && mediaItem != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        val mediaBrowser = mediaBrowserListenableFuture.get()
                        justStarted.set(true)
                        mediaBrowser.setMediaItem(mediaItem)
                        mediaBrowser.prepare()
                        mediaBrowser.play()
                        clearDatabase()
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun startPodcast(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?,
        podcastEpisode: PodcastEpisode?
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        val browser = mediaBrowserListenableFuture.get()
                        justStarted.set(true)
                        browser.setMediaItem(MappingUtil.mapMediaItem(podcastEpisode!!))
                        browser.prepare()
                        browser.play()
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun enqueue(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?,
        media: MutableList<Child?>,
        playImmediatelyAfter: Boolean
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        Log.e(TAG, "enqueue")
                        val browser = mediaBrowserListenableFuture.get()
                        if (playImmediatelyAfter && browser.getNextMediaItemIndex() != -1) {
                            enqueueDatabase(media, false, browser.getNextMediaItemIndex())
                            browser.addMediaItems(
                                browser.getNextMediaItemIndex(),
                                MappingUtil.mapMediaItems(media).filterNotNull()
                            )
                        } else {
                            enqueueDatabase(
                                media,
                                false,
                                mediaBrowserListenableFuture.get().getMediaItemCount()
                            )
                            mediaBrowserListenableFuture.get()
                                .addMediaItems(MappingUtil.mapMediaItems(media).filterNotNull())
                        }
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun enqueue(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?,
        media: Child?,
        playImmediatelyAfter: Boolean
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        Log.e(TAG, "enqueue")
                        val browser = mediaBrowserListenableFuture.get()
                        if (playImmediatelyAfter && browser.getNextMediaItemIndex() != -1) {
                            enqueueDatabase(media, false, browser.getNextMediaItemIndex())
                            browser.addMediaItem(
                                browser.getNextMediaItemIndex(),
                                MappingUtil.mapMediaItem(media)
                            )
                        } else {
                            enqueueDatabase(
                                media,
                                false,
                                mediaBrowserListenableFuture.get().getMediaItemCount()
                            )
                            mediaBrowserListenableFuture.get()
                                .addMediaItem(MappingUtil.mapMediaItem(media))
                        }
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun shuffle(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>?,
        media: MutableList<Child?>,
        startIndex: Int,
        endIndex: Int
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        Log.e(TAG, "shuffle")
                        val browser = mediaBrowserListenableFuture.get()
                        browser.removeMediaItems(startIndex, endIndex + 1)
                        browser.addMediaItems(
                            MappingUtil.mapMediaItems(media).filterNotNull().subList(startIndex, endIndex + 1)
                        )
                        swapDatabase(media)
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun swap(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser?>?,
        media: MutableList<Child?>?,
        from: Int,
        to: Int
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        Log.e(TAG, "swap")
                        mediaBrowserListenableFuture.get()!!.moveMediaItem(from, to)
                        swapDatabase(media)
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun remove(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser?>?,
        media: MutableList<Child?>,
        toRemove: Int
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        Log.e(TAG, "remove")
                        if (mediaBrowserListenableFuture.get()!!
                                .getMediaItemCount() > 1 && mediaBrowserListenableFuture.get()!!
                                .getCurrentMediaItemIndex() != toRemove
                        ) {
                            mediaBrowserListenableFuture.get()!!.removeMediaItem(toRemove)
                            removeDatabase(media, toRemove)
                        } else {
                            removeDatabase(media, -1)
                        }
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun removeRange(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser?>?,
        media: MutableList<Child?>,
        fromItem: Int,
        toItem: Int
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        Log.e(TAG, "remove range")
                        mediaBrowserListenableFuture.get()!!.removeMediaItems(fromItem, toItem)
                        removeRangeDatabase(media, fromItem, toItem)
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    @JvmStatic
    fun getCurrentIndex(
        mediaBrowserListenableFuture: ListenableFuture<MediaBrowser?>?,
        callback: MediaIndexCallback
    ) {
        if (mediaBrowserListenableFuture != null) {
            mediaBrowserListenableFuture.addListener(Runnable {
                try {
                    if (mediaBrowserListenableFuture.isDone()) {
                        callback.onRecovery(
                            mediaBrowserListenableFuture.get()!!.getCurrentMediaItemIndex()
                        )
                    }
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    fun setLastPlayedTimestamp(mediaItem: MediaItem?) {
        if (mediaItem != null) queueRepository.setLastPlayedTimestamp(mediaItem.mediaId)
    }

    fun setPlayingPausedTimestamp(mediaItem: MediaItem?, ms: Long) {
        if (mediaItem != null) queueRepository.setPlayingPausedTimestamp(mediaItem.mediaId, ms)
    }

    fun scrobble(mediaItem: MediaItem?, submission: Boolean) {
        if (mediaItem != null && isScrobblingEnabled()) {
            songRepository.scrobble(mediaItem.mediaMetadata.extras!!.getString("id"), submission)
        }
    }

    @OptIn(markerClass = [UnstableApi::class])
    fun continuousPlay(
        mediaItem: MediaItem?,
        existingBrowserFuture: ListenableFuture<MediaBrowser>?
    ) {
        if (mediaItem == null || !isContinuousPlayEnabled() || !isInstantMixUsable()) {
            return
        }

        setLastInstantMix()

        val instantMix: LiveData<MutableList<Child?>?> =
            songRepository.getContinuousMix(mediaItem.mediaId, 25)

        instantMix.observeForever(object : Observer<MutableList<Child?>?> {
            override fun onChanged(media: MutableList<Child?>?) {
                if (media == null || media.isEmpty()) {
                    return
                }

                if (existingBrowserFuture != null) {
                    Log.d(TAG, "Continuous play: adding " + media.size + " tracks")
                    enqueue(existingBrowserFuture, media, true)
                }
                instantMix.removeObserver(this)
            }
        })
    }

    fun saveChronology(mediaItem: MediaItem?) {
        if (mediaItem != null) {
            chronologyRepository.insert(Chronology(mediaItem))
        }
    }

    private val queueRepository: QueueRepository
        get() = QueueRepository()

    private val songRepository: SongRepository
        get() = SongRepository()

    private val chronologyRepository: ChronologyRepository
        get() = ChronologyRepository()

    private fun enqueueDatabase(media: MutableList<Child?>?, reset: Boolean, afterIndex: Int) {
        queueRepository.insertAll(media!!, reset, afterIndex)
    }

    private fun enqueueDatabase(media: Child?, reset: Boolean, afterIndex: Int) {
        queueRepository.insert(media!!, reset, afterIndex)
    }

    private fun swapDatabase(media: MutableList<Child?>?) {
        queueRepository.insertAll(media!!, true, 0)
    }

    private fun removeDatabase(media: MutableList<Child?>, toRemove: Int) {
        if (toRemove != -1) {
            media.removeAt(toRemove)
            queueRepository.insertAll(media, true, 0)
        }
    }

    private fun removeRangeDatabase(media: MutableList<Child?>, fromItem: Int, toItem: Int) {
        val toRemove = media.subList(fromItem, toItem)

        media.removeAll(toRemove)

        queueRepository.insertAll(media, true, 0)
    }

    fun clearDatabase() {
        queueRepository.deleteAll()
    }
}
