package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentArtistPageBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.helper.recyclerview.CustomLinearSnapHelper
import com.cappielloantonio.tempo.helper.recyclerview.GridItemDecoration
import com.cappielloantonio.tempo.interfaces.ClickCallback
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.ArtistInfo2
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.adapter.AlbumCatalogueAdapter
import com.cappielloantonio.tempo.ui.adapter.ArtistCatalogueAdapter
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getArtistDisplayBiography
import com.cappielloantonio.tempo.util.Preferences.setArtistDisplayBiography
import com.cappielloantonio.tempo.util.TileSizeManager.Companion.instance
import com.cappielloantonio.tempo.viewmodel.ArtistPageViewModel
import com.cappielloantonio.tempo.viewmodel.PlaybackViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.concurrent.Future

@UnstableApi
class ArtistPageFragment : Fragment(), ClickCallback {
    private var bind: FragmentArtistPageBinding? = null
    private var activity: MainActivity? = null
    private var artistPageViewModel: ArtistPageViewModel? = null
    private var playbackViewModel: PlaybackViewModel? = null

    private var songHorizontalAdapter: SongHorizontalAdapter? = null
    private var albumCatalogueAdapter: AlbumCatalogueAdapter? = null
    private var artistCatalogueAdapter: ArtistCatalogueAdapter? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private var spanCount = 2
    private var tileSpacing = 20

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = FragmentArtistPageBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()
        artistPageViewModel =
            ViewModelProvider(requireActivity()).get<ArtistPageViewModel>(ArtistPageViewModel::class.java)
        playbackViewModel =
            ViewModelProvider(requireActivity()).get<PlaybackViewModel>(PlaybackViewModel::class.java)

        instance!!.calculateTileSize(requireContext())
        spanCount = instance!!.getTileSpanCount(requireContext())
        tileSpacing = instance!!.getTileSpacing(requireContext())

        init(view)
        initAppBar()
        initArtistInfo()
        initPlayButtons()
        initTopSongsView()
        initAlbumsView()
        initSimilarArtistsView()

        return view
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()
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

