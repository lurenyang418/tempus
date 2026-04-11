package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import androidx.viewpager2.widget.ViewPager2
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentHomeTabMusicBinding
import com.cappielloantonio.tempo.helper.recyclerview.CustomLinearSnapHelper
import com.cappielloantonio.tempo.helper.recyclerview.DotsIndicatorDecoration
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.interfaces.PlaylistCallback
import com.cappielloantonio.tempo.model.Chronology
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.AlbumAdapter
import com.cappielloantonio.tempo.ui.adapter.AlbumHorizontalAdapter
import com.cappielloantonio.tempo.ui.adapter.ArtistAdapter
import com.cappielloantonio.tempo.ui.adapter.ArtistHorizontalAdapter
import com.cappielloantonio.tempo.ui.adapter.DiscoverSongAdapter
import com.cappielloantonio.tempo.ui.adapter.PlaylistHorizontalAdapter
import com.cappielloantonio.tempo.ui.adapter.ShareHorizontalAdapter
import com.cappielloantonio.tempo.ui.adapter.SimilarTrackAdapter
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter
import com.cappielloantonio.tempo.ui.adapter.YearAdapter
import com.cappielloantonio.tempo.ui.dialog.HomeRearrangementDialog
import com.cappielloantonio.tempo.ui.dialog.PlaylistEditorDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader.getUri
import com.cappielloantonio.tempo.util.ExternalAudioWriter.downloadToUserDirectory
import com.cappielloantonio.tempo.util.MappingUtil.mapDownload
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.isSharingEnabled
import com.cappielloantonio.tempo.util.Preferences.isStarredAlbumsSyncEnabled
import com.cappielloantonio.tempo.util.Preferences.isStarredArtistsSyncEnabled
import com.cappielloantonio.tempo.util.Preferences.isStarredSyncEnabled
import com.cappielloantonio.tempo.util.UIUtil.getSpanCount
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.google.android.material.snackbar.Snackbar
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.stream.Collectors
import java.util.concurrent.Future
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.booleanArrayOf
import kotlin.math.min

@UnstableApi
class HomeTabMusicFragment : Fragment(), ClickCallback {
    private var bind: FragmentHomeTabMusicBinding? = null
    private var activity: MainActivity? = null
    private var homeViewModel: HomeViewModel? = null
    private var playbackViewModel: PlaybackViewModel? = null

    private var discoverSongAdapter: DiscoverSongAdapter? = null
    private var similarMusicAdapter: SimilarTrackAdapter? = null
    private var radioArtistAdapter: ArtistAdapter? = null
    private var bestOfArtistAdapter: ArtistAdapter? = null
    private var starredSongAdapter: SongHorizontalAdapter? = null
    private var topSongAdapter: SongHorizontalAdapter? = null
    private var starredAlbumAdapter: AlbumHorizontalAdapter? = null
    private var starredArtistAdapter: ArtistHorizontalAdapter? = null
    private var recentlyAddedAlbumAdapter: AlbumAdapter? = null
    private var recentlyPlayedAlbumAdapter: AlbumAdapter? = null
    private var mostPlayedAlbumAdapter: AlbumAdapter? = null
    private var newReleasesAlbumAdapter: AlbumHorizontalAdapter? = null
    private var yearAdapter: YearAdapter? = null
    private var playlistHorizontalAdapter: PlaylistHorizontalAdapter? = null
    private var shareHorizontalAdapter: ShareHorizontalAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity = getActivity() as MainActivity?

        bind = FragmentHomeTabMusicBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        homeViewModel =
            ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        playbackViewModel =
            ViewModelProvider(requireActivity()).get<PlaybackViewModel>(PlaybackViewModel::class.java)

        init()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSyncStarredView()
        initSyncStarredAlbumsView()
        initSyncStarredArtistsView()
        initDiscoverSongSlideView()
        initSimilarSongView()
        initArtistRadio()
        initArtistBestOf()
        initStarredTracksView()
        initStarredAlbumsView()
        initStarredArtistsView()
        initMostPlayedAlbumView()
        initRecentPlayedAlbumView()
        initNewReleasesView()
        initYearSongView()
        initRecentAddedAlbumView()
        initTopSongsView()
        initPinnedPlaylistsView()
        initSharesView()
        initHomeReorganizer()

        reorder()
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()

