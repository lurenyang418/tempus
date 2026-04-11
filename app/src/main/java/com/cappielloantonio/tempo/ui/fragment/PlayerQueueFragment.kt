package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.InnerFragmentPlayerQueueBinding
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.model.Queue
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PlayQueue
import com.cappielloantonio.tempo.ui.adapter.PlayerSongQueueAdapter
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader.getUri
import com.cappielloantonio.tempo.util.ExternalAudioWriter.downloadToUserDirectory
import com.cappielloantonio.tempo.util.MappingUtil.mapMediaItems
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.isSyncronizationEnabled
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.Collections
import java.util.stream.Collectors

@UnstableApi
class PlayerQueueFragment : Fragment(), ClickCallback {
    private var bind: InnerFragmentPlayerQueueBinding? = null

    private var fabMenuToggle: FloatingActionButton? = null
    private var fabClearQueue: ExtendedFloatingActionButton? = null
    private var fabShuffleQueue: ExtendedFloatingActionButton? = null

    private var fabSaveToPlaylist: ExtendedFloatingActionButton? = null
    private var fabDownloadAll: ExtendedFloatingActionButton? = null
    private var fabLoadQueue: ExtendedFloatingActionButton? = null

    private var isMenuOpen = false
    private val ANIMATION_DURATION = 250
    private val FAB_VERTICAL_SPACING_DP = 70f

    private var playerBottomSheetViewModel: PlayerBottomSheetViewModel? = null
    private var playbackViewModel: PlaybackViewModel? = null
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private var playerSongQueueAdapter: PlayerSongQueueAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = InnerFragmentPlayerQueueBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()

        playerBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<PlayerBottomSheetViewModel>(
                PlayerBottomSheetViewModel::class.java
            )
        playbackViewModel =
            ViewModelProvider(requireActivity()).get<PlaybackViewModel>(PlaybackViewModel::class.java)

        fabMenuToggle = bind!!.fabMenuToggle
        fabClearQueue = bind!!.fabClearQueue
        fabShuffleQueue = bind!!.fabShuffleQueue

        fabSaveToPlaylist = bind!!.fabSaveToPlaylist
        fabDownloadAll = bind!!.fabDownloadAll
        fabLoadQueue = bind!!.fabLoadQueue

        fabMenuToggle!!.setOnClickListener(View.OnClickListener { v: View? -> toggleFabMenu() })
        fabClearQueue!!.setOnClickListener(View.OnClickListener { v: View? -> handleClearQueueClick() })
        fabShuffleQueue!!.setOnClickListener(View.OnClickListener { v: View? -> handleShuffleQueueClick() })

        fabSaveToPlaylist!!.setOnClickListener(View.OnClickListener { v: View? -> handleSaveToPlaylistClick() })
        fabDownloadAll!!.setOnClickListener(View.OnClickListener { v: View? -> handleDownloadAllClick() })
        fabLoadQueue!!.setOnClickListener(View.OnClickListener { v: View? -> handleLoadQueueClick() })

        // Hide Load Queue FAB if sync is disabled
        if (!isSyncronizationEnabled()) {
            fabLoadQueue!!.setVisibility(View.GONE)
        }

        initQueueRecyclerView()

