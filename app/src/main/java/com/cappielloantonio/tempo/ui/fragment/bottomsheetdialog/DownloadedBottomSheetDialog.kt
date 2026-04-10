package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.ExternalAudioReader.delete
import java.util.concurrent.Future
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections
import java.util.Random
import java.util.function.Consumer
import java.util.stream.Collectors

@UnstableApi
class DownloadedBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var songs: MutableList<Child?>? = null
    private var groupTitle: String? = null
    private var groupSubtitle: String? = null

    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_downloaded_dialog, container, false)

        songs = this.requireArguments().getParcelableArrayList<Child?>(Constants.DOWNLOAD_GROUP)
        groupTitle = this.requireArguments().getString(Constants.DOWNLOAD_GROUP_TITLE)
        groupSubtitle = this.requireArguments().getString(Constants.DOWNLOAD_GROUP_SUBTITLE)

        initUI(view)
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

    private fun initUI(view: View) {
        val playRandom = view.findViewById<TextView>(R.id.play_random_text_view)
        playRandom.setVisibility(if (songs!!.size > 1) View.VISIBLE else View.GONE)

        val remove = view.findViewById<TextView>(R.id.remove_all_text_view)
        remove.setText(
            if (songs!!.size > 1) getText(R.string.downloaded_bottom_sheet_remove_all) else getText(
                R.string.downloaded_bottom_sheet_remove
            )
        )
    }

    private fun init(view: View) {
        val coverAlbum = view.findViewById<ImageView>(R.id.group_cover_image_view)
        from(
            requireContext(),
            songs!!.get(Random().nextInt(songs!!.size))!!.coverArtId,
            CustomGlideRequest.ResourceType.Unknown
        ).build().into(coverAlbum)

        val groupTitleView = view.findViewById<TextView>(R.id.group_title_text_view)
        groupTitleView.setText(this.groupTitle)
        groupTitleView.setSelected(true)

        val groupSubtitleView = view.findViewById<TextView>(R.id.group_subtitle_text_view)
        groupSubtitleView.setText(this.groupSubtitle)
        groupSubtitleView.setSelected(true)

        val playRandom = view.findViewById<TextView>(R.id.play_random_text_view)
        playRandom.setOnClickListener { v ->
            val nonNullSongs = songs ?: mutableListOf()
            Collections.shuffle(nonNullSongs)
            MediaManager.startQueue(mediaBrowserListenableFuture, nonNullSongs, 0)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        }

        val playNext = view.findViewById<TextView>(R.id.play_next_text_view)
        playNext.setOnClickListener { v ->
            val nonNullSongs = songs ?: mutableListOf()
            MediaManager.enqueue(mediaBrowserListenableFuture, nonNullSongs, true)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        }

        val addToQueue = view.findViewById<TextView>(R.id.add_to_queue_text_view)
        addToQueue.setOnClickListener { v ->
            val nonNullSongs = songs ?: mutableListOf()
            MediaManager.enqueue(mediaBrowserListenableFuture, nonNullSongs, false)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        }

        val removeAll = view.findViewById<TextView>(R.id.remove_all_text_view)
        removeAll.setOnClickListener { v ->
            val nonNullSongs = songs ?: mutableListOf()
            if (getDownloadDirectoryUri() == null) {
                val mediaItems: MutableList<MediaItem?> = MappingUtil.mapDownloads(nonNullSongs)
                val downloads: MutableList<Download?> = ArrayList(nonNullSongs.filterNotNull().map { Download(it) })
                DownloadUtil.getDownloadTracker(requireContext()).remove(mediaItems, downloads)
            } else {
                nonNullSongs.forEach(Consumer { obj: Child? -> delete(obj!!) })
            }
            dismissBottomSheet()
        }
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