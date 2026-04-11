package com.cappielloantonio.tempo.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.InnerFragmentPlayerLyricsBinding
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Line
import com.cappielloantonio.tempo.subsonic.models.LyricsList
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.isDisplayAlwaysOn
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlin.math.max

@OptIn(markerClass = [UnstableApi::class])
class PlayerLyricsFragment : Fragment() {
    private var bind: InnerFragmentPlayerLyricsBinding? = null
    private var playerBottomSheetViewModel: PlayerBottomSheetViewModel? = null
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null
    private var mediaBrowser: MediaBrowser? = null
    private var syncLyricsHandler: Handler? = null
    private var syncLyricsRunnable: Runnable? = null
    private var currentLyrics: String? = null
    private var currentLyricsList: LyricsList? = null
    private var lastLineIdx: Int? = null
    private var currentDescription: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = InnerFragmentPlayerLyricsBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()

        playerBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<PlayerBottomSheetViewModel>(
                PlayerBottomSheetViewModel::class.java
            )

        initOverlay()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPanelContent()
        observeDownloadState()
    }

    override fun onStart() {
        super.onStart()
        initializeBrowser()
    }

    override fun onResume() {
        super.onResume()
        bindMediaController()
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        releaseHandler()
        if (!isDisplayAlwaysOn()) {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
        currentLyrics = null
        currentLyricsList = null
        currentDescription = null
        lastLineIdx = null
    }

    private fun initOverlay() {
        bind!!.syncLyricsTapButton.setOnClickListener(View.OnClickListener { view: View? ->
            playerBottomSheetViewModel!!.changeSyncLyricsState()
        })

        bind!!.downloadLyricsButton.setOnClickListener(View.OnClickListener { view: View? ->
            val saved = playerBottomSheetViewModel!!.downloadCurrentLyrics()
            if (getContext() != null) {
                Toast.makeText(
                    requireContext(),
                    if (saved) R.string.player_lyrics_download_success else R.string.player_lyrics_download_failure,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun initializeBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(
                requireContext(),
                ComponentName(requireContext(), MediaService::class.java)
            )
        ).buildAsync()
    }

    private fun releaseHandler() {
        if (syncLyricsHandler != null) {
            syncLyricsHandler!!.removeCallbacks(syncLyricsRunnable!!)
            syncLyricsHandler = null
        }
    }

    private fun releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture!!)
    }

    private fun bindMediaController() {
        mediaBrowserListenableFuture!!.addListener(Runnable {
            try {
                mediaBrowser = mediaBrowserListenableFuture!!.get()
                defineProgressHandler()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun initPanelContent() {
        playerBottomSheetViewModel!!.liveLyrics.observe(
            getViewLifecycleOwner(),
            Observer { lyrics: String? ->
                currentLyrics = lyrics
                updatePanelContent()
            })

        playerBottomSheetViewModel!!.liveLyricsList.observe(
            getViewLifecycleOwner(),
            Observer { lyricsList: LyricsList? ->
                currentLyricsList = lyricsList
                lastLineIdx = null
                updatePanelContent()
            })

        playerBottomSheetViewModel!!.liveDescription.observe(
            getViewLifecycleOwner(),
            Observer { description: String? ->
                currentDescription = description
                updatePanelContent()
            })
    }

    private fun observeDownloadState() {
        playerBottomSheetViewModel!!.lyricsCachedState.observe(
            getViewLifecycleOwner(),
            Observer { cached: Boolean? ->
                if (bind != null) {
                    val downloadButton = bind!!.downloadLyricsButton
                    if (cached != null && cached) {
                        downloadButton.setIconResource(R.drawable.ic_done)
                        downloadButton.setContentDescription(getString(R.string.player_lyrics_downloaded_content_description))
                    } else {
                        downloadButton.setIconResource(R.drawable.ic_download)
                        downloadButton.setContentDescription(getString(R.string.player_lyrics_download_content_description))
                    }
                }
            })
    }

    private fun updatePanelContent() {
        if (bind == null) {
            return
        }

        bind!!.nowPlayingSongLyricsSrollView.smoothScrollTo(0, 0)

        if (hasStructuredLyrics(currentLyricsList)) {
            setSyncLyrics(currentLyricsList!!)
            bind!!.nowPlayingSongLyricsTextView.setVisibility(View.VISIBLE)
            bind!!.emptyDescriptionImageView.setVisibility(View.GONE)
            bind!!.titleEmptyDescriptionLabel.setVisibility(View.GONE)
            bind!!.syncLyricsTapButton.setVisibility(View.VISIBLE)
            bind!!.downloadLyricsButton.setVisibility(View.VISIBLE)
            bind!!.downloadLyricsButton.setEnabled(true)
        } else if (hasText(currentLyrics)) {
            bind!!.nowPlayingSongLyricsTextView.setText(MusicUtil.getReadableLyrics(currentLyrics))
            bind!!.nowPlayingSongLyricsTextView.setVisibility(View.VISIBLE)
            bind!!.emptyDescriptionImageView.setVisibility(View.GONE)
            bind!!.titleEmptyDescriptionLabel.setVisibility(View.GONE)
            bind!!.syncLyricsTapButton.setVisibility(View.GONE)
            bind!!.downloadLyricsButton.setVisibility(View.VISIBLE)
            bind!!.downloadLyricsButton.setEnabled(true)
        } else if (hasText(currentDescription)) {
            bind!!.nowPlayingSongLyricsTextView.setText(
                MusicUtil.getReadableLyrics(
                    currentDescription
                )
            )
            bind!!.nowPlayingSongLyricsTextView.setVisibility(View.VISIBLE)
            bind!!.emptyDescriptionImageView.setVisibility(View.GONE)
            bind!!.titleEmptyDescriptionLabel.setVisibility(View.GONE)
            bind!!.syncLyricsTapButton.setVisibility(View.GONE)
            bind!!.downloadLyricsButton.setVisibility(View.GONE)
            bind!!.downloadLyricsButton.setEnabled(false)
        } else {
            bind!!.nowPlayingSongLyricsTextView.setVisibility(View.GONE)
            bind!!.emptyDescriptionImageView.setVisibility(View.VISIBLE)
            bind!!.titleEmptyDescriptionLabel.setVisibility(View.VISIBLE)
            bind!!.syncLyricsTapButton.setVisibility(View.GONE)
            bind!!.downloadLyricsButton.setVisibility(View.GONE)
            bind!!.downloadLyricsButton.setEnabled(false)
        }
    }

    private fun hasText(value: String?): Boolean {
        return value != null && !value.trim { it <= ' ' }.isEmpty()
    }

    private fun hasStructuredLyrics(lyricsList: LyricsList?): Boolean {
        return lyricsList != null && lyricsList.structuredLyrics != null && !lyricsList.structuredLyrics!!.isEmpty() && lyricsList.structuredLyrics!!.get(
            0
        ).line != null && !lyricsList.structuredLyrics!!.get(
            0
        ).line!!.isEmpty()
    }

    @SuppressLint("DefaultLocale")
    private fun setSyncLyrics(lyricsList: LyricsList) {
        if (lyricsList.structuredLyrics != null && !lyricsList.structuredLyrics!!.isEmpty() && lyricsList.structuredLyrics!!.get(
                0
            ).line != null
        ) {
            val lyricsBuilder = StringBuilder()
            val lines: MutableList<Line>? = lyricsList.structuredLyrics!!.get(0).line?.toMutableList()

            if (lines != null) {
                for (line in lines) {
                    lyricsBuilder.append(line.value.trim { it <= ' ' }).append("\n\n")
                }
            }

            bind!!.nowPlayingSongLyricsTextView.setText(lyricsBuilder.toString())
        }
    }

    private fun defineProgressHandler() {
        playerBottomSheetViewModel!!.liveLyricsList.observe(
            getViewLifecycleOwner(),
            { lyricsList: LyricsList? ->
                if (!hasStructuredLyrics(lyricsList)) {
                    releaseHandler()
                    return@observe
                }
                if (!lyricsList!!.structuredLyrics!!.get(0).synced) {
                    releaseHandler()
                    return@observe
                }

                syncLyricsHandler = Handler(Looper.getMainLooper())
                syncLyricsRunnable = Runnable {
                    if (syncLyricsHandler != null) {
                        if (bind != null) {
                            displaySyncedLyrics()
                        }

                        syncLyricsHandler!!.postDelayed(syncLyricsRunnable!!, 250)
                    }
                }
                syncLyricsHandler!!.postDelayed(syncLyricsRunnable!!, 250)
            })
    }

    private fun displaySyncedLyrics() {
        val lyricsList = playerBottomSheetViewModel!!.liveLyricsList.getValue()
        val timestamp = (mediaBrowser!!.getCurrentPosition()).toInt()

        if (hasStructuredLyrics(lyricsList)) {
            val lines: MutableList<Line>? = lyricsList!!.structuredLyrics!!.get(0).line?.toMutableList()
            if (lines == null || lines.isEmpty()) {
                return
            }

            // Find the index of the currently playing line
            var curIdx = 0
            while (curIdx < lines.size) {
                val start = lines.get(curIdx).start
                if (start != null && start > timestamp) {
                    curIdx-- // Found the first line that starts after the current timestamp
                    break
                }
                ++curIdx
            }

            // Only update if the highlighted line has changed
            if (lastLineIdx != null && curIdx == lastLineIdx) {
                return
            }
            lastLineIdx = curIdx

            val lyricsBuilder = StringBuilder()
            for (line in lines) {
                lyricsBuilder.append(line.value.trim { it <= ' ' }).append("\n\n")
            }
            val lyrics = lyricsBuilder.toString()
            val spannableString: Spannable = SpannableString(lyrics)

            // Make each line clickable for navigation and highlight the current one
            var offset = 0
            var highlightStart = -1
            for (i in lines.indices) {
                val highlight = i == curIdx
                if (highlight) highlightStart = offset

                val len = lines.get(i).value.length + 2
                val lineStart: Int = lines.get(i).start!!
                spannableString.setSpan(object : ClickableSpan() {
                    override fun onClick(view: View) {
                        // Seeking to 1ms after the actual start prevents scrolling / highlighting artifacts
                        mediaBrowser!!.seekTo((lineStart + 1).toLong())
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.setUnderlineText(false)
                        if (highlight) {
                            ds.setColor(
                                requireContext().getResources()
                                    .getColor(R.color.lyricsTextColor, null)
                            )
                        } else {
                            ds.setColor(
                                requireContext().getResources()
                                    .getColor(R.color.shadowsLyricsTextColor, null)
                            )
                        }
                    }
                }, offset, offset + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                offset += len
            }

            bind!!.nowPlayingSongLyricsTextView.setMovementMethod(LinkMovementMethod.getInstance())
            bind!!.nowPlayingSongLyricsTextView.setText(spannableString)

            // Scroll to the highlighted line, but only if there is one
            if (highlightStart >= 0 && playerBottomSheetViewModel!!.syncLyricsState) {
                bind!!.nowPlayingSongLyricsSrollView.smoothScrollTo(0, getScroll(highlightStart))
            }
        }
    }

    private fun getScroll(startIndex: Int): Int {
        val layout = bind!!.nowPlayingSongLyricsTextView.getLayout()
        if (layout == null) return 0

        val line = layout.getLineForOffset(startIndex)
        val lineTop = layout.getLineTop(line)
        val lineBottom = layout.getLineBottom(line)
        val lineCenter = (lineTop + lineBottom) / 2

        val scrollViewHeight = bind!!.nowPlayingSongLyricsSrollView.getHeight()
        val scroll = lineCenter - scrollViewHeight / 2

        return max(scroll, 0)
    }

    companion object {
        private const val TAG = "PlayerLyricsFragment"
    }
}