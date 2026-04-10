package com.cappielloantonio.tempo.ui.fragment.bottomsheetdialog

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PlaylistWithSongs
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

@UnstableApi
class PlaylistBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private var playlist: PlaylistWithSongs? = null
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_playlist_dialog, container, false)

        playlist = requireArguments().getParcelable<PlaylistWithSongs?>(Constants.PLAYLIST_OBJECT)

        init(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        val coverPlaylist = view.findViewById<ImageView>(R.id.playlist_cover_image_view)

        from(view.getContext(), playlist!!.coverArtId, CustomGlideRequest.ResourceType.Playlist)
            .build()
            .into(coverPlaylist)

        val titlePlaylist = view.findViewById<TextView>(R.id.playlist_title_text_view)
        titlePlaylist.setText(playlist!!.name)

        titlePlaylist.setSelected(true)

        val countPlaylist = view.findViewById<TextView>(R.id.playlist_count_text_view)
        countPlaylist.setText(
            view.getContext().getString(
                R.string.playlist_counted_tracks,
                playlist!!.songCount,
                MusicUtil.getReadableDurationString(playlist!!.duration, false)
            )
        )

        val playNext = view.findViewById<TextView>(R.id.play_next_text_view)
        playNext.setOnClickListener { v ->
            val entries: MutableList<Child?> = playlist!!.entries?.map { it }?.toMutableList() ?: mutableListOf()
            MediaManager.enqueue(mediaBrowserListenableFuture, entries, true)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
            dismissBottomSheet()
        }

        val addToQueue = view.findViewById<TextView>(R.id.add_to_queue_text_view)
        addToQueue.setOnClickListener { v ->
            val entries: MutableList<Child?> = playlist!!.entries?.map { it }?.toMutableList() ?: mutableListOf()
            MediaManager.enqueue(mediaBrowserListenableFuture, entries, false)
            (requireActivity() as MainActivity).setBottomSheetInPeek(true)
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

    companion object {
        private const val TAG = "PlaylistBottomSheetDialog"
    }
}