        return view
    }

    override fun onStart() {
        super.onStart()
        initializeBrowser()
        @Suppress("UNCHECKED_CAST")
        MediaManager.registerPlaybackObserver(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser?>?, playbackViewModel!!)
        observePlayback()
    }

    override fun onResume() {
        super.onResume()
        setMediaBrowserListenableFuture()
        updateNowPlayingItem()
        mediaBrowserListenableFuture!!.addListener(Runnable {
            try {
                val position =
                    mediaBrowserListenableFuture!!.get().getCurrentMediaItemIndex().toLong()
                requireActivity().runOnUiThread(Runnable {
                    bind!!.playerQueueRecyclerView.scrollToPosition(position.toInt())
                })
            } catch (e: Exception) {
                Log.e(
                    "PlayerQueueFragment",
                    "Failed to get mediaBrowserListenableFuture in onResume",
                    e
                )
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun initializeBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(
                requireContext(),
                ComponentName(requireContext(), MediaService::class.java)
            )
        ).buildAsync()
    }

    private fun releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture!!)
    }

    private fun setMediaBrowserListenableFuture() {
        @Suppress("UNCHECKED_CAST")
        playerSongQueueAdapter!!.setMediaBrowserListenableFuture(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser?>)
    }

    private fun initQueueRecyclerView() {
        bind!!.playerQueueRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.playerQueueRecyclerView.setHasFixedSize(true)

        playerSongQueueAdapter = PlayerSongQueueAdapter(this)
        bind!!.playerQueueRecyclerView.setAdapter(playerSongQueueAdapter)
        reapplyPlayback()

        playerBottomSheetViewModel!!.queueSong.observe(
            getViewLifecycleOwner(),
            Observer { queue: MutableList<Queue> ->
                playerSongQueueAdapter!!.items =
                    queue.map { it as? Child? }.toMutableList()
                reapplyPlayback()
            })

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            var originalPosition: Int = -1
            var fromPosition: Int = -1
            var toPosition: Int = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (originalPosition == -1) {
                    originalPosition = viewHolder.getBindingAdapterPosition()
                }

                fromPosition = viewHolder.getBindingAdapterPosition()
                toPosition = target.getBindingAdapterPosition()
                val queueItems = playerSongQueueAdapter!!.items ?: return false
                Collections.swap(queueItems, fromPosition, toPosition)
                recyclerView.getAdapter()!!.notifyItemMoved(fromPosition, toPosition)

                return false
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                if (originalPosition != -1 && fromPosition != -1 && toPosition != -1) {
                    @Suppress("UNCHECKED_CAST")
                    MediaManager.swap(
                        mediaBrowserListenableFuture as ListenableFuture<MediaBrowser?>?,
                        playerSongQueueAdapter!!.items,
                        originalPosition,
                        toPosition
                    )
                }

                originalPosition = -1
                fromPosition = -1
                toPosition = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                @Suppress("UNCHECKED_CAST")
                MediaManager.remove(
                    mediaBrowserListenableFuture as ListenableFuture<MediaBrowser?>?,
                    playerSongQueueAdapter!!.items!!,
                    viewHolder.getBindingAdapterPosition()
                )
                viewHolder.getBindingAdapter()!!.notifyDataSetChanged()
            }
        }).attachToRecyclerView(bind!!.playerQueueRecyclerView)
    }

    private fun updateNowPlayingItem() {
        playerSongQueueAdapter!!.notifyDataSetChanged()
    }

    override fun onMediaClick(bundle: Bundle?) {
        val tracks: MutableList<Child?> = bundle?.getParcelableArrayList<Child?>(
            Constants.TRACKS_OBJECT
        )?.toMutableList() ?: mutableListOf()
        MediaManager.startQueue(
            mediaBrowserListenableFuture, tracks, bundle?.getInt(Constants.ITEM_POSITION) ?: 0
        )
    }

    private fun observePlayback() {
        playbackViewModel!!.getCurrentSongId()
            .observe(getViewLifecycleOwner(), Observer { id: String? ->
                if (playerSongQueueAdapter != null) {
                    val playing = playbackViewModel!!.getIsPlaying().getValue()
                    playerSongQueueAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
        playbackViewModel!!.getIsPlaying()
            .observe(getViewLifecycleOwner(), Observer { playing: Boolean? ->
                if (playerSongQueueAdapter != null) {
                    val id = playbackViewModel!!.getCurrentSongId().getValue()
                    playerSongQueueAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
    }

    private fun reapplyPlayback() {
        if (playerSongQueueAdapter != null) {
            val id = playbackViewModel!!.getCurrentSongId().getValue()
            val playing = playbackViewModel!!.getIsPlaying().getValue()
            playerSongQueueAdapter!!.setPlaybackState(id, playing != null && playing)
        }
    }

    /**
     * Toggles the visibility and animates all six secondary FABs.
     */
    private fun toggleFabMenu() {
        if (isMenuOpen) {
            // CLOSE MENU (Reverse order for visual effect)
            if (isSyncronizationEnabled()) {
                closeFab(fabLoadQueue!!, 4)
            }
            closeFab(fabSaveToPlaylist!!, 3)
            closeFab(fabClearQueue!!, 2)
            closeFab(fabDownloadAll!!, 1)
            closeFab(fabShuffleQueue!!, 0)

            fabMenuToggle!!.animate().rotation(0f).setDuration(ANIMATION_DURATION.toLong()).start()
        } else {
            // OPEN MENU (lowest index at bottom)
            openFab(fabShuffleQueue!!, 0)
            openFab(fabDownloadAll!!, 1)
            openFab(fabClearQueue!!, 2)
            openFab(fabSaveToPlaylist!!, 3)
            if (isSyncronizationEnabled()) {
                openFab(fabLoadQueue!!, 4)
            }
            fabMenuToggle!!.animate().rotation(45f).setDuration(ANIMATION_DURATION.toLong()).start()
        }
        isMenuOpen = !isMenuOpen
    }

    private fun openFab(fab: View, index: Int) {
        val displacement =
            getResources().getDisplayMetrics().density * (FAB_VERTICAL_SPACING_DP * (index + 1))

        fab.setVisibility(View.VISIBLE)
        fab.setAlpha(0f)
        fab.setTranslationY(displacement) // Start at the hidden (closed) position

        fab.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(ANIMATION_DURATION.toLong())
            .start()
    }

    private fun closeFab(fab: View, index: Int) {
        val displacement =
            getResources().getDisplayMetrics().density * (FAB_VERTICAL_SPACING_DP * (index + 1))

        fab.animate()
            .translationY(displacement)
            .alpha(0f)
            .setDuration(ANIMATION_DURATION.toLong())
            .withEndAction(Runnable { fab.setVisibility(View.GONE) })
            .start()
    }

    private fun handleShuffleQueueClick() {
        Log.d(TAG, "Shuffle Queue Clicked!")

        mediaBrowserListenableFuture!!.addListener(Runnable {
            try {
                val mediaBrowser = mediaBrowserListenableFuture!!.get()
                val startPosition = mediaBrowser.getCurrentMediaItemIndex() + 1
                val queueItems = playerSongQueueAdapter!!.items ?: return@Runnable
                val endPosition = queueItems.size - 1

                if (startPosition < endPosition) {
                    val pool = ArrayList<Int?>()

                    for (i in startPosition..endPosition) {
                        pool.add(i)
                    }

                    while (pool.size >= 2) {
                        val fromPosition = (Math.random() * (pool.size)).toInt()
                        val positionA: Int = pool.get(fromPosition)!!
                        pool.removeAt(fromPosition)

                        val toPosition = (Math.random() * (pool.size)).toInt()
                        val positionB: Int = pool.get(toPosition)!!
                        pool.removeAt(toPosition)

                        Collections.swap(queueItems, positionA, positionB)
                        bind!!.playerQueueRecyclerView.getAdapter()!!
                            .notifyItemMoved(positionA, positionB)
                    }

                    MediaManager.shuffle(
                        mediaBrowserListenableFuture,
                        queueItems,
                        startPosition,
                        endPosition
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error shuffling queue", e)
            }
            toggleFabMenu()
        }, MoreExecutors.directExecutor())
    }

    private fun handleClearQueueClick() {
        Log.d(TAG, "Clear Queue Clicked!")

        mediaBrowserListenableFuture!!.addListener(Runnable {
            try {
                val mediaBrowser = mediaBrowserListenableFuture!!.get()
                val startPosition = mediaBrowser.getCurrentMediaItemIndex() + 1
                val queueItems = playerSongQueueAdapter!!.items ?: return@Runnable
                val endPosition = queueItems.size

                @Suppress("UNCHECKED_CAST")
                MediaManager.removeRange(
                    mediaBrowserListenableFuture as ListenableFuture<MediaBrowser?>?,
                    queueItems,
                    startPosition,
                    endPosition
                )
                bind!!.playerQueueRecyclerView.getAdapter()!!
                    .notifyItemRangeRemoved(startPosition, endPosition - startPosition)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing queue", e)
            }
            toggleFabMenu()
        }, MoreExecutors.directExecutor())
    }

    private fun handleSaveToPlaylistClick() {
        Log.d(TAG, "Save to Playlist Clicked!")

        val queueSongs: MutableList<Child?>? = playerSongQueueAdapter!!.items

        if (queueSongs == null || queueSongs.isEmpty()) {
            Toast.makeText(requireContext(), "Queue is empty", Toast.LENGTH_SHORT).show()
            toggleFabMenu()
            return
        }

        val bundle = Bundle()
        bundle.putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList<Child?>(queueSongs))

        val dialog = PlaylistChooserDialog()
        dialog.setArguments(bundle)
        dialog.show(requireActivity().getSupportFragmentManager(), null)

        toggleFabMenu()
    }

    private fun handleDownloadAllClick() {
        Log.d(TAG, "Download All Clicked!")

        val queueSongs: MutableList<Child?>? = playerSongQueueAdapter!!.items

        if (queueSongs == null || queueSongs.isEmpty()) {
            Toast.makeText(requireContext(), "Queue is empty", Toast.LENGTH_SHORT).show()
            toggleFabMenu()
            return
        }

        var downloadCount = 0

        if (getDownloadDirectoryUri() == null) {
            val mediaItemsToDownload: MutableList<MediaItem?> = mapMediaItems(queueSongs)
            val downloadModels: MutableList<Download?> = ArrayList<Download?>()

            for (child in queueSongs.filterNotNull()) {
                val downloadModel =
                    Download(child)
                downloadModel.artist = child.artist
                downloadModel.album = child.album
                downloadModel.coverArtId = child.coverArtId
                downloadModels.add(downloadModel)
            }

            val downloaderManager = DownloadUtil.getDownloadTracker(requireContext())
            downloaderManager.download(mediaItemsToDownload, downloadModels)
            downloadCount = queueSongs.size
            Toast.makeText(
                requireContext(),
                getResources().getQuantityString(
                    R.plurals.songs_download_started,
                    downloadCount,
                    downloadCount
                ),
                Toast.LENGTH_SHORT
            ).show()

            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                if (playerSongQueueAdapter != null) {
                    playerSongQueueAdapter!!.notifyDataSetChanged()
                }
            }, 1000)
        } else {
            for (song in queueSongs.filterNotNull()) {
                if (getUri(song) == null) {
                    downloadToUserDirectory(requireContext(), song)
                    downloadCount++
                }
            }

            if (downloadCount > 0) {
                Toast.makeText(
                    requireContext(),
                    getResources().getQuantityString(
                        R.plurals.songs_download_started,
                        downloadCount,
                        downloadCount
                    ),
                    Toast.LENGTH_SHORT
                ).show()

                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    if (playerSongQueueAdapter != null) {
                        playerSongQueueAdapter!!.notifyDataSetChanged()
                    }
                }, 2000)
            } else {
                Toast.makeText(requireContext(), "All songs already downloaded", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        toggleFabMenu()
    }

    private fun handleLoadQueueClick() {
        Log.d(TAG, "Load Queue Clicked!")
        if (!isSyncronizationEnabled()) {
            toggleFabMenu()
            return
        }

        val playerBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<PlayerBottomSheetViewModel>(
                PlayerBottomSheetViewModel::class.java
            )

        playerBottomSheetViewModel.playQueue.observe(
            getViewLifecycleOwner(),
            object : Observer<PlayQueue?> {
                override fun onChanged(value: PlayQueue?) {
                    val playQueue = value
                    playerBottomSheetViewModel.playQueue.removeObserver(this)

                    if (playQueue != null && playQueue.entries != null && !playQueue.entries!!.isEmpty()) {
                        var currentIndex = 0
                        for (i in playQueue.entries!!.indices) {
                            if (playQueue.entries!!.get(i).id == playQueue.current) {
                                currentIndex = i
                                break
                            }
                        }

                        @Suppress("UNCHECKED_CAST")
                        val entries: MutableList<Child?> = playQueue.entries?.toMutableList() ?: mutableListOf()
                        MediaManager.startQueue(
                            mediaBrowserListenableFuture,
                            entries,
                            currentIndex
                        )

                        Toast.makeText(requireContext(), "Queue loaded", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No saved queue found", Toast.LENGTH_SHORT)
                            .show()
                    }

                    toggleFabMenu()
                }
            })

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            if (isMenuOpen) {
                toggleFabMenu()
            }
        }, 1000)
    }

    companion object {
        private const val TAG = "PlayerQueueFragment"
    }
}