    private fun init(view: View) {
        artistPageViewModel!!.setArtist(requireArguments().getParcelable<ArtistID3>(Constants.ARTIST_OBJECT)!!)

        bind!!.mostStreamedSongTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putString(Constants.MEDIA_BY_ARTIST, Constants.MEDIA_BY_ARTIST)
            bundle.putParcelable(Constants.ARTIST_OBJECT, artistPageViewModel!!.getArtist())
            activity!!.navController!!.navigate(
                R.id.action_artistPageFragment_to_songListPageFragment,
                bundle
            )
        })

        val favoriteToggle = view.findViewById<ToggleButton>(R.id.button_favorite)
        favoriteToggle.setChecked(artistPageViewModel!!.getArtist().starred != null)
        favoriteToggle.setOnClickListener(View.OnClickListener { v: View? ->
            artistPageViewModel!!.setFavorite(
                requireContext()
            )
        })

        val bioToggle = view.findViewById<Button>(R.id.button_toggle_bio)
        bioToggle.setOnClickListener(View.OnClickListener { v: View? ->
            Toast.makeText(
                getActivity(),
                R.string.artist_no_artist_info_toast,
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    private fun initAppBar() {
        activity!!.setSupportActionBar(bind!!.animToolbar)
        if (activity!!.getSupportActionBar() != null) activity!!.getSupportActionBar()!!
            .setDisplayHomeAsUpEnabled(true)

        bind!!.collapsingToolbar.setTitle(artistPageViewModel!!.getArtist().name)
        bind!!.animToolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> activity!!.navController!!.navigateUp() })
        bind!!.collapsingToolbar.setExpandedTitleColor(getResources().getColor(R.color.white, null))
    }

    private fun initArtistInfo() {
        artistPageViewModel!!.getArtistInfo(artistPageViewModel!!.getArtist().id)
            .observe(getViewLifecycleOwner(), Observer { artistInfo: ArtistInfo2? ->
                if (artistInfo == null) {
                    if (bind != null) bind!!.artistPageBioSector.setVisibility(View.GONE)
                } else {
                    if (getContext() != null && bind != null) {
                        val currentArtist = artistPageViewModel!!.getArtist()
                        val primaryId =
                            if (currentArtist.coverArtId != null && !currentArtist.coverArtId!!.trim { it <= ' ' }
                                    .isEmpty())
                                currentArtist.coverArtId
                            else
                                currentArtist.id

                        val fallbackId =
                            if (Objects.requireNonNull<String?>(primaryId) == currentArtist.coverArtId && currentArtist.id != null && (currentArtist.id != primaryId))
                                currentArtist.id
                            else
                                null

                        from(requireContext(), primaryId, CustomGlideRequest.ResourceType.Artist)
                            .build()
                            .listener(object : RequestListener<Drawable?> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable?>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    if (e != null) {
                                        e.message
                                        if (e.message!!.contains("400") && fallbackId != null) {
                                            Log.d(
                                                "ArtistCover",
                                                "Primary ID failed (400), trying fallback: " + fallbackId
                                            )

                                            from(
                                                requireContext(),
                                                fallbackId,
                                                CustomGlideRequest.ResourceType.Artist
                                            )
                                                .build()
                                                .into(bind!!.artistBackdropImageView)
                                            return@onLoadFailed true
                                        }
                                    }
                                    return@onLoadFailed false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any,
                                    target: Target<Drawable?>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return@onResourceReady false
                                }
                            })
                            .into(bind!!.artistBackdropImageView)
                    }

                    if (bind != null) {
                        val normalizedBio =
                            MusicUtil.forceReadableString(artistInfo.biography).trim { it <= ' ' }
                        val lastFmUrl = artistInfo.lastFmUrl

                        if (normalizedBio.isEmpty()) {
                            bind!!.bioTextView.setVisibility(View.GONE)
                        } else {
                            bind!!.bioTextView.setText(normalizedBio)
                        }

                        if (lastFmUrl == null) {
                            bind!!.bioMoreTextViewClickable.setVisibility(View.GONE)
                        } else {
                            bind!!.bioMoreTextViewClickable.setOnClickListener(View.OnClickListener { v: View? ->
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setData(Uri.parse(artistInfo.lastFmUrl))
                                startActivity(intent)
                            })
                            bind!!.bioMoreTextViewClickable.setVisibility(View.VISIBLE)
                        }

                        if (!normalizedBio.isEmpty() || lastFmUrl != null) {
                            val view: View = bind!!.getRoot()

                            val bioToggle = view.findViewById<Button>(R.id.button_toggle_bio)
                            bioToggle.setOnClickListener(View.OnClickListener { v: View? ->
                                if (bind != null) {
                                    val displayBio = getArtistDisplayBiography()
                                    setArtistDisplayBiography(!displayBio)
                                    bind!!.artistPageBioSector.setVisibility(if (displayBio) View.GONE else View.VISIBLE)
                                }
                            })

                            val displayBio = getArtistDisplayBiography()
                            bind!!.artistPageBioSector.setVisibility(if (displayBio) View.VISIBLE else View.GONE)
                        }
                    }
                }
            })
    }

    private fun initPlayButtons() {
        bind!!.artistPageShuffleButton.setOnClickListener(View.OnClickListener { v: View? ->
            artistPageViewModel!!.artistShuffleList.observe(
                getViewLifecycleOwner(),
                object : Observer<MutableList<Child?>?> {
                    @Suppress("UNCHECKED_CAST")
                    override fun onChanged(songs: MutableList<Child?>?) {
                        if (songs != null && !songs.isEmpty()) {
                            MediaManager.startQueue(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser>?, songs, 0)
                            activity!!.setBottomSheetInPeek(true)
                            artistPageViewModel!!.artistShuffleList.removeObserver(this)
                        }
                    }
                })
        })

        bind!!.artistPageRadioButton.setOnClickListener(View.OnClickListener { v: View? ->
            artistPageViewModel!!.artistInstantMix!!.observe(
                getViewLifecycleOwner(),
                object : Observer<MutableList<Child?>?> {
                    @Suppress("UNCHECKED_CAST")
                    override fun onChanged(songs: MutableList<Child?>?) {
                        if (songs != null && !songs.isEmpty()) {
                            MediaManager.startQueue(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser>?, songs, 0)
                            activity!!.setBottomSheetInPeek(true)
                            artistPageViewModel!!.artistInstantMix!!.removeObserver(this)
                        }
                    }
                })
        })
    }

    private fun initTopSongsView() {
        bind!!.mostStreamedSongRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))

        songHorizontalAdapter =
            SongHorizontalAdapter(getViewLifecycleOwner(), this, true, true, null as AlbumID3?)
        bind!!.mostStreamedSongRecyclerView.setAdapter(songHorizontalAdapter)
        setMediaBrowserListenableFuture()
        reapplyPlayback()
        artistPageViewModel!!.artistTopSongList.observe(
            getViewLifecycleOwner(),
            Observer { songs: MutableList<Child?>? ->
                if (songs == null) {
                    if (bind != null) bind!!.artistPageTopSongsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.artistPageTopSongsSector.setVisibility(if (!songs.isEmpty()) View.VISIBLE else View.GONE)
                    songHorizontalAdapter!!.setItems(songs)
                    reapplyPlayback()
                }
            })
    }

    private fun initAlbumsView() {
        bind!!.albumsRecyclerView.setLayoutManager(GridLayoutManager(requireContext(), spanCount))
        bind!!.albumsRecyclerView.addItemDecoration(
            GridItemDecoration(
                spanCount,
                tileSpacing,
                false
            )
        )
        bind!!.albumsRecyclerView.setHasFixedSize(true)

        albumCatalogueAdapter = AlbumCatalogueAdapter(this, false)
        bind!!.albumsRecyclerView.setAdapter(albumCatalogueAdapter)

        artistPageViewModel!!.albumList.observe(
            getViewLifecycleOwner(),
            Observer { albums: MutableList<AlbumID3?>? ->
                if (albums == null) {
                    if (bind != null) bind!!.artistPageAlbumsSector.setVisibility(View.GONE)
                } else {
                    if (bind != null) bind!!.artistPageAlbumsSector.setVisibility(if (!albums.isEmpty()) View.VISIBLE else View.GONE)
                    albumCatalogueAdapter!!.setItems(albums)
                }
            })
    }

    private fun initSimilarArtistsView() {
        bind!!.similarArtistsRecyclerView.setLayoutManager(
            GridLayoutManager(
                requireContext(),
                spanCount
            )
        )
        bind!!.similarArtistsRecyclerView.addItemDecoration(
            GridItemDecoration(
                spanCount,
                tileSpacing,
                false
            )
        )
        bind!!.similarArtistsRecyclerView.setHasFixedSize(true)

        artistCatalogueAdapter = ArtistCatalogueAdapter(this)
        bind!!.similarArtistsRecyclerView.setAdapter(artistCatalogueAdapter)

        artistPageViewModel!!.getArtistInfo(artistPageViewModel!!.getArtist().id)
            .observe(getViewLifecycleOwner(), Observer { artist: ArtistInfo2? ->
                if (artist == null) {
                    if (bind != null) bind!!.similarArtistSector.setVisibility(View.GONE)
                } else {
                    if (bind != null && artist.similarArtists != null) bind!!.similarArtistSector.setVisibility(
                        if (!artist.similarArtists!!.isEmpty()) View.VISIBLE else View.GONE
                    )

                    val artists: MutableList<ArtistID3?> = ArrayList<ArtistID3?>()

                    if (artist.similarArtists != null) {
                        artists.addAll(artist.similarArtists!!)
                    }

                    artistCatalogueAdapter!!.setItems(artists)
                }
            })

        val similarArtistSnapHelper = CustomLinearSnapHelper()
        similarArtistSnapHelper.attachToRecyclerView(bind!!.similarArtistsRecyclerView)
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
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        val raw: Any = mediaBrowserListenableFuture!!
        @Suppress("UNCHECKED_CAST")
        val future: Future<out androidx.media3.session.MediaController> = raw as Future<out androidx.media3.session.MediaController>
        MediaBrowser.releaseFuture(future)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onMediaClick(bundle: Bundle?) {
        MediaManager.startQueue(
            mediaBrowserListenableFuture as ListenableFuture<MediaBrowser>?, bundle?.getParcelableArrayList<Child?>(
                Constants.TRACKS_OBJECT
            )!!, bundle?.getInt(Constants.ITEM_POSITION) ?: 0
        )
        activity?.setBottomSheetInPeek(true)
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
        findNavController(requireView()).navigate(R.id.artistPageFragment, bundle)
    }

    override fun onArtistLongClick(bundle: Bundle?) {
        findNavController(requireView()).navigate(R.id.artistBottomSheetDialog, bundle)
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

    @Suppress("UNCHECKED_CAST")
    private fun setMediaBrowserListenableFuture() {
        songHorizontalAdapter!!.setMediaBrowserListenableFuture(mediaBrowserListenableFuture as ListenableFuture<MediaBrowser>?)
    }
}