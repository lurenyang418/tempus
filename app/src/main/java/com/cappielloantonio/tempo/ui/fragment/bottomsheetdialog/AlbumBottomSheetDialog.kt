package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserDialog
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.ExternalAudioReader.delete
import com.cappielloantonio.tempo.util.ExternalAudioWriter.downloadToUserDirectory
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MappingUtil.observeExternalAudioRefresh
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.isSharingEnabled
import com.cappielloantonio.tempo.viewmodel.AlbumBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future
import java.util.Collections
import java.util.function.Consumer
import java.util.stream.Collectors

@UnstableApi
class AlbumBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var homeViewModel: HomeViewModel? = null
    private var albumBottomSheetViewModel: AlbumBottomSheetViewModel? = null
    private var album: AlbumID3? = null

    private var removeAllTextView: TextView? = null
    private var currentAlbumTracks: MutableList<Child?>? = mutableListOf<Child?>()
    private var currentAlbumMediaItems = mutableListOf<MediaItem?>()

    private var isFirstBatch = true

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_album_dialog, container, false)

        album = this.requireArguments().getParcelable<AlbumID3?>(Constants.ALBUM_OBJECT)

        homeViewModel =
            ViewModelProvider(requireActivity()).get<HomeViewModel>(HomeViewModel::class.java)
        albumBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<AlbumBottomSheetViewModel>(
                AlbumBottomSheetViewModel::class.java
            )
        albumBottomSheetViewModel!!.setAlbum(album!!)

        init(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeExternalAudioRefresh(
            getViewLifecycleOwner(),
            Runnable { this.updateRemoveAllVisibility() })
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    private fun init(view: View) {
        val coverAlbum = view.findViewById<ImageView>(R.id.album_cover_image_view)
        from(
            requireContext(),
            albumBottomSheetViewModel!!.getAlbum().coverArtId,
            CustomGlideRequest.ResourceType.Album
        )
            .build()
            .into(coverAlbum)

        val titleAlbum = view.findViewById<TextView>(R.id.album_title_text_view)
        titleAlbum.setText(albumBottomSheetViewModel!!.getAlbum().name)
        titleAlbum.setSelected(true)

        val artistAlbum = view.findViewById<TextView>(R.id.album_artist_text_view)
        artistAlbum.setText(albumBottomSheetViewModel!!.getAlbum().artist)

        val favoriteToggle = view.findViewById<ToggleButton>(R.id.button_favorite)
        favoriteToggle.setChecked(albumBottomSheetViewModel!!.getAlbum().starred != null)
        favoriteToggle.setOnClickListener(View.OnClickListener { v: View? ->
            albumBottomSheetViewModel!!.setFavorite(
                requireContext()
            )
        })

        val playRadio = view.findViewById<TextView>(R.id.play_radio_text_view)
        playRadio.setOnClickListener { v ->
            val activity = getActivity() as MainActivity?
            if (activity == null) return@setOnClickListener

            val activityBrowserFuture =
                activity.getMediaBrowserListenableFuture()
            if (activityBrowserFuture == null) return@setOnClickListener

            isFirstBatch = true
            Toast.makeText(
                requireContext(),
                R.string.bottom_sheet_generating_instant_mix,
                Toast.LENGTH_SHORT
            ).show()
            albumBottomSheetViewModel!!.getAlbumInstantMix(activity, album!!)
                .observe(activity, Observer { media: MutableList<Child?>? ->
                    if (media == null || media.isEmpty()) return@Observer
                    if (getActivity() == null) return@Observer

                    MusicUtil.ratingFilter(media)
                    if (isFirstBatch) {
                        isFirstBatch = false

                        MediaManager.startQueue(activityBrowserFuture, media, 0)
                        activity.setBottomSheetInPeek(true)

                        if (isAdded()) {
                            dismissBottomSheet()
                        }
                    } else {
                        MediaManager.enqueue(activityBrowserFuture, media, true)
                    }
                })
        }


        val playRandom = view.findViewById<TextView>(R.id.play_random_text_view)
        playRandom.setOnClickListener(View.OnClickListener { v: View? ->
            val albumRepository = AlbumRepository()
            albumRepository.getAlbumTracks(album!!.id)
                .observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                    val safeSongs = songs ?: return@Observer
                    Collections.shuffle(safeSongs)
                    MediaManager.startQueue(mediaBrowserListenableFuture, safeSongs, 0)
                    (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                    dismissBottomSheet()
                })
        })

        val playNext = view.findViewById<TextView>(R.id.play_next_text_view)
        playNext.setOnClickListener { v ->
            albumBottomSheetViewModel!!.albumTracks.observe(
                getViewLifecycleOwner(),
                Observer { songs: MutableList<Child?>? ->
                    MediaManager.enqueue(mediaBrowserListenableFuture, songs ?: mutableListOf(), true)
                    (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                    dismissBottomSheet()
                })
        }

        val addToQueue = view.findViewById<TextView>(R.id.add_to_queue_text_view)
        addToQueue.setOnClickListener { v ->
            albumBottomSheetViewModel!!.albumTracks.observe(
                getViewLifecycleOwner(),
                Observer { songs: MutableList<Child?>? ->
                    MediaManager.enqueue(mediaBrowserListenableFuture, songs ?: mutableListOf(), false)
                    (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                    dismissBottomSheet()
                })
        }

        val downloadAll = view.findViewById<TextView>(R.id.download_all_text_view)
        albumBottomSheetViewModel!!.albumTracks.observe(
            getViewLifecycleOwner(),
            Observer { songs: MutableList<Child?>? ->
                val nonNullSongs: MutableList<Child> = songs?.filterNotNull()?.toMutableList() ?: mutableListOf()
                val mediaItems: MutableList<MediaItem?> = MappingUtil.mapDownloads(nonNullSongs)
                val downloads = ArrayList(nonNullSongs.map { Download(it) })
                downloadAll.setOnClickListener { v ->
                    if (getDownloadDirectoryUri() == null) {
                        DownloadUtil.getDownloadTracker(requireContext())
                            .download(mediaItems, downloads)
                    } else {
                        nonNullSongs.forEach(Consumer { child: Child? ->
                            downloadToUserDirectory(
                                requireContext(),
                                child!!
                            )
                        })
                    }
                    dismissBottomSheet()
                }
            })

        val addToPlaylist = view.findViewById<TextView>(R.id.add_to_playlist_text_view)
        addToPlaylist.setOnClickListener(View.OnClickListener { v: View? ->
            albumBottomSheetViewModel!!.albumTracks.observe(
                getViewLifecycleOwner(),
                Observer { songs: MutableList<Child?>? ->
                    val bundle = Bundle()
                    bundle.putParcelableArrayList(
                        Constants.TRACKS_OBJECT,
                        ArrayList(songs ?: emptyList())
                    )

                    val dialog = PlaylistChooserDialog()
                    dialog.setArguments(bundle)
                    dialog.show(requireActivity().getSupportFragmentManager(), null)
                    dismissBottomSheet()
                })
        })

        removeAllTextView = view.findViewById<TextView?>(R.id.remove_all_text_view)
        albumBottomSheetViewModel!!.albumTracks.observe(
            getViewLifecycleOwner(),
            Observer { songs: MutableList<Child?>? ->
                currentAlbumTracks = if (songs != null) songs else mutableListOf<Child?>()
                currentAlbumMediaItems = MappingUtil.mapDownloads(currentAlbumTracks!!)

                removeAllTextView!!.setOnClickListener { v ->
                    if (getDownloadDirectoryUri() == null) {
                        val downloads: MutableList<Download?> = ArrayList(currentAlbumTracks!!.filterNotNull().map { Download(it) })
                        DownloadUtil.getDownloadTracker(requireContext())
                            .remove(currentAlbumMediaItems, downloads)
                    } else {
                        currentAlbumTracks!!.forEach(Consumer { obj: Child? -> delete(obj!!) })
                    }
                    dismissBottomSheet()
                }
                updateRemoveAllVisibility()
            })

        val goToArtist = view.findViewById<TextView>(R.id.go_to_artist_text_view)
        goToArtist.setOnClickListener(View.OnClickListener { v: View? ->
            albumBottomSheetViewModel!!.artist.observe(
                getViewLifecycleOwner(),
                Observer { artist: ArtistID3? ->
                    if (artist != null) {
                        val bundle = Bundle()
                        bundle.putParcelable(Constants.ARTIST_OBJECT, artist)
                        NavHostFragment.findNavController(this)
                            .navigate(R.id.artistPageFragment, bundle)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.album_error_retrieving_artist),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dismissBottomSheet()
                })
        })

        val share = view.findViewById<TextView>(R.id.share_text_view)
        share.setOnClickListener(View.OnClickListener { v: View? ->
            albumBottomSheetViewModel!!.shareAlbum()
                .observe(getViewLifecycleOwner(), Observer { sharedAlbum: Share? ->
                    if (sharedAlbum != null) {
                        val clipboardManager =
                            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData =
                            ClipData.newPlainText(getString(R.string.app_name), sharedAlbum.url)
                        clipboardManager.setPrimaryClip(clipData)
                        refreshShares()
                        dismissBottomSheet()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.share_unsupported_error),
                            Toast.LENGTH_SHORT
                        ).show()
                        dismissBottomSheet()
                    }
                })
        })

        share.setVisibility(if (isSharingEnabled()) View.VISIBLE else View.GONE)
    }

    override fun onClick(v: View?) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    private fun updateRemoveAllVisibility() {
        if (removeAllTextView == null) {
            return
        }

        if (currentAlbumTracks == null || currentAlbumTracks!!.isEmpty()) {
            removeAllTextView!!.setVisibility(View.GONE)
            return
        }

        if (getDownloadDirectoryUri() == null) {
            val mediaItems = currentAlbumMediaItems
            if (mediaItems.isEmpty()) {
                removeAllTextView!!.setVisibility(View.GONE)
            } else if (DownloadUtil.getDownloadTracker(requireContext())
                    .areDownloaded(mediaItems)
            ) {
                removeAllTextView!!.setVisibility(View.VISIBLE)
            } else {
                removeAllTextView!!.setVisibility(View.GONE)
            }
        } else {
            val hasLocal = currentAlbumTracks!!.any { song -> song != null && ExternalAudioReader.getUri(song) != null }
            removeAllTextView!!.setVisibility(if (hasLocal) View.VISIBLE else View.GONE)
        }
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

    private fun refreshShares() {
        homeViewModel!!.refreshShares(requireActivity())
    }

    companion object {
        private const val TAG = "AlbumBottomSheetDialog"
    }
}