        @Suppress("UNCHECKED_CAST")
        MediaManager.registerPlaybackObserver(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser?>?, playbackViewModel!!)
        observeStarredSongsPlayback()
        observeTopSongsPlayback()
    }

    override fun onResume() {
        super.onResume()
        refreshSharesView()
        if (topSongAdapter != null) setTopSongsMediaBrowserListenableFuture()
        if (starredSongAdapter != null) setStarredSongsMediaBrowserListenableFuture()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        bind!!.discoveryTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshDiscoverySongSample(getViewLifecycleOwner())
            true
        })

        bind!!.discoveryTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            homeViewModel!!.randomShuffleSample.observe(
                getViewLifecycleOwner(),
                Observer { songs: MutableList<Child?>? ->
                    MusicUtil.ratingFilter(songs)
                    if (!songs!!.isEmpty()) {
                        MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                        activity!!.setBottomSheetInPeek(true)
                    }
                })
        })

        bind!!.similarTracksTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshSimilarSongSample(getViewLifecycleOwner())
            true
        })

        bind!!.radioArtistTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshRadioArtistSample(getViewLifecycleOwner())
            true
        })

        bind!!.bestOfArtistTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshBestOfArtist(getViewLifecycleOwner())
            true
        })

        bind!!.starredTracksTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putString(Constants.MEDIA_STARRED, Constants.MEDIA_STARRED)
            activity!!.navController!!.navigate(
                R.id.action_homeFragment_to_songListPageFragment,
                bundle
            )
        })

        bind!!.starredAlbumsTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putString(Constants.ALBUM_STARRED, Constants.ALBUM_STARRED)
            activity!!.navController!!.navigate(
                R.id.action_homeFragment_to_albumListPageFragment,
                bundle
            )
        })

        bind!!.starredArtistsTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putString(Constants.ARTIST_STARRED, Constants.ARTIST_STARRED)
            activity!!.navController!!.navigate(
                R.id.action_homeFragment_to_artistListPageFragment,
                bundle
            )
        })

        bind!!.recentlyAddedAlbumsTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putString(Constants.ALBUM_RECENTLY_ADDED, Constants.ALBUM_RECENTLY_ADDED)
            activity!!.navController!!.navigate(
                R.id.action_homeFragment_to_albumListPageFragment,
                bundle
            )
        })

        bind!!.playlistCatalogueTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putString(Constants.PLAYLIST_ALL, Constants.PLAYLIST_ALL)
            activity!!.navController!!.navigate(
                R.id.action_homeFragment_to_playlistCatalogueFragment,
                bundle
            )
        })

        bind!!.recentlyPlayedAlbumsTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putString(Constants.ALBUM_RECENTLY_PLAYED, Constants.ALBUM_RECENTLY_PLAYED)
            activity!!.navController!!.navigate(
                R.id.action_homeFragment_to_albumListPageFragment,
                bundle
            )
        })

        bind!!.mostPlayedAlbumsTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putString(Constants.ALBUM_MOST_PLAYED, Constants.ALBUM_MOST_PLAYED)
            activity!!.navController!!.navigate(
                R.id.action_homeFragment_to_albumListPageFragment,
                bundle
            )
        })

        bind!!.starredTracksTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshStarredTracks(getViewLifecycleOwner())
            true
        })

        bind!!.starredAlbumsTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshStarredAlbums(getViewLifecycleOwner())
            true
        })

        bind!!.starredArtistsTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshStarredArtists(getViewLifecycleOwner())
            true
        })

        bind!!.recentlyPlayedAlbumsTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshRecentlyPlayedAlbumList(getViewLifecycleOwner())
            true
        })

        bind!!.mostPlayedAlbumsTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshMostPlayedAlbums(getViewLifecycleOwner())
            true
        })

        bind!!.recentlyAddedAlbumsTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshMostRecentlyAddedAlbums(getViewLifecycleOwner())
            true
        })

        bind!!.sharesTextViewRefreshable.setOnLongClickListener(OnLongClickListener { v: View? ->
            homeViewModel!!.refreshShares(getViewLifecycleOwner())
            true
        })

        bind!!.gridTracksPreTextView.setOnClickListener(View.OnClickListener { view: View? ->
            showPopupMenu(
                view,
                R.menu.filter_top_songs_popup_menu
            )
        })
    }

    private fun initSyncStarredView() {
        if (isStarredSyncEnabled()) {
            homeViewModel!!.allStarredTracks.observe(
                getViewLifecycleOwner(),
                object : Observer<MutableList<Child?>?> {
                    override fun onChanged(value: MutableList<Child?>?) {
                        if (value != null && !value.isEmpty()) {
                            var songsToSyncCount = 0
                            val toSyncSample: MutableList<String?> = ArrayList<String?>()

                            if (getDownloadDirectoryUri() == null) {
                                val manager = DownloadUtil.getDownloadTracker(requireContext())
                                for (song in value) {
                                    if (song != null && !manager.isDownloaded(song.id)) {
                                        songsToSyncCount++
                                        if (toSyncSample.size < 3) {
                                            toSyncSample.add(song.title ?: "")
                                        }
                                    }
                                }
                            } else {
                                for (song in value) {
                                    if (song != null && getUri(song) == null) {
                                        songsToSyncCount++
                                        if (toSyncSample.size < 3) {
                                            toSyncSample.add(song.title ?: "")
                                        }
                                    }
                                }
                            }

                            if (songsToSyncCount > 0) {
                                bind!!.homeSyncStarredCard.setVisibility(View.VISIBLE)

                                val displayText = StringBuilder()
                                if (!toSyncSample.isEmpty()) {
                                    displayText.append(toSyncSample.joinToString(", "))
                                    if (songsToSyncCount > 3) {
                                        displayText.append("...")
                                    }
                                }

                                val countText = getResources().getQuantityString(
                                    R.plurals.home_sync_starred_songs_count,
                                    songsToSyncCount,
                                    songsToSyncCount
                                )

                                if (displayText.length > 0) {
                                    bind!!.homeSyncStarredTracksToSync.setText(displayText.toString() + "\n" + countText)
                                } else {
                                    bind!!.homeSyncStarredTracksToSync.setText(countText)
                                }

                                if (getActivity() != null) {
                                    getActivity()!!.runOnUiThread(Runnable { reorder() })
                                }
                            } else {
                                bind!!.homeSyncStarredCard.setVisibility(View.GONE)
                            }
                        }
                    }
                })
        }

        bind!!.homeSyncStarredCancel.setOnClickListener(View.OnClickListener { v: View? ->
            bind!!.homeSyncStarredCard.setVisibility(View.GONE)
            if (getActivity() != null) {
                getActivity()!!.runOnUiThread(Runnable { reorder() })
            }
        })

        bind!!.homeSyncStarredDownload.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                homeViewModel!!.allStarredTracks.observe(
                    getViewLifecycleOwner(),
                    object : Observer<MutableList<Child?>?> {
                        override fun onChanged(value: MutableList<Child?>?) {
                            if (value != null && !value.isEmpty()) {
                                var downloadedCount = 0

                                if (getDownloadDirectoryUri() == null) {
                                    val manager = DownloadUtil.getDownloadTracker(requireContext())
                                    for (song in value) {
                                        if (song != null && !manager.isDownloaded(song.id)) {
                                            manager.download(mapDownload(song), Download(song))
                                            downloadedCount++
                                        }
                                    }
                                } else {
                                    for (song in value) {
                                        if (song != null && getUri(song) == null) {
                                            downloadToUserDirectory(requireContext(), song)
                                            downloadedCount++
                                        }
                                    }
                                }

                                if (downloadedCount > 0) {
                                    Toast.makeText(
                                        requireContext(),
                                        getResources().getQuantityString(
                                            R.plurals.songs_download_started,
                                            downloadedCount,
                                            downloadedCount
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            bind!!.homeSyncStarredCard.setVisibility(View.GONE)
                            if (getActivity() != null) {
                                getActivity()!!.runOnUiThread(Runnable { reorder() })
                            }
                        }
                    })
            }
        })
    }

    private fun initSyncStarredAlbumsView() {
        if (isStarredAlbumsSyncEnabled()) {
            homeViewModel!!.getStarredAlbums(getViewLifecycleOwner())
                .observe(getViewLifecycleOwner(), object : Observer<MutableList<AlbumID3?>?> {
                    @Suppress("UNCHECKED_CAST")
                    override fun onChanged(value: MutableList<AlbumID3?>?) {
                        if (value != null && !value.isEmpty()) {
                            checkIfAlbumsNeedSync(value as MutableList<AlbumID3>)
                        }
                    }
                })
        }

        bind!!.homeSyncStarredAlbumsCancel.setOnClickListener(View.OnClickListener { v: View? ->
            bind!!.homeSyncStarredAlbumsCard.setVisibility(View.GONE)
            if (getActivity() != null) {
                getActivity()!!.runOnUiThread(Runnable { reorder() })
            }
        })

        bind!!.homeSyncStarredAlbumsDownload.setOnClickListener(View.OnClickListener { v: View? ->
            homeViewModel!!.allStarredAlbumSongs!!.observe(
                getViewLifecycleOwner(),
                object : Observer<MutableList<Child?>?> {
                    override fun onChanged(value: MutableList<Child?>?) {
                        if (value != null && !value.isEmpty()) {
                            var songsToDownload = 0

                            if (getDownloadDirectoryUri() == null) {
                                val manager = DownloadUtil.getDownloadTracker(requireContext())
                                for (song in value) {
                                    if (song != null && !manager.isDownloaded(song.id)) {
                                        manager.download(mapDownload(song), Download(song))
                                        songsToDownload++
                                    }
                                }
                            } else {
                                for (song in value) {
                                    if (song != null && getUri(song) == null) {
                                        downloadToUserDirectory(requireContext(), song)
                                        songsToDownload++
                                    }
                                }
                            }

                            if (songsToDownload > 0) {
                                Toast.makeText(
                                    requireContext(),
                                    getResources().getQuantityString(
                                        R.plurals.songs_download_started,
                                        songsToDownload,
                                        songsToDownload
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        bind!!.homeSyncStarredAlbumsCard.setVisibility(View.GONE)
                        if (getActivity() != null) {
                            getActivity()!!.runOnUiThread(Runnable { reorder() })
                        }
                    }
                })
        })
    }

    private fun checkIfAlbumsNeedSync(albums: MutableList<AlbumID3>) {
        homeViewModel!!.allStarredAlbumSongs!!.observe(
            getViewLifecycleOwner(),
            object : Observer<MutableList<Child?>?> {
                override fun onChanged(value: MutableList<Child?>?) {
                    if (value != null) {
                        var songsToDownload = 0
                        val albumsNeedingSync: MutableList<kotlin.String?> =
                            ArrayList<kotlin.String?>()

                        if (getDownloadDirectoryUri() == null) {
                            val manager = DownloadUtil.getDownloadTracker(requireContext())

                            for (album in albums) {
                                var albumNeedsSync = false
                                for (song in value) {
                                    if (song != null && song.albumId != null && song.albumId == album.id && !manager.isDownloaded(
                                            song.id
                                        )
                                    ) {
                                        songsToDownload++
                                        albumNeedsSync = true
                                    }
                                }
                                if (albumNeedsSync) {
                                    albumsNeedingSync.add(album.name ?: "")
                                }
                            }
                        } else {
                            for (album in albums) {
                                var albumNeedsSync = false
                                for (song in value) {
                                    if (song != null && song.albumId != null && song.albumId == album.id && getUri(
                                            song
                                        ) == null
                                    ) {
                                        songsToDownload++
                                        albumNeedsSync = true
                                    }
                                }
                                if (albumNeedsSync) {
                                    albumsNeedingSync.add(album.name ?: "")
                                }
                            }
                        }

                        if (songsToDownload > 0) {
                            bind!!.homeSyncStarredAlbumsCard.setVisibility(View.VISIBLE)

                            val displayText = StringBuilder()
                            val sampleAlbums: MutableList<kotlin.String?> =
                                ArrayList<kotlin.String?>()

                            for (i in 0..<min(albumsNeedingSync.size, 3)) {
                                sampleAlbums.add(albumsNeedingSync.get(i))
                            }

                            if (!sampleAlbums.isEmpty()) {
                                displayText.append(sampleAlbums.joinToString(", "))
                                if (albumsNeedingSync.size > 3) {
                                    displayText.append("...")
                                }
                            }

                            val countText = getResources().getQuantityString(
                                R.plurals.home_sync_starred_albums_count,
                                albumsNeedingSync.size,
                                albumsNeedingSync.size
                            )

                            if (displayText.length > 0) {
                                bind!!.homeSyncStarredAlbumsToSync.setText(displayText.toString() + "\n" + countText)
                            } else {
                                bind!!.homeSyncStarredAlbumsToSync.setText(countText)
                            }

                            if (getActivity() != null) {
                                getActivity()!!.runOnUiThread(Runnable { reorder() })
                            }
                        } else {
                            bind!!.homeSyncStarredAlbumsCard.setVisibility(View.GONE)
                        }
                    }
                }
            })
    }

    private fun initSyncStarredArtistsView() {
        if (isStarredArtistsSyncEnabled()) {
            homeViewModel!!.getStarredArtists(getViewLifecycleOwner())
                .observe(getViewLifecycleOwner(), object : Observer<MutableList<ArtistID3?>?> {
                    @Suppress("UNCHECKED_CAST")
                    override fun onChanged(value: MutableList<ArtistID3?>?) {
                        if (value != null && !value.isEmpty()) {
                            checkIfArtistsNeedSync(value as MutableList<ArtistID3>)
                        }
                    }
                })
        }

        bind!!.homeSyncStarredArtistsCancel.setOnClickListener(View.OnClickListener { v: View? ->
            bind!!.homeSyncStarredArtistsCard.setVisibility(View.GONE)
            if (getActivity() != null) {
                getActivity()!!.runOnUiThread(Runnable { reorder() })
            }
        })

        bind!!.homeSyncStarredArtistsDownload.setOnClickListener(View.OnClickListener { v: View? ->
            homeViewModel!!.allStarredArtistSongs!!.observe(
                getViewLifecycleOwner(),
                object : Observer<MutableList<Child?>?> {
                    override fun onChanged(value: MutableList<Child?>?) {
                        if (value != null && !value.isEmpty()) {
                            var songsToDownload = 0

                            if (getDownloadDirectoryUri() == null) {
                                val manager = DownloadUtil.getDownloadTracker(requireContext())
                                for (song in value) {
                                    if (song != null && !manager.isDownloaded(song.id)) {
                                        manager.download(mapDownload(song), Download(song))
                                        songsToDownload++
                                    }
                                }
                            } else {
                                for (song in value) {
                                    if (song != null && getUri(song) == null) {
                                        downloadToUserDirectory(requireContext(), song)
                                        songsToDownload++
                                    }
                                }
                            }

                            if (songsToDownload > 0) {
                                Toast.makeText(
                                    requireContext(),
                                    getResources().getQuantityString(
                                        R.plurals.songs_download_started,
                                        songsToDownload,
                                        songsToDownload
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        bind!!.homeSyncStarredArtistsCard.setVisibility(View.GONE)
                        if (getActivity() != null) {
                            getActivity()!!.runOnUiThread(Runnable { reorder() })
                        }
                    }
                })
        })
    }

    private fun checkIfArtistsNeedSync(artists: MutableList<ArtistID3>) {
        homeViewModel!!.allStarredArtistSongs!!.observe(
            getViewLifecycleOwner(),
            object : Observer<MutableList<Child?>?> {
                override fun onChanged(value: MutableList<Child?>?) {
                    if (value != null) {
                        var songsToDownload = 0
                        val artistsNeedingSync: MutableList<kotlin.String?> =
                            ArrayList<kotlin.String?>()

                        if (getDownloadDirectoryUri() == null) {
                            val manager = DownloadUtil.getDownloadTracker(requireContext())

                            for (artist in artists) {
                                var artistNeedsSync = false
                                for (song in value) {
                                    if (song != null && song.artistId != null && song.artistId == artist.id && !manager.isDownloaded(
                                            song.id
                                        )
                                    ) {
                                        songsToDownload++
                                        artistNeedsSync = true
                                    }
                                }
                                if (artistNeedsSync) {
                                    artistsNeedingSync.add(artist.name ?: "")
                                }
                            }
                        } else {
                            for (artist in artists) {
                                var artistNeedsSync = false
                                for (song in value) {
                                    if (song != null && song.artistId != null && song.artistId == artist.id && getUri(
                                            song
                                        ) == null
                                    ) {
                                        songsToDownload++
                                        artistNeedsSync = true
                                    }
                                }
                                if (artistNeedsSync) {
                                    artistsNeedingSync.add(artist.name ?: "")
                                }
                            }
                        }

                        if (songsToDownload > 0) {
                            bind!!.homeSyncStarredArtistsCard.setVisibility(View.VISIBLE)

                            val displayText = StringBuilder()
                            val sampleArtists: MutableList<kotlin.String?> =
                                ArrayList<kotlin.String?>()

                            for (i in 0..<min(artistsNeedingSync.size, 3)) {
                                sampleArtists.add(artistsNeedingSync.get(i))
                            }

                            if (!sampleArtists.isEmpty()) {
                                displayText.append(sampleArtists.joinToString(", "))
                                if (artistsNeedingSync.size > 3) {
                                    displayText.append("...")
                                }
                            }

                            val countText = getResources().getQuantityString(
                                R.plurals.home_sync_starred_artists_count,
                                artistsNeedingSync.size,
                                artistsNeedingSync.size
                            )

                            if (displayText.length > 0) {
                                bind!!.homeSyncStarredArtistsToSync.setText(displayText.toString() + "\n" + countText)
                            } else {
                                bind!!.homeSyncStarredArtistsToSync.setText(countText)
                            }

                            if (getActivity() != null) {
                                getActivity()!!.runOnUiThread(Runnable { reorder() })
                            }
                        } else {
                            bind!!.homeSyncStarredArtistsCard.setVisibility(View.GONE)
                        }
                    }
                }
            })
    }

    private fun initDiscoverSongSlideView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_DISCOVERY)) return

        bind!!.discoverSongRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.discoverSongRecyclerView.setHasFixedSize(true)

        discoverSongAdapter = DiscoverSongAdapter(this)
        bind!!.discoverSongRecyclerView.setAdapter(discoverSongAdapter)

        homeViewModel!!.getDiscoverSongSample(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                MusicUtil.ratingFilter(songs)
                if (songs == null) {
                    if (bind != null) bind!!.homeDiscoverSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeDiscoverSector.setVisibility(if (!songs.isEmpty()) View.VISIBLE else View.GONE)

                    discoverSongAdapter!!.setItems(songs)
                }
            })
    }

    private fun initSimilarSongView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_MADE_FOR_YOU)) return

        bind!!.similarTracksRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.similarTracksRecyclerView.setHasFixedSize(true)

        similarMusicAdapter = SimilarTrackAdapter(this)
        bind!!.similarTracksRecyclerView.setAdapter(similarMusicAdapter)
        homeViewModel!!.getStarredTracksSample(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                MusicUtil.ratingFilter(songs)
                if (songs == null) {
                    if (bind != null) bind!!.homeSimilarTracksSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeSimilarTracksSector.setVisibility(if (!songs.isEmpty()) View.VISIBLE else View.GONE)

                    similarMusicAdapter!!.setItems(songs)
                }
            })

        val similarSongSnapHelper = CustomLinearSnapHelper()
        similarSongSnapHelper.attachToRecyclerView(bind!!.similarTracksRecyclerView)
    }

    private fun initArtistBestOf() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_BEST_OF)) return

        bind!!.bestOfArtistRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.bestOfArtistRecyclerView.setHasFixedSize(true)

        bestOfArtistAdapter = ArtistAdapter(this, false, true)
        bind!!.bestOfArtistRecyclerView.setAdapter(bestOfArtistAdapter)
        homeViewModel!!.getBestOfArtists(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { artists: MutableList<ArtistID3?>? ->
                if (artists == null) {
                    if (bind != null) bind!!.homeBestOfArtistSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeBestOfArtistSector.setVisibility(if (!artists.isEmpty()) View.VISIBLE else View.GONE)

                    bestOfArtistAdapter!!.setItems(artists)
                }
            })

        val artistBestOfSnapHelper = CustomLinearSnapHelper()
        artistBestOfSnapHelper.attachToRecyclerView(bind!!.bestOfArtistRecyclerView)
    }

    private fun initArtistRadio() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_RADIO_STATION)) return

        bind!!.radioArtistRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.radioArtistRecyclerView.setHasFixedSize(true)

        radioArtistAdapter = ArtistAdapter(this, true, false)
        bind!!.radioArtistRecyclerView.setAdapter(radioArtistAdapter)
        homeViewModel!!.getStarredArtistsSample(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { artists: MutableList<ArtistID3?>? ->
                if (artists == null) {
                    if (bind != null) bind!!.homeRadioArtistSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeRadioArtistSector.setVisibility(if (!artists.isEmpty()) View.VISIBLE else View.GONE)
                    if (bind != null) bind!!.afterRadioArtistDivider.setVisibility(if (!artists.isEmpty()) View.VISIBLE else View.GONE)

                    radioArtistAdapter!!.setItems(artists)
                }
            })

        val artistRadioSnapHelper = CustomLinearSnapHelper()
        artistRadioSnapHelper.attachToRecyclerView(bind!!.radioArtistRecyclerView)
    }

    private fun initTopSongsView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_TOP_SONGS)) return

        bind!!.topSongsRecyclerView.setHasFixedSize(true)

        topSongAdapter = SongHorizontalAdapter(getViewLifecycleOwner(), this, true, false, null)
        bind!!.topSongsRecyclerView.setAdapter(topSongAdapter)
        setTopSongsMediaBrowserListenableFuture()
        reapplyTopSongsPlayback()
        homeViewModel!!.getChronologySample(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { chronologies: MutableList<Chronology>? ->
                if (chronologies == null || chronologies.isEmpty()) {
                    if (bind != null) bind!!.homeGridTracksSector.setVisibility(View.GONE)
                    if (bind != null) bind!!.afterGridDivider.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeGridTracksSector.setVisibility(View.VISIBLE)
                    if (bind != null) bind!!.afterGridDivider.setVisibility(View.VISIBLE)
                    if (bind != null) bind!!.topSongsRecyclerView.setLayoutManager(
                        GridLayoutManager(
                            requireContext(),
                            getSpanCount(chronologies.size, 5),
                            GridLayoutManager.HORIZONTAL,
                            false
                        )
                    )

                    val topSongs = chronologies.map { it as Child? }.toMutableList()

                    topSongAdapter!!.setItems(topSongs)
                    reapplyTopSongsPlayback()
                }
            })

        val topTrackSnapHelper: SnapHelper = PagerSnapHelper()
        topTrackSnapHelper.attachToRecyclerView(bind!!.topSongsRecyclerView)

        bind!!.topSongsRecyclerView.addItemDecoration(
            DotsIndicatorDecoration(
                getResources().getDimensionPixelSize(R.dimen.radius),
                getResources().getDimensionPixelSize(R.dimen.radius) * 4,
                getResources().getDimensionPixelSize(R.dimen.dots_height),
                requireContext().getResources().getColor(R.color.titleTextColor, null),
                requireContext().getResources().getColor(R.color.titleTextColor, null)
            )
        )
    }

    private fun initStarredTracksView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_TRACKS)) return

        bind!!.starredTracksRecyclerView.setHasFixedSize(true)

        starredSongAdapter = SongHorizontalAdapter(getViewLifecycleOwner(), this, true, false, null)
        bind!!.starredTracksRecyclerView.setAdapter(starredSongAdapter)
        setStarredSongsMediaBrowserListenableFuture()
        reapplyStarredSongsPlayback()
        homeViewModel!!.getStarredTracks(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                if (songs == null) {
                    if (bind != null) bind!!.starredTracksSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.starredTracksSector.setVisibility(if (!songs.isEmpty()) View.VISIBLE else View.GONE)
                    if (bind != null) bind!!.starredTracksRecyclerView.setLayoutManager(
                        GridLayoutManager(
                            requireContext(),
                            getSpanCount(songs.size, 5),
                            GridLayoutManager.HORIZONTAL,
                            false
                        )
                    )

                    starredSongAdapter!!.setItems(songs)
                    reapplyStarredSongsPlayback()
                }
            })

        val starredTrackSnapHelper: SnapHelper = PagerSnapHelper()
        starredTrackSnapHelper.attachToRecyclerView(bind!!.starredTracksRecyclerView)

        bind!!.starredTracksRecyclerView.addItemDecoration(
            DotsIndicatorDecoration(
                getResources().getDimensionPixelSize(R.dimen.radius),
                getResources().getDimensionPixelSize(R.dimen.radius) * 4,
                getResources().getDimensionPixelSize(R.dimen.dots_height),
                requireContext().getResources().getColor(R.color.titleTextColor, null),
                requireContext().getResources().getColor(R.color.titleTextColor, null)
            )
        )
    }

    private fun initStarredAlbumsView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_ALBUMS)) return

        bind!!.starredAlbumsRecyclerView.setHasFixedSize(true)

        starredAlbumAdapter = AlbumHorizontalAdapter(this, false)
        bind!!.starredAlbumsRecyclerView.setAdapter(starredAlbumAdapter)
        homeViewModel!!.getStarredAlbums(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { albums: MutableList<AlbumID3?>? ->
                if (albums == null) {
                    if (bind != null) bind!!.starredAlbumsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.starredAlbumsSector.setVisibility(if (!albums.isEmpty()) View.VISIBLE else View.GONE)
                    if (bind != null) bind!!.starredAlbumsRecyclerView.setLayoutManager(
                        GridLayoutManager(
                            requireContext(),
                            getSpanCount(albums.size, 5),
                            GridLayoutManager.HORIZONTAL,
                            false
                        )
                    )

                    starredAlbumAdapter!!.setItems(albums)
                }
            })

        val starredAlbumSnapHelper: SnapHelper = PagerSnapHelper()
        starredAlbumSnapHelper.attachToRecyclerView(bind!!.starredAlbumsRecyclerView)

        bind!!.starredAlbumsRecyclerView.addItemDecoration(
            DotsIndicatorDecoration(
                getResources().getDimensionPixelSize(R.dimen.radius),
                getResources().getDimensionPixelSize(R.dimen.radius) * 4,
                getResources().getDimensionPixelSize(R.dimen.dots_height),
                requireContext().getResources().getColor(R.color.titleTextColor, null),
                requireContext().getResources().getColor(R.color.titleTextColor, null)
            )
        )
    }

    private fun initStarredArtistsView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_STARRED_ARTISTS)) return

        bind!!.starredArtistsRecyclerView.setHasFixedSize(true)

        starredArtistAdapter = ArtistHorizontalAdapter(this)
        bind!!.starredArtistsRecyclerView.setAdapter(starredArtistAdapter)
        homeViewModel!!.getStarredArtists(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { artists: MutableList<ArtistID3?>? ->
                if (artists == null) {
                    if (bind != null) bind!!.starredArtistsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.starredArtistsSector.setVisibility(if (!artists.isEmpty()) View.VISIBLE else View.GONE)
                    if (bind != null) bind!!.afterFavoritesDivider.setVisibility(if (!artists.isEmpty()) View.VISIBLE else View.GONE)
                    if (bind != null) bind!!.starredArtistsRecyclerView.setLayoutManager(
                        GridLayoutManager(
                            requireContext(),
                            getSpanCount(artists.size, 5),
                            GridLayoutManager.HORIZONTAL,
                            false
                        )
                    )

                    starredArtistAdapter!!.setItems(artists)
                }
            })

        val starredArtistSnapHelper: SnapHelper = PagerSnapHelper()
        starredArtistSnapHelper.attachToRecyclerView(bind!!.starredArtistsRecyclerView)

        bind!!.starredArtistsRecyclerView.addItemDecoration(
            DotsIndicatorDecoration(
                getResources().getDimensionPixelSize(R.dimen.radius),
                getResources().getDimensionPixelSize(R.dimen.radius) * 4,
                getResources().getDimensionPixelSize(R.dimen.dots_height),
                requireContext().getResources().getColor(R.color.titleTextColor, null),
                requireContext().getResources().getColor(R.color.titleTextColor, null)
            )
        )
    }

    private fun initNewReleasesView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_NEW_RELEASES)) return

        bind!!.newReleasesRecyclerView.setHasFixedSize(true)

        newReleasesAlbumAdapter = AlbumHorizontalAdapter(this, false)
        bind!!.newReleasesRecyclerView.setAdapter(newReleasesAlbumAdapter)
        homeViewModel!!.getRecentlyReleasedAlbums(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { albums: MutableList<AlbumID3?>? ->
                if (albums == null) {
                    if (bind != null) bind!!.homeNewReleasesSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeNewReleasesSector.setVisibility(if (!albums.isEmpty()) View.VISIBLE else View.GONE)
                    if (bind != null) bind!!.newReleasesRecyclerView.setLayoutManager(
                        GridLayoutManager(
                            requireContext(),
                            getSpanCount(albums.size, 5),
                            GridLayoutManager.HORIZONTAL,
                            false
                        )
                    )

                    newReleasesAlbumAdapter!!.setItems(albums)
                }
            })

        val newReleasesSnapHelper: SnapHelper = PagerSnapHelper()
        newReleasesSnapHelper.attachToRecyclerView(bind!!.newReleasesRecyclerView)

        bind!!.newReleasesRecyclerView.addItemDecoration(
            DotsIndicatorDecoration(
                getResources().getDimensionPixelSize(R.dimen.radius),
                getResources().getDimensionPixelSize(R.dimen.radius) * 4,
                getResources().getDimensionPixelSize(R.dimen.dots_height),
                requireContext().getResources().getColor(R.color.titleTextColor, null),
                requireContext().getResources().getColor(R.color.titleTextColor, null)
            )
        )
    }

    private fun initYearSongView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_FLASHBACK)) return

        bind!!.yearsRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.yearsRecyclerView.setHasFixedSize(true)

        yearAdapter = YearAdapter(this)
        bind!!.yearsRecyclerView.setAdapter(yearAdapter)
        homeViewModel!!.getYearList(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { years: MutableList<Int?>? ->
                if (years == null) {
                    if (bind != null) bind!!.homeFlashbackSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeFlashbackSector.setVisibility(if (!years.isEmpty()) View.VISIBLE else View.GONE)

                    yearAdapter!!.setItems(years)
                }
            })

        val yearSnapHelper = CustomLinearSnapHelper()
        yearSnapHelper.attachToRecyclerView(bind!!.yearsRecyclerView)
    }

    private fun initMostPlayedAlbumView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_MOST_PLAYED)) return

        bind!!.mostPlayedAlbumsRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.mostPlayedAlbumsRecyclerView.setHasFixedSize(true)

        mostPlayedAlbumAdapter = AlbumAdapter(this)
        bind!!.mostPlayedAlbumsRecyclerView.setAdapter(mostPlayedAlbumAdapter)
        homeViewModel!!.getMostPlayedAlbums(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { albums: MutableList<AlbumID3?>? ->
                if (albums == null) {
                    if (bind != null) bind!!.homeMostPlayedAlbumsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeMostPlayedAlbumsSector.setVisibility(if (!albums.isEmpty()) View.VISIBLE else View.GONE)

                    mostPlayedAlbumAdapter!!.setItems(albums)
                }
            })

        val mostPlayedAlbumSnapHelper = CustomLinearSnapHelper()
        mostPlayedAlbumSnapHelper.attachToRecyclerView(bind!!.mostPlayedAlbumsRecyclerView)
    }

    private fun initRecentPlayedAlbumView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_LAST_PLAYED)) return

        bind!!.recentlyPlayedAlbumsRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.recentlyPlayedAlbumsRecyclerView.setHasFixedSize(true)

        recentlyPlayedAlbumAdapter = AlbumAdapter(this)
        bind!!.recentlyPlayedAlbumsRecyclerView.setAdapter(recentlyPlayedAlbumAdapter)
        homeViewModel!!.getRecentlyPlayedAlbumList(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { albums: MutableList<AlbumID3?>? ->
                if (albums == null) {
                    if (bind != null) bind!!.homeRecentlyPlayedAlbumsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeRecentlyPlayedAlbumsSector.setVisibility(if (!albums.isEmpty()) View.VISIBLE else View.GONE)

                    recentlyPlayedAlbumAdapter!!.setItems(albums)
                }
            })

        val recentPlayedAlbumSnapHelper = CustomLinearSnapHelper()
        recentPlayedAlbumSnapHelper.attachToRecyclerView(bind!!.recentlyPlayedAlbumsRecyclerView)
    }

    private fun initRecentAddedAlbumView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_RECENTLY_ADDED)) return

        bind!!.recentlyAddedAlbumsRecyclerView.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        bind!!.recentlyAddedAlbumsRecyclerView.setHasFixedSize(true)

        recentlyAddedAlbumAdapter = AlbumAdapter(this)
        bind!!.recentlyAddedAlbumsRecyclerView.setAdapter(recentlyAddedAlbumAdapter)
        homeViewModel!!.getMostRecentlyAddedAlbums(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { albums: MutableList<AlbumID3?>? ->
                if (albums == null) {
                    if (bind != null) bind!!.homeRecentlyAddedAlbumsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.homeRecentlyAddedAlbumsSector.setVisibility(if (!albums.isEmpty()) View.VISIBLE else View.GONE)

                    recentlyAddedAlbumAdapter!!.setItems(albums)
                }
            })

        val recentAddedAlbumSnapHelper = CustomLinearSnapHelper()
        recentAddedAlbumSnapHelper.attachToRecyclerView(bind!!.recentlyAddedAlbumsRecyclerView)
    }

    private fun initPinnedPlaylistsView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_PINNED_PLAYLISTS)) return

        bind!!.pinnedPlaylistsRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        bind!!.pinnedPlaylistsRecyclerView.setHasFixedSize(true)

        playlistHorizontalAdapter = PlaylistHorizontalAdapter(this)
        bind!!.pinnedPlaylistsRecyclerView.setAdapter(playlistHorizontalAdapter)
        homeViewModel!!.getPinnedPlaylists(getViewLifecycleOwner())
            .observe(getViewLifecycleOwner(), Observer { playlists: MutableList<Playlist?>? ->
                if (playlists == null) {
                    if (bind != null) bind!!.pinnedPlaylistsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.pinnedPlaylistsSector.setVisibility(if (!playlists.isEmpty()) View.VISIBLE else View.GONE)

                    playlistHorizontalAdapter!!.setItems(playlists)
                }
            })
    }

    private fun initSharesView() {
        if (homeViewModel!!.checkHomeSectorVisibility(Constants.HOME_SECTOR_SHARED)) return

        bind!!.sharesRecyclerView.setHasFixedSize(true)

        shareHorizontalAdapter = ShareHorizontalAdapter(this)
        bind!!.sharesRecyclerView.setAdapter(shareHorizontalAdapter)
        if (isSharingEnabled()) {
            homeViewModel!!.getShares(getViewLifecycleOwner())
                .observe(getViewLifecycleOwner(), Observer { shares: MutableList<Share?>? ->
                    if (shares == null) {
                        if (bind != null) bind!!.sharesSector.setVisibility(View.GONE)
                    } else {
                        if (bind != null) bind!!.sharesSector.setVisibility(if (!shares.isEmpty()) View.VISIBLE else View.GONE)
                        if (bind != null) bind!!.sharesRecyclerView.setLayoutManager(
                            GridLayoutManager(
                                requireContext(),
                                getSpanCount(shares.size, 10),
                                GridLayoutManager.HORIZONTAL,
                                false
                            )
                        )

                        shareHorizontalAdapter!!.setItems(shares)
                    }
                })
        }

        val starredTrackSnapHelper: SnapHelper = PagerSnapHelper()
        starredTrackSnapHelper.attachToRecyclerView(bind!!.sharesRecyclerView)

        bind!!.sharesRecyclerView.addItemDecoration(
            DotsIndicatorDecoration(
                getResources().getDimensionPixelSize(R.dimen.radius),
                getResources().getDimensionPixelSize(R.dimen.radius) * 4,
                getResources().getDimensionPixelSize(R.dimen.dots_height),
                requireContext().getResources().getColor(R.color.titleTextColor, null),
                requireContext().getResources().getColor(R.color.titleTextColor, null)
            )
        )
    }

    private fun initHomeReorganizer() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            if (bind != null) bind!!.homeSectorRearrangementButton.setVisibility(
                View.VISIBLE
            )
        }
        handler.postDelayed(runnable, 5000)

        bind!!.homeSectorRearrangementButton.setOnClickListener(View.OnClickListener { v: View? ->
            val dialog = HomeRearrangementDialog()
            dialog.show(requireActivity().getSupportFragmentManager(), null)
        })
    }

    private fun refreshSharesView() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            if (getView() != null && bind != null && isSharingEnabled()) {
                homeViewModel!!.refreshShares(getViewLifecycleOwner())
            }
        }
        handler.postDelayed(runnable, 100)
    }

    private fun setSlideViewOffset(viewPager: ViewPager2, pageOffset: Float, pageMargin: Float) {
        viewPager.setPageTransformer(ViewPager2.PageTransformer { page: View?, position: Float ->
            val myOffset = position * -(2 * pageOffset + pageMargin)
            if (viewPager.getOrientation() == ViewPager2.ORIENTATION_HORIZONTAL) {
                if (ViewCompat.getLayoutDirection(viewPager) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                    page!!.setTranslationX(-myOffset)
                } else {
                    page!!.setTranslationX(myOffset)
                }
            } else {
                page!!.setTranslationY(myOffset)
            }
        })
    }

    fun reorder() {
        if (bind != null && homeViewModel!!.homeSectorList != null) {
            bind!!.homeLinearLayoutContainer.removeAllViews()

            if (bind!!.homeSyncStarredCard.getVisibility() == View.VISIBLE) {
                bind!!.homeLinearLayoutContainer.addView(bind!!.homeSyncStarredCard)
            }

            if (bind!!.homeSyncStarredAlbumsCard.getVisibility() == View.VISIBLE) {
                bind!!.homeLinearLayoutContainer.addView(bind!!.homeSyncStarredAlbumsCard)
            }

            if (bind!!.homeSyncStarredArtistsCard.getVisibility() == View.VISIBLE) {
                bind!!.homeLinearLayoutContainer.addView(bind!!.homeSyncStarredArtistsCard)
            }

            for (sector in homeViewModel!!.homeSectorList) {
                if (!sector!!.isVisible) continue

                when (sector.id) {
                    Constants.HOME_SECTOR_DISCOVERY -> bind!!.homeLinearLayoutContainer.addView(bind!!.homeDiscoverSector)
                    Constants.HOME_SECTOR_MADE_FOR_YOU -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.homeSimilarTracksSector
                    )

                    Constants.HOME_SECTOR_BEST_OF -> bind!!.homeLinearLayoutContainer.addView(bind!!.homeBestOfArtistSector)
                    Constants.HOME_SECTOR_RADIO_STATION -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.homeRadioArtistSector
                    )

                    Constants.HOME_SECTOR_TOP_SONGS -> bind!!.homeLinearLayoutContainer.addView(bind!!.homeGridTracksSector)
                    Constants.HOME_SECTOR_STARRED_TRACKS -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.starredTracksSector
                    )

                    Constants.HOME_SECTOR_STARRED_ALBUMS -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.starredAlbumsSector
                    )

                    Constants.HOME_SECTOR_STARRED_ARTISTS -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.starredArtistsSector
                    )

                    Constants.HOME_SECTOR_NEW_RELEASES -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.homeNewReleasesSector
                    )

                    Constants.HOME_SECTOR_FLASHBACK -> bind!!.homeLinearLayoutContainer.addView(bind!!.homeFlashbackSector)
                    Constants.HOME_SECTOR_MOST_PLAYED -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.homeMostPlayedAlbumsSector
                    )

                    Constants.HOME_SECTOR_LAST_PLAYED -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.homeRecentlyPlayedAlbumsSector
                    )

                    Constants.HOME_SECTOR_RECENTLY_ADDED -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.homeRecentlyAddedAlbumsSector
                    )

                    Constants.HOME_SECTOR_PINNED_PLAYLISTS -> bind!!.homeLinearLayoutContainer.addView(
                        bind!!.pinnedPlaylistsSector
                    )

                    Constants.HOME_SECTOR_SHARED -> bind!!.homeLinearLayoutContainer.addView(bind!!.sharesSector)
                }
            }

            bind!!.homeLinearLayoutContainer.addView(bind!!.homeSectorRearrangementButton)
        }
    }

    private fun showPopupMenu(view: View?, menuResource: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.getMenuInflater().inflate(menuResource, popup.getMenu())

        popup.setOnMenuItemClickListener { menuItem: MenuItem? ->
            if (menuItem!!.getItemId() == R.id.menu_last_week_name) {
                homeViewModel!!.changeChronologyPeriod(getViewLifecycleOwner(), 0)
                bind!!.gridTracksPreTextView.setText(getString(R.string.home_title_last_week))
                true
            } else if (menuItem.getItemId() == R.id.menu_last_month_name) {
                homeViewModel!!.changeChronologyPeriod(getViewLifecycleOwner(), 1)
                bind!!.gridTracksPreTextView.setText(getString(R.string.home_title_last_month))
                true
            } else if (menuItem.getItemId() == R.id.menu_last_year_name) {
                homeViewModel!!.changeChronologyPeriod(getViewLifecycleOwner(), 2)
                bind!!.gridTracksPreTextView.setText(getString(R.string.home_title_last_year))
                true
            } else {
                false
            }
        }

        popup.show()
    }

    private fun refreshPlaylistView() {
        val handler = Handler(Looper.getMainLooper())

        val runnable = Runnable {
            if (getView() != null && bind != null && homeViewModel != null) homeViewModel!!.getPinnedPlaylists(
                getViewLifecycleOwner()
            )
        }

        handler.postDelayed(runnable, 100)
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
        bundle ?: return
        if (bundle.containsKey(Constants.MEDIA_MIX)) {
            val track = bundle.getParcelable<Child?>(Constants.TRACK_OBJECT)
            activity!!.setBottomSheetInPeek(true)

            if (mediaBrowserListenableFuture != null) {
                val playbackStarted = booleanArrayOf(false)
                Toast.makeText(
                    requireContext(),
                    R.string.bottom_sheet_generating_instant_mix,
                    Toast.LENGTH_SHORT
                ).show()
                homeViewModel!!.getMediaInstantMix(getViewLifecycleOwner(), track!!)
                    .observe(getViewLifecycleOwner(), object : Observer<MutableList<Child?>?> {
                        override fun onChanged(value: MutableList<Child?>?) {
                            if (playbackStarted[0] || value == null || value.isEmpty()) return
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (playbackStarted[0]) return@postDelayed
                                MediaManager.startQueue(mediaBrowserListenableFuture, value, 0)
                                playbackStarted[0] = true
                            }, 300)
                        }
                    })
            }
        } else if (bundle.containsKey(Constants.MEDIA_CHRONOLOGY)) {
            val media: MutableList<Child?>? =
                bundle.getParcelableArrayList<Child?>(Constants.TRACKS_OBJECT)
            MediaManager.startQueue(
                mediaBrowserListenableFuture,
                media!!,
                bundle.getInt(Constants.ITEM_POSITION)
            )
            activity!!.setBottomSheetInPeek(true)
        } else {
            MediaManager.startQueue(
                mediaBrowserListenableFuture, bundle.getParcelableArrayList<Child?>(
                    Constants.TRACKS_OBJECT
                )!!, bundle.getInt(Constants.ITEM_POSITION)
            )
            activity!!.setBottomSheetInPeek(true)
        }
        topSongAdapter!!.notifyDataSetChanged()
        starredSongAdapter!!.notifyDataSetChanged()
    }

    override fun onMediaLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
    }

    override fun onAlbumClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.albumPageFragment, bundle)
    }

    override fun onAlbumLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.albumBottomSheetDialog, bundle)
    }

    override fun onArtistClick(bundle: Bundle?) {
        bundle ?: return
        if (bundle.containsKey(Constants.MEDIA_MIX) && bundle.getBoolean(Constants.MEDIA_MIX)) {
            Snackbar.make(
                requireView(),
                R.string.artist_adapter_radio_station_starting,
                Snackbar.LENGTH_LONG
            )
                .setAnchorView(activity!!.binding!!.playerBottomSheet)
                .show()

            if (mediaBrowserListenableFuture != null) {
                homeViewModel!!.getArtistInstantMix(
                    getViewLifecycleOwner(), bundle.getParcelable<ArtistID3>(
                        Constants.ARTIST_OBJECT
                    )!!
                ).observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                    MusicUtil.ratingFilter(songs)
                    if (!songs!!.isEmpty()) {
                        MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                        activity!!.setBottomSheetInPeek(true)
                    }
                })
            }
        } else if (bundle.containsKey(Constants.MEDIA_BEST_OF) && bundle.getBoolean(Constants.MEDIA_BEST_OF)) {
            if (mediaBrowserListenableFuture != null) {
                homeViewModel!!.getArtistBestOf(
                    getViewLifecycleOwner(), bundle.getParcelable<ArtistID3>(
                        Constants.ARTIST_OBJECT
                    )!!
                ).observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                    MusicUtil.ratingFilter(songs)
                    if (!songs!!.isEmpty()) {
                        MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                        activity!!.setBottomSheetInPeek(true)
                    }
                })
            }
        } else {
            findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
        }
    }

    override fun onArtistLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
    }

    override fun onYearClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.songListPageFragment, bundle)
    }

    override fun onShareClick(bundle: Bundle?) {
        bundle ?: return
        val share = bundle.getParcelable<Share?>(Constants.SHARE_OBJECT)
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(share!!.url)
        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onPlaylistClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.playlistPageFragment, bundle)
    }

    override fun onPlaylistLongClick(bundle: Bundle?) {
        val dialog = PlaylistEditorDialog(object : PlaylistCallback {
            override fun onDismiss() {
                refreshPlaylistView()
            }
        })

        dialog.setArguments(bundle)
        dialog.show(activity!!.getSupportFragmentManager(), null)
    }

    override fun onShareLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.shareBottomSheetDialog, bundle)
    }

    private fun observeStarredSongsPlayback() {
        playbackViewModel!!.getCurrentSongId()
            .observe(getViewLifecycleOwner(), Observer { id: kotlin.String? ->
                if (starredSongAdapter != null) {
                    val playing = playbackViewModel!!.getIsPlaying().getValue()
                    starredSongAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
        playbackViewModel!!.getIsPlaying()
            .observe(getViewLifecycleOwner(), Observer { playing: Boolean? ->
                if (starredSongAdapter != null) {
                    val id = playbackViewModel!!.getCurrentSongId().getValue()
                    starredSongAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
    }

    private fun observeTopSongsPlayback() {
        playbackViewModel!!.getCurrentSongId()
            .observe(getViewLifecycleOwner(), Observer { id: kotlin.String? ->
                if (topSongAdapter != null) {
                    val playing = playbackViewModel!!.getIsPlaying().getValue()
                    topSongAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
        playbackViewModel!!.getIsPlaying()
            .observe(getViewLifecycleOwner(), Observer { playing: Boolean? ->
                if (topSongAdapter != null) {
                    val id = playbackViewModel!!.getCurrentSongId().getValue()
                    topSongAdapter!!.setPlaybackState(id, playing != null && playing)
                }
            })
    }

    private fun reapplyStarredSongsPlayback() {
        if (starredSongAdapter != null) {
            val id = playbackViewModel!!.getCurrentSongId().getValue()
            val playing = playbackViewModel!!.getIsPlaying().getValue()
            starredSongAdapter!!.setPlaybackState(id, playing != null && playing)
        }
    }

    private fun reapplyTopSongsPlayback() {
        if (topSongAdapter != null) {
            val id = playbackViewModel!!.getCurrentSongId().getValue()
            val playing = playbackViewModel!!.getIsPlaying().getValue()
            topSongAdapter!!.setPlaybackState(id, playing != null && playing)
        }
    }

    private fun setTopSongsMediaBrowserListenableFuture() {
        topSongAdapter!!.setMediaBrowserListenableFuture(mediaBrowserListenableFuture)
    }

    private fun setStarredSongsMediaBrowserListenableFuture() {
        starredSongAdapter!!.setMediaBrowserListenableFuture(mediaBrowserListenableFuture)
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}
