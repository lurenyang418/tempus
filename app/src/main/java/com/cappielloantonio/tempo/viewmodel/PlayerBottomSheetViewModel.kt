package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.model.LyricsCache
import com.cappielloantonio.tempo.model.Queue
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.LyricsRepository
import com.cappielloantonio.tempo.repository.OpenRepository
import com.cappielloantonio.tempo.repository.QueueRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.LyricsList
import com.cappielloantonio.tempo.subsonic.models.PlayQueue
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Constants.SeedType
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.OpenSubsonicExtensionsUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.isAutoDownloadLyricsEnabled
import com.cappielloantonio.tempo.util.Preferences.isStarredSyncEnabled
import com.google.gson.Gson
import java.util.Date
import java.util.stream.Collectors

@OptIn(markerClass = [UnstableApi::class])
class PlayerBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val queueRepository: QueueRepository
    private val favoriteRepository: FavoriteRepository
    private val openRepository: OpenRepository
    private val lyricsRepository: LyricsRepository
    private val lyricsLiveData = MutableLiveData<String?>(null)
    private val lyricsListLiveData = MutableLiveData<LyricsList?>(null)
    private val lyricsCachedLiveData = MutableLiveData<Boolean?>(false)
    private val descriptionLiveData = MutableLiveData<String?>(null)
    private val liveMedia = MutableLiveData<Child?>(null)
    private val liveAlbum = MutableLiveData<AlbumID3?>(null)
    private val liveArtist = MutableLiveData<ArtistID3?>(null)
    private val instantMix = MutableLiveData<MutableList<Child?>?>(null)
    private val gson = Gson()
    var syncLyricsState: Boolean = true
        private set
    private var cachedLyricsSource: LiveData<LyricsCache?>? = null
    private var currentSongId: String? = null
    private val cachedLyricsObserver =
        Observer { lyricsCache: LyricsCache? -> this.onCachedLyricsChanged(lyricsCache) }


    init {
        songRepository = SongRepository()
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        queueRepository = QueueRepository()
        favoriteRepository = FavoriteRepository()
        openRepository = OpenRepository()
        lyricsRepository = LyricsRepository()
    }

    val queueSong: LiveData<MutableList<Queue?>?>?
        get() = queueRepository.liveQueue

    fun setFavorite(context: Context?, media: Child?) {
        if (media != null) {
            if (media.starred != null) {
                if (NetworkUtil.isOffline) {
                    removeFavoriteOffline(media)
                } else {
                    removeFavoriteOnline(media)
                }
            } else {
                if (NetworkUtil.isOffline) {
                    setFavoriteOffline(media)
                } else {
                    setFavoriteOnline(context, media)
                }
            }
        }
    }

    private fun removeFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, false)
        media.starred = null
    }

    private fun removeFavoriteOnline(media: Child) {
        favoriteRepository.unstar(media.id, null, null, object : StarCallback {
            override fun onError() {
                // media.setStarred(new Date());
                favoriteRepository.starLater(media.id, null, null, false)
            }
        })
        media.starred = null
    }

    private fun setFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, true)
        media.starred = Date()
    }

    private fun setFavoriteOnline(context: Context?, media: Child) {
        favoriteRepository.star(media.id, null, null, object : StarCallback {
            override fun onError() {
                // media.setStarred(null);
                favoriteRepository.starLater(media.id, null, null, true)
            }
        })

        media.starred = Date()

        if (isStarredSyncEnabled() && getDownloadDirectoryUri() == null) {
            val safeContext = context ?: return
            DownloadUtil.getDownloadTracker(safeContext).download(
                MappingUtil.mapDownload(media),
                Download(media)
            )
        }
    }

    val liveLyrics: LiveData<String?>
        get() = lyricsLiveData

    val liveLyricsList: LiveData<LyricsList?>
        get() = lyricsListLiveData

    fun refreshMediaInfo(owner: LifecycleOwner?, media: Child?) {
        lyricsLiveData.postValue(null)
        lyricsListLiveData.postValue(null)
        lyricsCachedLiveData.postValue(false)

        clearCachedLyricsObserver()

        val songId = if (media != null) media.id else currentSongId

        if (TextUtils.isEmpty(songId) || owner == null) {
            return
        }

        currentSongId = songId

        observeCachedLyrics(owner, songId)

        val cachedLyrics = lyricsRepository.getLyrics(songId)
        if (cachedLyrics != null) {
            onCachedLyricsChanged(cachedLyrics)
        }

        if (NetworkUtil.isOffline || media == null) {
            return
        }

        if (OpenSubsonicExtensionsUtil.isSongLyricsExtensionAvailable) {
            openRepository.getLyricsBySongId(media.id)
                .observe(owner, Observer { lyricsList: LyricsList? ->
                    lyricsListLiveData.postValue(lyricsList)
                    lyricsLiveData.postValue(null)
                    if (shouldAutoDownloadLyrics() && hasStructuredLyrics(lyricsList)) {
                        saveLyricsToCache(media, null, lyricsList)
                    }
                })
        } else {
            songRepository.getSongLyrics(media).observe(owner, Observer { lyrics: String? ->
                lyricsLiveData.postValue(lyrics)
                lyricsListLiveData.postValue(null)
                if (shouldAutoDownloadLyrics() && !TextUtils.isEmpty(lyrics)) {
                    saveLyricsToCache(media, lyrics, null)
                }
            })
        }
    }

    fun getLiveMedia(): LiveData<Child?> {
        return liveMedia
    }

    fun setLiveMedia(owner: LifecycleOwner, mediaType: String?, mediaId: String?) {
        currentSongId = mediaId

        if (!TextUtils.isEmpty(mediaId)) {
            refreshMediaInfo(owner, null)
        } else {
            clearCachedLyricsObserver()
            lyricsLiveData.postValue(null)
            lyricsListLiveData.postValue(null)
            lyricsCachedLiveData.postValue(false)
        }

        if (mediaType != null) {
            when (mediaType) {
                Constants.MEDIA_TYPE_MUSIC -> {
                    songRepository.getSong(mediaId)
                        .observe(owner, Observer { value: Child? -> liveMedia.postValue(value) })
                    descriptionLiveData.postValue(null)
                }

                Constants.MEDIA_TYPE_PODCAST -> liveMedia.postValue(null)
                else -> liveMedia.postValue(null)
            }
        } else {
            liveMedia.postValue(null)
        }
    }

    fun getLiveAlbum(): LiveData<AlbumID3?> {
        return liveAlbum
    }

    fun setLiveAlbum(owner: LifecycleOwner, mediaType: String?, AlbumId: String?) {
        if (mediaType != null) {
            when (mediaType) {
                Constants.MEDIA_TYPE_MUSIC -> albumRepository.getAlbum(AlbumId)
                    .observe(owner, Observer { value: AlbumID3? -> liveAlbum.postValue(value) })

                Constants.MEDIA_TYPE_PODCAST -> liveAlbum.postValue(null)
            }
        }
    }

    fun getLiveArtist(): LiveData<ArtistID3?> {
        return liveArtist
    }

    fun setLiveArtist(owner: LifecycleOwner, mediaType: String?, ArtistId: String?) {
        if (mediaType != null) {
            when (mediaType) {
                Constants.MEDIA_TYPE_MUSIC -> artistRepository.getArtist(ArtistId)
                    .observe(owner, Observer { value: ArtistID3? -> liveArtist.postValue(value) })

                Constants.MEDIA_TYPE_PODCAST -> liveArtist.postValue(null)
            }
        }
    }

    fun setLiveDescription(description: String?) {
        descriptionLiveData.postValue(description)
    }

    val liveDescription: LiveData<String?>
        get() = descriptionLiveData

    fun getMediaInstantMix(owner: LifecycleOwner, media: Child): LiveData<MutableList<Child?>?> {
        instantMix.setValue(mutableListOf<Child?>())

        songRepository.getInstantMix(media.id, SeedType.TRACK, 20)
            .observe(owner, Observer { value: MutableList<Child?>? -> instantMix.postValue(value) })

        return instantMix
    }

    val playQueue: LiveData<PlayQueue?>
        get() = queueRepository.playQueue

    fun savePlayQueue(): Boolean {
        val media = getLiveMedia().getValue()
        val queue = queueRepository.media
        val ids: MutableList<String?> = queue.filterNotNull().map { it.id }.toMutableList()

        if (media != null) {
            // TODO: We need to get the actual playback position here
            Log.d(TAG, "Saving play queue - Current: " + media.id + ", Items: " + ids.size)
            queueRepository.savePlayQueue(ids, media.id, 0) // Still hardcoded to 0 for now
            return true
        }
        return false
    }

    private fun observeCachedLyrics(owner: LifecycleOwner, songId: String?) {
        if (TextUtils.isEmpty(songId)) {
            return
        }

        cachedLyricsSource = lyricsRepository.observeLyrics(songId)
        cachedLyricsSource!!.observe(owner, cachedLyricsObserver)
    }

    private fun clearCachedLyricsObserver() {
        if (cachedLyricsSource != null) {
            cachedLyricsSource!!.removeObserver(cachedLyricsObserver)
            cachedLyricsSource = null
        }
    }

    private fun onCachedLyricsChanged(lyricsCache: LyricsCache?) {
        if (lyricsCache == null) {
            lyricsCachedLiveData.postValue(false)
            return
        }

        lyricsCachedLiveData.postValue(true)

        if (!TextUtils.isEmpty(lyricsCache.structuredLyrics)) {
            try {
                val cachedList =
                    gson.fromJson<LyricsList?>(lyricsCache.structuredLyrics, LyricsList::class.java)
                lyricsListLiveData.postValue(cachedList)
                lyricsLiveData.postValue(null)
            } catch (exception: Exception) {
                lyricsListLiveData.postValue(null)
                lyricsLiveData.postValue(lyricsCache.lyrics)
            }
        } else {
            lyricsListLiveData.postValue(null)
            lyricsLiveData.postValue(lyricsCache.lyrics)
        }
    }

    private fun saveLyricsToCache(media: Child?, lyrics: String?, lyricsList: LyricsList?) {
        if (media == null) {
            return
        }

        if ((lyricsList == null || !hasStructuredLyrics(lyricsList)) && TextUtils.isEmpty(lyrics)) {
            return
        }

        val lyricsCache = LyricsCache(media.id)
        lyricsCache.artist = media.artist
        lyricsCache.title = media.title
        lyricsCache.updatedAt = System.currentTimeMillis()

        if (lyricsList != null && hasStructuredLyrics(lyricsList)) {
            lyricsCache.structuredLyrics = gson.toJson(lyricsList)
            lyricsCache.lyrics = null
        } else {
            lyricsCache.lyrics = lyrics
            lyricsCache.structuredLyrics = null
        }

        lyricsRepository.insert(lyricsCache)
        lyricsCachedLiveData.postValue(true)
    }

    private fun hasStructuredLyrics(lyricsList: LyricsList?): Boolean {
        return lyricsList != null && lyricsList.structuredLyrics != null && !lyricsList.structuredLyrics!!.isEmpty() && lyricsList.structuredLyrics!!.get(
            0
        ).line != null && !lyricsList.structuredLyrics!!.get(
            0
        ).line!!.isEmpty()
    }

    private fun shouldAutoDownloadLyrics(): Boolean {
        return isAutoDownloadLyricsEnabled()
    }

    fun downloadCurrentLyrics(): Boolean {
        val media = getLiveMedia().getValue()
        if (media == null) {
            return false
        }

        val lyricsList = lyricsListLiveData.getValue()
        val lyrics = lyricsLiveData.getValue()

        if ((lyricsList == null || !hasStructuredLyrics(lyricsList)) && TextUtils.isEmpty(lyrics)) {
            return false
        }

        saveLyricsToCache(media, lyrics, lyricsList)
        return true
    }

    val lyricsCachedState: LiveData<Boolean?>
        get() = lyricsCachedLiveData

    fun changeSyncLyricsState() {
        this.syncLyricsState = !this.syncLyricsState
    }

    companion object {
        private const val TAG = "PlayerBottomSheetViewModel"
    }
}
