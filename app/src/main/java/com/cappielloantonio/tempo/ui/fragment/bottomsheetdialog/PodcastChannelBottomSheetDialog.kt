package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.PodcastChannel
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PodcastChannelBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

@UnstableApi
class PodcastChannelBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var podcastChannelBottomSheetViewModel: PodcastChannelBottomSheetViewModel? = null
    private var podcastChannel: PodcastChannel? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_podcast_channel_dialog, container, false)

        podcastChannel =
            requireArguments().getParcelable<PodcastChannel?>(Constants.PODCAST_CHANNEL_OBJECT)

        podcastChannelBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<PodcastChannelBottomSheetViewModel>(
                PodcastChannelBottomSheetViewModel::class.java
            )
        podcastChannelBottomSheetViewModel!!.podcastChannel = podcastChannel

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

    private fun init(view: View) {
        val coverPodcast = view.findViewById<ImageView>(R.id.podcast_cover_image_view)

        from(
            requireContext(),
            podcastChannelBottomSheetViewModel!!.podcastChannel!!.coverArtId,
            CustomGlideRequest.ResourceType.Podcast
        )
            .build()
            .into(coverPodcast)

        val titlePodcast = view.findViewById<TextView>(R.id.podcast_title_text_view)
        titlePodcast.setText(podcastChannelBottomSheetViewModel!!.podcastChannel!!.title)

        val delete = view.findViewById<TextView>(R.id.delete_text_view)
        delete.setOnClickListener(View.OnClickListener { v: View? ->
            podcastChannelBottomSheetViewModel!!.deletePodcastChannel()
            dismissBottomSheet()
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
}