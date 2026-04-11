
package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentPlaylistPageBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioWriter.downloadToUserDirectory
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.cappielloantonio.tempo.viewmodel.PlaylistPageViewModel
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections
import java.util.Objects
import java.util.concurrent.Future
import java.util.function.Consumer
import java.util.stream.Collectors

@UnstableApi
class PlaylistPageFragment : Fragment(), ClickCallback {
    private var bind: FragmentPlaylistPageBinding? = null
    private var activity: MainActivity? = null
    private var playlistPageViewModel: PlaylistPageViewModel? = null
    private var playbackViewModel: PlaybackViewModel? = null

    private var songHorizontalAdapter: SongHorizontalAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.playlist_page_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)

        val searchView = searchItem.getActionView() as SearchView?
        searchView!!.setImeOptions(EditorInfo.IME_ACTION_DONE)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                songHorizontalAdapter!!.getFilter().filter(newText)
                return false
            }
        })

        searchView.setPadding(-32, 0, 0, 0)

        initMenuOption(menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentPlaylistPageBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        playlistPageViewModel =
            ViewModelProvider(requireActivity()).get<PlaylistPageViewModel>(PlaylistPageViewModel::class.java)
        playbackViewModel =
            ViewModelProvider(requireActivity()).get<PlaybackViewModel>(PlaybackViewModel::class.java)

        init()
        initAppBar()
        initMusicButton()
        initBackCover()
        initSongsView()

        return view
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()

        @Suppress("UNCHECKED_CAST")
        MediaManager.registerPlaybackObserver(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser?>?, playbackViewModel!!)
        observePlayback()
    }

    override fun onResume() {
        super.onResume()
        if (songHorizontalAdapter != null) setMediaBrowserListenableFuture()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.action_download_playlist) {
            playlistPageViewModel!!.playlistSongLiveList.observe(
                getViewLifecycleOwner(),
                Observer { songs: MutableList<Child?>? ->
                    if (isVisible() && getActivity() != null) {
                        if (getDownloadDirectoryUri() == null) {
                            DownloadUtil.getDownloadTracker(requireContext()).download(
                                MappingUtil.mapDownloads(songs!!),
                                ArrayList(songs.filterNotNull().map { child ->
                                    val toDownload = Download(child)
                                    toDownload.playlistId = playlistPageViewModel!!.getPlaylist()!!.id
                                    toDownload.playlistName = playlistPageViewModel!!.getPlaylist()!!.name
                                    toDownload
                                })
                            )
                        } else {
                            songs!!.forEach(Consumer { child: Child? ->
                                downloadToUserDirectory(
                                    requireContext(),
                                    child
                                )
                            })
                        }
                    }
                })
            return true
        } else if (item.getItemId() == R.id.action_pin_playlist) {
            playlistPageViewModel!!.setPinned(true)
            return true
        } else if (item.getItemId() == R.id.action_unpin_playlist) {
            playlistPageViewModel!!.setPinned(false)
            return true
        }

        return false
    }

    private fun init() {
        val playlist = BundleCompat.getParcelable(
            requireArguments(),
            Constants.PLAYLIST_OBJECT,
            Playlist::class.java
        )
        playlistPageViewModel!!.setPlaylist(checkNotNull(playlist))
    }

    private fun initMenuOption(menu: Menu) {
        playlistPageViewModel!!.isPinned(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { isPinned: Boolean? ->
                menu.findItem(R.id.action_unpin_playlist).setVisible(isPinned!!)
                menu.findItem(R.id.action_pin_playlist).setVisible(!isPinned)
            })
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.animToolbar)

        if (activity!!.getSupportActionBar() != null) {
            activity!!.getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
            activity!!.getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        }

        bind!!.animToolbar.setTitle(playlistPageViewModel!!.getPlaylist()!!.name)

        bind!!.playlistNameLabel.setText(playlistPageViewModel!!.getPlaylist()!!.name)
        bind!!.playlistSongCountLabel.setText(
            getString(
                R.string.playlist_song_count,
                playlistPageViewModel!!.getPlaylist()!!.songCount
            )
        )
        bind!!.playlistDurationLabel.setText(
            getString(
                R.string.playlist_duration,
                MusicUtil.getReadableDurationString(
                    playlistPageViewModel!!.getPlaylist()!!.duration,
                    false
                )
            )
        )

        bind!!.animToolbar.setNavigationOnClickListener(View.OnClickListener { v: View? ->
            hideKeyboard(v!!)
            activity!!.navController!!.navigateUp()
        })

        requireNotNull(bind!!.animToolbar.overflowIcon)
            .setTint(requireContext().getResources().getColor(R.color.titleTextColor, null))
    }

    private fun hideKeyboard(view: View) {
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

    private fun initMusicButton() {
        playlistPageViewModel!!.playlistSongLiveList.observe(
            getViewLifecycleOwner(),
            Observer { songs: MutableList<Child?>? ->
                val queueSongs = songs ?: return@Observer
                if (bind != null) {
                    bind!!.playlistPagePlayButton.setOnClickListener(View.OnClickListener { v: View? ->
                        MediaManager.startQueue(mediaBrowserListenableFuture, queueSongs, 0)
                        activity!!.setBottomSheetInPeek(true)
                    })

                    bind!!.playlistPageShuffleButton.setOnClickListener(View.OnClickListener { v: View? ->
                        val shuffledSongs: MutableList<Child?> = ArrayList(queueSongs)
                        Collections.shuffle(shuffledSongs)
                        MediaManager.startQueue(mediaBrowserListenableFuture, shuffledSongs, 0)
                        activity!!.setBottomSheetInPeek(true)
                    })
                }
            })
    }

    private fun initBackCover() {
        playlistPageViewModel!!.playlistSongLiveList.observe(
            requireActivity(),
            Observer { songs: MutableList<Child?>? ->
                if (bind != null && songs != null && !songs.isEmpty()) {
                    val randomSongs: MutableList<Child?> = ArrayList<Child?>(songs)
                    Collections.shuffle(randomSongs)

                    // Pic top-left
                    from(
                        requireContext(),
                        if (!randomSongs.isEmpty()) randomSongs.get(0)!!.coverArtId else playlistPageViewModel!!.getPlaylist()!!.coverArtId,
                        CustomGlideRequest.ResourceType.Song
                    )
                        .build()
                        .transform(
                            GranularRoundedCorners(
                                CustomGlideRequest.CORNER_RADIUS.toFloat(),
                                0f,
                                0f,
                                0f
                            )
                        )
                        .into(bind!!.playlistCoverImageViewTopLeft)

                    // Pic top-right
                    from(
                        requireContext(),
                        if (randomSongs.size > 1) randomSongs.get(1)!!.coverArtId else playlistPageViewModel!!.getPlaylist()!!.coverArtId,
                        CustomGlideRequest.ResourceType.Song
                    )
                        .build()
                        .transform(
                            GranularRoundedCorners(
                                0f,
                                CustomGlideRequest.CORNER_RADIUS.toFloat(),
                                0f,
                                0f
                            )
                        )
                        .into(bind!!.playlistCoverImageViewTopRight)

                    // Pic bottom-left
                    from(
                        requireContext(),
                        if (randomSongs.size > 2) randomSongs.get(2)!!.coverArtId else playlistPageViewModel!!.getPlaylist()!!.coverArtId,
                        CustomGlideRequest.ResourceType.Song
                    )
                        .build()
                        .transform(
                            GranularRoundedCorners(
                                0f,
                                0f,
                                0f,
                                CustomGlideRequest.CORNER_RADIUS.toFloat()
                            )
                        )
                        .into(bind!!.playlistCoverImageViewBottomLeft)

                    // Pic bottom-right
                    from(
                        requireContext(),
                        if (randomSongs.size > 3) randomSongs.get(3)!!.coverArtId else playlistPageViewModel!!.getPlaylist()!!.coverArtId,
                        CustomGlideRequest.ResourceType.Song
                    )
                        .build()
                        .transform(
                            GranularRoundedCorners(
                                0f,
                                0f,
                                CustomGlideRequest.CORNER_RADIUS.toFloat(),
                                0f
                            )
                        )
                        .into(bind!!.playlistCoverImageViewBottomRight)
                }
            })
    }

    private fun initSongsView() {
        bind!!.songRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.songRecyclerView.setHasFixedSize(true)

        songHorizontalAdapter =
            SongHorizontalAdapter(getViewLifecycleOwner(), this, true, false, null)
        bind!!.songRecyclerView.setAdapter(songHorizontalAdapter)
        setMediaBrowserListenableFuture()
        reapplyPlayback()

        playlistPageViewModel!!.playlistSongLiveList.observe(
            getViewLifecycleOwner(),
            Observer { songs: MutableList<Child?>? ->
                songHorizontalAdapter!!.setItems(songs)
                if (songs != null) {
                    bind!!.playlistSongCountLabel.setText(
                        getString(
                            R.string.playlist_song_count,
                            songs.size
                        )
                    )
                    val totalDuration = songs.sumOf { it?.duration?.toLong() ?: 0L }
                    bind!!.playlistDurationLabel.setText(
                        getString(
                            R.string.playlist_duration,
                            MusicUtil.getReadableDurationString(totalDuration, false)
                        )
                    )
                }
                reapplyPlayback()
            })
    }

    private fun initializeMediaBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(
                requireContext(),
                ComponentName(requireContext(), MediaService::class.java)
            )
        ).buildAsync()
    }

    @Suppress("UNCHECKED_CAST")
    private fun releaseMediaBrowser() {
        val future = mediaBrowserListenableFuture as ListenableFuture<MediaController>
        MediaBrowser.releaseFuture(future)
    }

    override fun onMediaClick(bundle: Bundle?) {
        val args = bundle ?: return
        val tracks = BundleCompat.getParcelableArrayList(args, Constants.TRACKS_OBJECT, Child::class.java)
            ?: return
        MediaManager.startQueue(
            mediaBrowserListenableFuture,
            tracks,
            args.getInt(Constants.ITEM_POSITION)
        )
        activity!!.setBottomSheetInPeek(true)
    }

    override fun onMediaLongClick(bundle: Bundle?) {
        bundle?.putString(Constants.PLAYLIST_ID, playlistPageViewModel!!.getPlaylist()!!.id)
        findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle!!)
    }

    private fun observePlayback() {
        playbackViewModel!!.getCurrentSongId()
            .observe(getViewLifecycleOwner(), Observer { id: String? ->
                if (songHorizontalAdapter != null) {
                    val playing = playbackViewModel!!.getIsPlaying().getValue()
                    songHorizontalAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
        playbackViewModel!!.getIsPlaying()
            .observe(getViewLifecycleOwner(), Observer { playing: Boolean? ->
                if (songHorizontalAdapter != null) {
                    val id = playbackViewModel!!.getCurrentSongId().getValue()
                    songHorizontalAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
    }

    private fun reapplyPlayback() {
        if (songHorizontalAdapter != null) {
            val id = playbackViewModel!!.getCurrentSongId().getValue()
            val playing = playbackViewModel!!.getIsPlaying().getValue()
            songHorizontalAdapter!!.setPlaybackState(id, playing != null && playing)
        }
    }

    private fun setMediaBrowserListenableFuture() {
        songHorizontalAdapter!!.setMediaBrowserListenableFuture(mediaBrowserListenableFuture)
    }
}
