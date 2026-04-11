package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.os.BundleCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.viewmodel.ArtistBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

@UnstableApi
class ArtistBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var artistBottomSheetViewModel: ArtistBottomSheetViewModel? = null
    private var artist: ArtistID3? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private var isFirstBatch = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_artist_dialog, container, false)

        artist = BundleCompat.getParcelable(
            requireArguments(),
            Constants.ARTIST_OBJECT,
            ArtistID3::class.java
        )

        artistBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<ArtistBottomSheetViewModel>(
                ArtistBottomSheetViewModel::class.java
            )
        artistBottomSheetViewModel!!.setArtist(artist!!)

        init(view)

        return view
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    // TODO Use the viewmodel as a conduit and avoid direct calls
    private fun init(view: View) {
        val coverArtist = view.findViewById<ImageView>(R.id.artist_cover_image_view)
        from(
            requireContext(),
            artistBottomSheetViewModel!!.getArtist().coverArtId,
            CustomGlideRequest.ResourceType.Artist
        )
            .build()
            .into(coverArtist)

        val nameArtist = view.findViewById<TextView>(R.id.song_title_text_view)
        nameArtist.setText(artistBottomSheetViewModel!!.getArtist().name)
        nameArtist.setSelected(true)

        val favoriteToggle = view.findViewById<ToggleButton>(R.id.button_favorite)
        favoriteToggle.setChecked(artistBottomSheetViewModel!!.getArtist().starred != null)
        favoriteToggle.setOnClickListener(View.OnClickListener { v: View? ->
            artistBottomSheetViewModel!!.setFavorite(requireContext())
        })

        val playRadio = view.findViewById<TextView>(R.id.play_radio_text_view)
        playRadio.setOnClickListener { v ->
            val activity = getActivity() as MainActivity?
            if (activity == null) return@setOnClickListener

            val activityBrowserFuture: ListenableFuture<MediaBrowser>? =
                activity.getMediaBrowserListenableFuture()
            if (activityBrowserFuture == null) return@setOnClickListener

            isFirstBatch = true
            Toast.makeText(
                requireContext(),
                R.string.bottom_sheet_generating_instant_mix,
                Toast.LENGTH_SHORT
            ).show()
            artistBottomSheetViewModel!!.getArtistInstantMix(activity, artist!!)
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
            val artistRepository = ArtistRepository()
            artistRepository.getRandomSong(artist!!, 50)
                .observe(getViewLifecycleOwner(), Observer { songs: MutableList<Child?>? ->
                    MusicUtil.ratingFilter(songs)
                    if (!songs!!.isEmpty()) {
                        MediaManager.startQueue(mediaBrowserListenableFuture, songs, 0)
                        (requireActivity() as MainActivity).setBottomSheetInPeek(true)
                    }
                    dismissBottomSheet()
                })
        })
    }

    override fun onClick(v: View?) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
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

    companion object {
        private const val TAG = "AlbumBottomSheetDialog"
    }
}
