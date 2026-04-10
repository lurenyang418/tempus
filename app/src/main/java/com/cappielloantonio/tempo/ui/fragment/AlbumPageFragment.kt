package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentAlbumPageBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.AlbumInfo
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserDialog
import com.cappielloantonio.tempo.ui.dialog.RatingDialog
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.AssetLinkUtil.AssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.applyLinkAppearance
import com.cappielloantonio.tempo.util.AssetLinkUtil.buildAssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.clearLinkAppearance
import com.cappielloantonio.tempo.util.AssetLinkUtil.copyToClipboard
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioWriter.downloadToUserDirectory
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.showAlbumDetail
import com.cappielloantonio.tempo.viewmodel.AlbumPageViewModel
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future
import java.util.Collections
import java.util.Objects
import java.util.function.Consumer
import java.util.stream.Collectors

@UnstableApi
class AlbumPageFragment : Fragment(), ClickCallback {
    private var bind: FragmentAlbumPageBinding? = null
    private var activity: MainActivity? = null
    private var albumPageViewModel: AlbumPageViewModel? = null
    private var playbackViewModel: PlaybackViewModel? = null
    private var songHorizontalAdapter: SongHorizontalAdapter? = null
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    /** @noinspection deprecation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /** @noinspection deprecation
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.album_page_menu, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentAlbumPageBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        albumPageViewModel =
            ViewModelProvider(requireActivity()).get<AlbumPageViewModel>(AlbumPageViewModel::class.java)
        playbackViewModel =
            ViewModelProvider(requireActivity()).get<PlaybackViewModel>(PlaybackViewModel::class.java)

        init(view)
        initAppBar()
        initAlbumInfoTextButton()
        initAlbumNotes()
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

    /** @noinspection deprecation
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.action_rate_album) {
            val bundle = Bundle()
            val album = albumPageViewModel!!.album.getValue()
            bundle.putParcelable(Constants.ALBUM_OBJECT, album)
            val dialog = RatingDialog()
            dialog.setArguments(bundle)
            dialog.show(requireActivity().getSupportFragmentManager(), null)
            return true
        }

        if (item.getItemId() == R.id.action_download_album) {
            albumPageViewModel!!.albumSongLiveList.observe(
                getViewLifecycleOwner(),
                Observer { songs: MutableList<Child?>? ->
                    val nonNullSongs = songs ?: mutableListOf()
                    if (getDownloadDirectoryUri() == null) {
                        DownloadUtil.getDownloadTracker(requireContext()).download(
                            MappingUtil.mapDownloads(nonNullSongs),
                            nonNullSongs.stream().map<Download?> { child: Child? -> Download(child!!) }
                                .collect(
                                    Collectors.toList()
                                )
                        )
                    } else {
                        nonNullSongs.forEach(Consumer { child: Child? ->
                            downloadToUserDirectory(
                                requireContext(),
                                child!!
                            )
                        })
                    }
                })
            return true
        }
        if (item.getItemId() == R.id.action_add_to_playlist) {
            albumPageViewModel!!.albumSongLiveList.observe(
                getViewLifecycleOwner(),
                Observer { songs: MutableList<Child?>? ->
                    val bundle = Bundle()
                    bundle.putParcelableArrayList(Constants.TRACKS_OBJECT, ArrayList<Child?>(songs))

                    val dialog = PlaylistChooserDialog()
                    dialog.setArguments(bundle)
                    dialog.show(requireActivity().getSupportFragmentManager(), null)
                })
            return true
        }

        return false
    }

    private fun init(view: View) {
        val albumArg: AlbumID3? =
            checkNotNull(requireArguments().getParcelable<AlbumID3?>(Constants.ALBUM_OBJECT))
        albumPageViewModel!!.setAlbum(getViewLifecycleOwner(), albumArg!!)
        val favoriteToggle = view.findViewById<ToggleButton>(R.id.button_favorite)
        favoriteToggle.setChecked(albumArg.starred != null)

        favoriteToggle.setOnClickListener(View.OnClickListener { v: View? ->
            albumPageViewModel!!.setFavorite()
        })
        albumPageViewModel!!.album.observe(getViewLifecycleOwner(), Observer { album: AlbumID3? ->
            if (album != null) {
                favoriteToggle.setChecked(album.starred != null)
            }
        })
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.animToolbar)

        if (activity!!.getSupportActionBar() != null) {
            activity!!.getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
            activity!!.getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        }

        albumPageViewModel!!.album.observe(getViewLifecycleOwner(), Observer { album: AlbumID3? ->
            if (bind != null && album != null) {
                bind!!.animToolbar.setTitle(album.name)

                bind!!.albumNameLabel.setText(album.name)
                bind!!.albumArtistLabel.setText(album.artist)
                applyLinkAppearance(bind!!.albumArtistLabel)
                val artistLink = buildArtistLink(album)
                bind!!.albumArtistLabel.setOnLongClickListener { v ->
                    if (artistLink != null) {
                        copyToClipboard(requireContext(), artistLink)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.asset_link_copied_toast, artistLink.id),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnLongClickListener true
                    }
                    false
                }
                bind!!.albumReleaseYearLabel.setText(if (album.year != 0) album.year.toString() else "")
                if (album.year != 0) {
                    bind!!.albumReleaseYearLabel.setVisibility(View.VISIBLE)
                    applyLinkAppearance(bind!!.albumReleaseYearLabel)
                    bind!!.albumReleaseYearLabel.setOnClickListener(View.OnClickListener { v: View? ->
                        openYearLink(
                            album.year
                        )
                    })
                    bind!!.albumReleaseYearLabel.setOnLongClickListener(OnLongClickListener { v: View? ->
                        val yearLink = buildYearLink(album.year)
                        if (yearLink != null) {
                            copyToClipboard(requireContext(), yearLink)
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.asset_link_copied_toast, yearLink.id),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    })
                } else {
                    bind!!.albumReleaseYearLabel.setVisibility(View.GONE)
                    bind!!.albumReleaseYearLabel.setOnClickListener(null)
                    bind!!.albumReleaseYearLabel.setOnLongClickListener(null)
                    clearLinkAppearance(bind!!.albumReleaseYearLabel)
                }
                bind!!.albumSongCountDurationTextview.setText(
                    getString(
                        R.string.album_page_tracks_count_and_duration,
                        album.songCount,
                        if (album.duration != null) album.duration!! / 60 else 0
                    )
                )
                if (album.genre != null && !album.genre!!.isEmpty()) {
                    bind!!.albumGenresTextview.setText(album.genre)
                    bind!!.albumGenresTextview.setVisibility(View.VISIBLE)
                } else {
                    bind!!.albumGenresTextview.setVisibility(View.GONE)
                }

                if (album.releaseDate != null && album.originalReleaseDate != null) {
                    if (album.releaseDate!!.getFormattedDate() != null || album.originalReleaseDate!!.getFormattedDate() != null) bind!!.albumReleaseYearsTextview.setVisibility(
                        View.VISIBLE
                    )
                    else bind!!.albumReleaseYearsTextview.setVisibility(View.GONE)

                    if (album.releaseDate!!.getFormattedDate() == null || album.originalReleaseDate!!.getFormattedDate() == null) {
                        bind!!.albumReleaseYearsTextview.setText(
                            getString(
                                R.string.album_page_release_date_label,
                                if (album.releaseDate != null) album.releaseDate!!.getFormattedDate() else album.originalReleaseDate!!.getFormattedDate()
                            )
                        )
                    }

                    if (album.releaseDate!!.getFormattedDate() != null && album.originalReleaseDate!!.getFormattedDate() != null) {
                        if (album.releaseDate!!.year == album.originalReleaseDate!!.year && album.releaseDate!!.month == album.originalReleaseDate!!.month && album.releaseDate!!.day == album.originalReleaseDate!!.day) {
                            bind!!.albumReleaseYearsTextview.setText(
                                getString(
                                    R.string.album_page_release_date_label,
                                    album.releaseDate!!.getFormattedDate()
                                )
                            )
                        } else {
                            bind!!.albumReleaseYearsTextview.setText(
                                getString(
                                    R.string.album_page_release_dates_label,
                                    album.releaseDate!!.getFormattedDate(),
                                    album.originalReleaseDate!!.getFormattedDate()
                                )
                            )
                        }
                    }
                }
            }
        })

        bind!!.animToolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> activity!!.navController!!.navigateUp() })

        Objects.requireNonNull<Drawable?>(bind!!.animToolbar.getOverflowIcon())
            .setTint(requireContext().getResources().getColor(R.color.titleTextColor, null))

        bind!!.albumOtherInfoButton.setOnClickListener(View.OnClickListener { v: View? ->
            if (bind!!.albumDetailView.getVisibility() == View.GONE) {
                bind!!.albumDetailView.setVisibility(View.VISIBLE)
                bind!!.albumNameLabel.setMaxLines(Int.Companion.MAX_VALUE)
            } else if (bind!!.albumDetailView.getVisibility() == View.VISIBLE) {
                bind!!.albumDetailView.setVisibility(View.GONE)
                bind!!.albumNameLabel.setMaxLines(2)
            }
        })

        if (showAlbumDetail()) {
            bind!!.albumDetailView.setVisibility(View.VISIBLE)
        }
    }

    private fun initAlbumInfoTextButton() {
        bind!!.albumArtistLabel.setOnClickListener(View.OnClickListener { v: View? ->
            albumPageViewModel!!.artist.observe(
                getViewLifecycleOwner(),
                Observer { artist: ArtistID3? ->
                    if (artist != null) {
                        val bundle = Bundle()
                        bundle.putParcelable(Constants.ARTIST_OBJECT, artist)
                        activity!!.navController!!.navigate(
                            R.id.action_albumPageFragment_to_artistPageFragment,
                            bundle
                        )
                    } else Toast.makeText(
                        requireContext(),
                        getString(R.string.album_error_retrieving_artist),
                        Toast.LENGTH_SHORT
                    ).show()
                })
        })
    }

    private fun initAlbumNotes() {
        albumPageViewModel!!.albumInfo.observe(
            getViewLifecycleOwner(),
            Observer { albumInfo: AlbumInfo? ->
                if (albumInfo != null) {
                    if (bind != null) bind!!.albumNotesTextview.setVisibility(View.VISIBLE)
                    if (bind != null) bind!!.albumNotesTextview.setText(
                        MusicUtil.forceReadableString(
                            albumInfo.notes
                        )
                    )

                    if (bind != null && albumInfo.lastFmUrl != null && !albumInfo.lastFmUrl!!.isEmpty()) {
                        bind!!.albumNotesTextview.setOnClickListener(View.OnClickListener { v: View? ->
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setData(Uri.parse(albumInfo.lastFmUrl))
                            startActivity(intent)
                        })
                    }
                } else {
                    if (bind != null) bind!!.albumNotesTextview.setVisibility(View.GONE)
                }
            })
    }

    private fun initMusicButton() {
        albumPageViewModel!!.albumSongLiveList.observe(
            getViewLifecycleOwner(),
            Observer { songs: MutableList<Child?>? ->
                if (bind != null && !songs!!.isEmpty()) {
                    bind!!.albumPagePlayButton.setOnClickListener(View.OnClickListener { v: View? ->
                        MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                        activity!!.setBottomSheetInPeek(true)
                    })

                    bind!!.albumPageShuffleButton.setOnClickListener(View.OnClickListener { v: View? ->
                        Collections.shuffle(songs)
                        MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                        activity!!.setBottomSheetInPeek(true)
                    })
                }
                if (bind != null && songs!!.isEmpty()) {
                    bind!!.albumPagePlayButton.setEnabled(false)
                    bind!!.albumPageShuffleButton.setEnabled(false)
                }
            })
    }

    private fun initBackCover() {
        albumPageViewModel!!.album.observe(getViewLifecycleOwner(), Observer { album: AlbumID3? ->
            if (bind != null && album != null) {
                from(
                    requireContext(),
                    album.coverArtId,
                    CustomGlideRequest.ResourceType.Album
                ).build().into(bind!!.albumCoverImageView)
            }
        })
    }

    private fun initSongsView() {
        albumPageViewModel!!.album.observe(getViewLifecycleOwner(), Observer { album: AlbumID3? ->
            if (bind != null && album != null) {
                bind!!.songRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
                bind!!.songRecyclerView.setHasFixedSize(true)

                songHorizontalAdapter =
                    SongHorizontalAdapter(getViewLifecycleOwner(), this, false, false, album)
                bind!!.songRecyclerView.setAdapter(songHorizontalAdapter)
                setMediaBrowserListenableFuture()
                reapplyPlayback()

                albumPageViewModel!!.albumSongLiveList.observe(
                    getViewLifecycleOwner(),
                    Observer { songs: MutableList<Child?>? ->
                        songHorizontalAdapter!!.setItems(songs)
                        reapplyPlayback()
                    })
            }
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
        MediaManager.startQueue(
            mediaBrowserListenableFuture, bundle?.getParcelableArrayList<Child?>(
                Constants.TRACKS_OBJECT
            )!!, bundle?.getInt(Constants.ITEM_POSITION) ?: 0
        )
        activity!!.setBottomSheetInPeek(true)
    }

    override fun onMediaLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle)
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

    private fun openYearLink(year: Int) {
        val link = buildYearLink(year)
        if (link != null) {
            activity!!.openAssetLink(link)
        }
    }

    private fun buildYearLink(year: Int): AssetLink? {
        if (year <= 0) return null
        return buildAssetLink(AssetLinkUtil.TYPE_YEAR, year.toString())
    }

    private fun buildArtistLink(album: AlbumID3?): AssetLink? {
        if (album == null || album.artistId == null || album.artistId!!.isEmpty()) {
            return null
        }
        return buildAssetLink(AssetLinkUtil.TYPE_ARTIST, album.artistId)
    }
}
