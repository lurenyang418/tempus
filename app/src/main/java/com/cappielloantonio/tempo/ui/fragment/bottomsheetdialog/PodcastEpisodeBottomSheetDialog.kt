package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.viewmodel.PodcastEpisodeBottomSheetViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

@UnstableApi
class PodcastEpisodeBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var podcastEpisodeBottomSheetViewModel: PodcastEpisodeBottomSheetViewModel? = null
    private var podcastEpisode: PodcastEpisode? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_podcast_episode_dialog, container, false)

        podcastEpisode = requireArguments().getParcelable<PodcastEpisode?>(Constants.PODCAST_OBJECT)

        podcastEpisodeBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<PodcastEpisodeBottomSheetViewModel>(
                PodcastEpisodeBottomSheetViewModel::class.java
            )
        podcastEpisodeBottomSheetViewModel!!.podcastEpisode = podcastEpisode

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
            podcastEpisodeBottomSheetViewModel!!.podcastEpisode!!.coverArtId,
            CustomGlideRequest.ResourceType.Podcast
        )
            .build()
            .into(coverPodcast)

        val titlePodcast = view.findViewById<TextView>(R.id.podcast_title_text_view)
        titlePodcast.setText(podcastEpisodeBottomSheetViewModel!!.podcastEpisode!!.title)

        titlePodcast.setSelected(true)

        val playNext = view.findViewById<TextView>(R.id.play_next_text_view)
        playNext.setOnClickListener(View.OnClickListener { v: View? ->
            // TODO
            // MediaManager.enqueue(mediaBrowserListenableFuture, podcast, true);
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        })

        val addToQueue = view.findViewById<TextView>(R.id.add_to_queue_text_view)
        addToQueue.setOnClickListener(View.OnClickListener { v: View? ->
            // TODO
            // MediaManager.enqueue(mediaBrowserListenableFuture, podcast, false);
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        })

        val download = view.findViewById<TextView>(R.id.download_text_view)
        download.setOnClickListener(View.OnClickListener { v: View? ->
            // TODO
            /* DownloadUtil.getDownloadTracker(requireContext()).download(
                    MappingUtil.mapMediaItem(podcast, false),
                    MappingUtil.mapDownload(podcast, null, null)
            ); */
            dismissBottomSheet()
        })

        val remove = view.findViewById<TextView>(R.id.remove_text_view)
        remove.setOnClickListener(View.OnClickListener { v: View? ->
            // TODO
            /* DownloadUtil.getDownloadTracker(requireContext()).remove(
                    MappingUtil.mapMediaItem(podcast, false),
                    MappingUtil.mapDownload(podcast, null, null)
            ); */
            dismissBottomSheet()
        })

        initDownloadUI(download, remove)

        val delete = view.findViewById<TextView>(R.id.delete_text_view)
        delete.setOnClickListener(View.OnClickListener { v: View? ->
            podcastEpisodeBottomSheetViewModel!!.deletePodcastEpisode()
            dismissBottomSheet()
        })

        val goToChannel = view.findViewById<TextView>(R.id.go_to_channel_text_view)
        goToChannel.setOnClickListener(View.OnClickListener { v: View? ->
            Toast.makeText(requireContext(), "Open the channel", Toast.LENGTH_SHORT).show()
            dismissBottomSheet()
        })
    }

    override fun onClick(v: View?) {
        dismissBottomSheet()
    }

    private fun dismissBottomSheet() {
        dismiss()
    }

    private fun initDownloadUI(download: TextView?, remove: TextView?) {
        // TODO
        /* if (DownloadUtil.getDownloadTracker(requireContext()).isDownloaded(MappingUtil.mapMediaItem(podcast, false))) {
            download.setVisibility(View.GONE);
            remove.setVisibility(View.VISIBLE);
        } else {
            download.setVisibility(View.VISIBLE);
            remove.setVisibility(View.GONE);
        } */
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