package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.PlaylistRepository.AddToPlaylistCallback
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.PlaylistChooserDialog
import com.cappielloantonio.tempo.ui.dialog.RatingDialog
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.AssetLinkUtil.AssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.applyLinkAppearance
import com.cappielloantonio.tempo.util.AssetLinkUtil.buildAssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.clearLinkAppearance
import com.cappielloantonio.tempo.util.AssetLinkUtil.copyToClipboard
import com.cappielloantonio.tempo.util.AssetLinkUtil.getLabelRes
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.ExternalAudioWriter.downloadToUserDirectory
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MappingUtil.observeExternalAudioRefresh
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.isSharingEnabled
import com.cappielloantonio.tempo.viewmodel.HomeViewModel
import com.cappielloantonio.tempo.viewmodel.SongBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

@UnstableApi
class SongBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var homeViewModel: HomeViewModel? = null
    private var songBottomSheetViewModel: SongBottomSheetViewModel? = null
    private var song: Child? = null

    private var downloadButton: TextView? = null
    private var removeButton: TextView? = null
    private var assetLinkChipGroup: ChipGroup? = null
    private var songLinkChip: Chip? = null
    private var albumLinkChip: Chip? = null
    private var artistLinkChip: Chip? = null
    private var currentSongLink: AssetLink? = null
    private var currentAlbumLink: AssetLink? = null
    private var currentArtistLink: AssetLink? = null

    private var isFirstBatch = true
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_song_dialog, container, false)

        song = requireArguments().getParcelable<Child?>(Constants.TRACK_OBJECT)

        homeViewModel =
            ViewModelProvider(requireActivity()).get<HomeViewModel>(HomeViewModel::class.java)
        songBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<SongBottomSheetViewModel>(
                SongBottomSheetViewModel::class.java
            )
        songBottomSheetViewModel!!.setSong(song!!)

        init(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeExternalAudioRefresh(
            getViewLifecycleOwner(),
            Runnable { this.updateDownloadButtons() })
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
        val coverSong = view.findViewById<ImageView>(R.id.song_cover_image_view)
        from(
            requireContext(),
            songBottomSheetViewModel!!.getSong().coverArtId,
            CustomGlideRequest.ResourceType.Song
        )
            .build()
            .into(coverSong)

        val titleSong = view.findViewById<TextView>(R.id.song_title_text_view)
        titleSong.setText(songBottomSheetViewModel!!.getSong().title)

        titleSong.setSelected(true)

        val artistSong = view.findViewById<TextView>(R.id.song_artist_text_view)
        artistSong.setText(songBottomSheetViewModel!!.getSong().artist)

        initAssetLinkChips(view)
        bindAssetLinkView(coverSong, currentSongLink)
        bindAssetLinkView(titleSong, currentSongLink)
        bindAssetLinkView(
            artistSong,
            if (currentArtistLink != null) currentArtistLink else currentSongLink
        )

        val favoriteToggle = view.findViewById<ToggleButton>(R.id.button_favorite)
        favoriteToggle.setChecked(songBottomSheetViewModel!!.getSong().starred != null)
        favoriteToggle.setOnClickListener(View.OnClickListener { v: View? ->
            songBottomSheetViewModel!!.setFavorite(requireContext())
        })
        favoriteToggle.setOnLongClickListener(OnLongClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putParcelable(Constants.TRACK_OBJECT, song)

            val dialog = RatingDialog()
            dialog.setArguments(bundle)
            dialog.show(requireActivity().getSupportFragmentManager(), null)

            dismissBottomSheet()
            true
        })

        val playRadio = view.findViewById<TextView>(R.id.play_radio_text_view)
        playRadio.setOnClickListener { v ->
            val activity = getActivity() as MainActivity?
            if (activity == null) return@setOnClickListener

            val activityBrowserFuture: ListenableFuture<MediaBrowser>? =
                activity.getMediaBrowserListenableFuture()
            if (activityBrowserFuture == null) {
                Log.e(TAG, "MediaBrowser Future is null in MainActivity")
                return@setOnClickListener
            }

            isFirstBatch = true
            Toast.makeText(
                requireContext(),
                R.string.bottom_sheet_generating_instant_mix,
                Toast.LENGTH_SHORT
            ).show()
            songBottomSheetViewModel!!.getInstantMix(activity, song!!)
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

        val playNext = view.findViewById<TextView>(R.id.play_next_text_view)
        playNext.setOnClickListener(View.OnClickListener { v: View? ->
            MediaManager.enqueue(mediaBrowserListenableFuture, song, true)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        })

        val addToQueue = view.findViewById<TextView>(R.id.add_to_queue_text_view)
        addToQueue.setOnClickListener(View.OnClickListener { v: View? ->
            MediaManager.enqueue(mediaBrowserListenableFuture, song, false)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        })

        val rate = view.findViewById<TextView>(R.id.rate_text_view)
        rate.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putParcelable(Constants.TRACK_OBJECT, song)

            val dialog = RatingDialog()
            dialog.setArguments(bundle)
            dialog.show(requireActivity().getSupportFragmentManager(), null)
            dismissBottomSheet()
        })

        downloadButton = view.findViewById<TextView?>(R.id.download_text_view)
        downloadButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            if (getDownloadDirectoryUri() == null) {
                DownloadUtil.getDownloadTracker(requireContext()).download(
                    MappingUtil.mapDownload(song!!),
                    Download(song!!)
                )
            } else {
                downloadToUserDirectory(requireContext(), song)
            }
            dismissBottomSheet()
        })

        removeButton = view.findViewById<TextView?>(R.id.remove_text_view)
        removeButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            if (getDownloadDirectoryUri() == null) {
                DownloadUtil.getDownloadTracker(requireContext()).remove(
                    MappingUtil.mapDownload(song!!),
                    Download(song!!)
                )
            } else {
                ExternalAudioReader.delete(song!!)
            }
            dismissBottomSheet()
        })

        updateDownloadButtons()

        val playlistId = requireArguments().getString(Constants.PLAYLIST_ID)
        val itemPosition = requireArguments().getInt(Constants.ITEM_POSITION, -1)

        val removeFromPlaylist = view.findViewById<TextView>(R.id.remove_from_playlist_text_view)
        if (playlistId != null && itemPosition != -1) {
            removeFromPlaylist.setVisibility(View.VISIBLE)
            removeFromPlaylist.setOnClickListener(View.OnClickListener { v: View? ->
                songBottomSheetViewModel!!.removeFromPlaylist(
                    playlistId,
                    itemPosition,
                    object : AddToPlaylistCallback {
                        override fun onSuccess() {
                            Toast.makeText(
                                requireContext(),
                                R.string.playlist_chooser_dialog_toast_remove_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            dismissBottomSheet()
                        }

                        override fun onFailure() {
                            Toast.makeText(
                                requireContext(),
                                R.string.playlist_chooser_dialog_toast_remove_failure,
                                Toast.LENGTH_SHORT
                            ).show()
                            dismissBottomSheet()
                        }

                        override fun onAllSkipped() {
                            dismissBottomSheet()
                        }
                    })
            })
        }

        val addToPlaylist = view.findViewById<TextView>(R.id.add_to_playlist_text_view)
        addToPlaylist.setOnClickListener(View.OnClickListener { v: View? ->
            val bundle = Bundle()
            bundle.putParcelableArrayList(
                Constants.TRACKS_OBJECT,
                ArrayList<Child?>(mutableListOf<Child?>(song))
            )

            val dialog = PlaylistChooserDialog()
            dialog.setArguments(bundle)
            dialog.show(requireActivity().getSupportFragmentManager(), null)
            dismissBottomSheet()
        })

        val goToAlbum = view.findViewById<TextView>(R.id.go_to_album_text_view)
        goToAlbum.setOnClickListener(View.OnClickListener { v: View? ->
            songBottomSheetViewModel!!.album.observe(
                getViewLifecycleOwner(),
                Observer { album: AlbumID3? ->
                    if (album != null) {
                        val bundle = Bundle()
                        bundle.putParcelable(Constants.ALBUM_OBJECT, album)
                        NavHostFragment.findNavController(this)
                            .navigate(R.id.albumPageFragment, bundle)
                    } else Toast.makeText(
                        requireContext(),
                        getString(R.string.song_bottom_sheet_error_retrieving_album),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismissBottomSheet()
                })
        })

        goToAlbum.setVisibility(if (songBottomSheetViewModel!!.getSong().albumId != null) View.VISIBLE else View.GONE)

        val goToArtist = view.findViewById<TextView>(R.id.go_to_artist_text_view)
        goToArtist.setOnClickListener(View.OnClickListener { v: View? ->
            songBottomSheetViewModel!!.artist.observe(
                getViewLifecycleOwner(),
                Observer { artist: ArtistID3? ->
                    if (artist != null) {
                        val bundle = Bundle()
                        bundle.putParcelable(Constants.ARTIST_OBJECT, artist)
                        NavHostFragment.findNavController(this)
                            .navigate(R.id.artistPageFragment, bundle)
                    } else Toast.makeText(
                        requireContext(),
                        getString(R.string.song_bottom_sheet_error_retrieving_artist),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismissBottomSheet()
                })
        })

        goToArtist.setVisibility(if (songBottomSheetViewModel!!.getSong().artistId != null) View.VISIBLE else View.GONE)

        val share = view.findViewById<TextView>(R.id.share_text_view)
        share.setOnClickListener(View.OnClickListener { v: View? ->
            songBottomSheetViewModel!!.shareTrack()
                .observe(getViewLifecycleOwner(), Observer { sharedTrack: Share? ->
                    if (sharedTrack != null) {
                        val clipboardManager =
                            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData =
                            ClipData.newPlainText(getString(R.string.app_name), sharedTrack.url)
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

    private fun updateDownloadButtons() {
        if (downloadButton == null || removeButton == null) {
            return
        }

        if (getDownloadDirectoryUri() == null) {
            val downloaded =
                DownloadUtil.getDownloadTracker(requireContext()).isDownloaded(song!!.id)
            downloadButton!!.setVisibility(if (downloaded) View.GONE else View.VISIBLE)
            removeButton!!.setVisibility(if (downloaded) View.VISIBLE else View.GONE)
        } else {
            val hasLocal = ExternalAudioReader.getUri(song!!) != null
            downloadButton!!.setVisibility(if (hasLocal) View.GONE else View.VISIBLE)
            removeButton!!.setVisibility(if (hasLocal) View.VISIBLE else View.GONE)
        }
    }

    private fun initAssetLinkChips(root: View) {
        assetLinkChipGroup = root.findViewById<ChipGroup?>(R.id.asset_link_chip_group)
        songLinkChip = root.findViewById<Chip?>(R.id.asset_link_song_chip)
        albumLinkChip = root.findViewById<Chip?>(R.id.asset_link_album_chip)
        artistLinkChip = root.findViewById<Chip?>(R.id.asset_link_artist_chip)

        currentSongLink = bindAssetLinkChip(songLinkChip, AssetLinkUtil.TYPE_SONG, song!!.id)
        currentAlbumLink =
            bindAssetLinkChip(albumLinkChip, AssetLinkUtil.TYPE_ALBUM, song!!.albumId)
        currentArtistLink =
            bindAssetLinkChip(artistLinkChip, AssetLinkUtil.TYPE_ARTIST, song!!.artistId)
        syncAssetLinkGroupVisibility()
    }

    private fun bindAssetLinkChip(chip: Chip?, type: String, id: String?): AssetLink? {
        if (chip == null) return null
        if (id == null || id.isEmpty()) {
            clearAssetLinkChip(chip)
            return null
        }

        val label = getString(getLabelRes(type))
        val assetLink = buildAssetLink(type, id)
        if (assetLink == null) {
            clearAssetLinkChip(chip)
            return null
        }

        chip.setText(getString(R.string.asset_link_chip_text, label, assetLink.id))
        chip.setVisibility(View.VISIBLE)

        chip.setOnClickListener(View.OnClickListener { v: View? ->
            (requireActivity() as MainActivity).openAssetLink(assetLink)
        })

        chip.setOnLongClickListener(OnLongClickListener { v: View? ->
            copyToClipboard(requireContext(), assetLink)
            Toast.makeText(
                requireContext(),
                getString(R.string.asset_link_copied_toast, id),
                Toast.LENGTH_SHORT
            ).show()
            true
        })

        return assetLink
    }

    private fun clearAssetLinkChip(chip: Chip?) {
        if (chip == null) return
        chip.setVisibility(View.GONE)
        chip.setText("")
        chip.setOnClickListener(null)
        chip.setOnLongClickListener(null)
    }

    private fun syncAssetLinkGroupVisibility() {
        if (assetLinkChipGroup == null) return
        var hasVisible = false
        for (i in 0..<assetLinkChipGroup!!.getChildCount()) {
            val child = assetLinkChipGroup!!.getChildAt(i)
            if (child.getVisibility() == View.VISIBLE) {
                hasVisible = true
                break
            }
        }
        assetLinkChipGroup!!.setVisibility(if (hasVisible) View.VISIBLE else View.GONE)
    }

    private fun bindAssetLinkView(view: View?, assetLink: AssetLink?) {
        if (view == null) return
        if (assetLink == null) {
            clearLinkAppearance(view)
            view.setOnClickListener(null)
            view.setOnLongClickListener(null)
            view.setClickable(false)
            view.setLongClickable(false)
            return
        }

        view.setClickable(true)
        view.setLongClickable(true)
        applyLinkAppearance(view)
        view.setOnClickListener(View.OnClickListener { v: View? ->
            (requireActivity() as MainActivity).openAssetLink(
                assetLink,
                AssetLinkUtil.TYPE_SONG != assetLink.type
            )
        })
        view.setOnLongClickListener(OnLongClickListener { v: View? ->
            copyToClipboard(requireContext(), assetLink)
            Toast.makeText(
                requireContext(),
                getString(R.string.asset_link_copied_toast, assetLink.id),
                Toast.LENGTH_SHORT
            ).show()
            true
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
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        val raw: Any = mediaBrowserListenableFuture!!
        @Suppress("UNCHECKED_CAST")
        val future: Future<out androidx.media3.session.MediaController> = raw as Future<out androidx.media3.session.MediaController>
        MediaBrowser.releaseFuture(future)
    }

    private fun refreshShares() {
        homeViewModel!!.refreshShares(requireActivity())
    }

    companion object {
        private const val TAG = "SongBottomSheetDialog"
    }
}
