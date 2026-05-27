package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.viewpager2.widget.ViewPager2
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.FragmentPlayerBottomSheetBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.service.MediaManager
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PlayQueue
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.fragment.pager.PlayerControllerVerticalPager
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.Preferences.getRepeatMode
import com.cappielloantonio.tempo.util.Preferences.getSyncCountdownTimer
import com.cappielloantonio.tempo.util.Preferences.isShuffleModeEnabled
import com.cappielloantonio.tempo.util.Preferences.isSyncronizationEnabled
import com.cappielloantonio.tempo.util.Preferences.setRepeatMode
import com.cappielloantonio.tempo.util.Preferences.setShuffleModeEnabled
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import com.google.android.material.elevation.SurfaceColors
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.function.IntPredicate
import java.util.stream.IntStream

@OptIn(markerClass = [UnstableApi::class])
class PlayerBottomSheetFragment : Fragment() {
    private var bind: FragmentPlayerBottomSheetBinding? = null

    private var playerBottomSheetViewModel: PlayerBottomSheetViewModel? = null
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private var progressBarHandler: Handler? = null
    private var progressBarRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentPlayerBottomSheetBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()

        playerBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<PlayerBottomSheetViewModel>(
                PlayerBottomSheetViewModel::class.java
            )

        customizeBottomSheetBackground()
        customizeBottomSheetAction()
        initViewPager()
        setHeaderBookmarksButton()

        return view
    }

    override fun onStart() {
        super.onStart()

        initializeMediaBrowser()
        bindMediaController()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun customizeBottomSheetBackground() {
        bind!!.playerHeaderLayout.getRoot()
            .setBackgroundColor(SurfaceColors.getColorForElevation(requireContext(), 8f))
    }

    private fun customizeBottomSheetAction() {
        bind!!.playerHeaderLayout.getRoot()
            .setOnClickListener(View.OnClickListener { view: View? -> (requireActivity() as MainActivity).expandBottomSheet() })
    }

    private fun initViewPager() {
        bind!!.playerBodyLayout.playerBodyBottomSheetViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL)
        bind!!.playerBodyLayout.playerBodyBottomSheetViewPager.setAdapter(
            PlayerControllerVerticalPager(this)
        )
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

    private fun releaseMediaBrowser() {
        MediaController.releaseFuture(mediaBrowserListenableFuture!!)
    }

    private fun bindMediaController() {
        mediaBrowserListenableFuture!!.addListener(Runnable {
            try {
                val mediaBrowser = mediaBrowserListenableFuture!!.get()

                mediaBrowser.setShuffleModeEnabled(isShuffleModeEnabled())
                mediaBrowser.setRepeatMode(getRepeatMode())

                setMediaControllerListener(mediaBrowser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setMediaControllerListener(mediaBrowser: MediaBrowser) {
        defineProgressBarHandler(mediaBrowser)
        setMediaControllerUI(mediaBrowser)
        setMetadata(mediaBrowser.getMediaMetadata())
        setContentDuration(mediaBrowser.getContentDuration())
        setPlayingState(mediaBrowser.isPlaying())
        setHeaderMediaController()
        setHeaderNextButtonState(mediaBrowser.hasNextMediaItem())

        mediaBrowser.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                setMediaControllerUI(mediaBrowser)
                setMetadata(mediaMetadata)
                setContentDuration(mediaBrowser.getContentDuration())
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                setPlayingState(isPlaying)
            }

            override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
                super.onSkipSilenceEnabledChanged(skipSilenceEnabled)
            }

            override fun onEvents(player: Player, events: Player.Events) {
                setHeaderNextButtonState(mediaBrowser.hasNextMediaItem())
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                setShuffleModeEnabled(shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                setRepeatMode(repeatMode)
            }
        })
    }

    private fun setMetadata(mediaMetadata: MediaMetadata) {
        if (mediaMetadata.extras != null) {
            playerBottomSheetViewModel!!.setLiveMedia(
                getViewLifecycleOwner(),
                mediaMetadata.extras!!.getString("type"),
                mediaMetadata.extras!!.getString("id")
            )
            playerBottomSheetViewModel!!.setLiveAlbum(
                getViewLifecycleOwner(),
                mediaMetadata.extras!!.getString("type"),
                mediaMetadata.extras!!.getString("albumId")
            )
            playerBottomSheetViewModel!!.setLiveArtist(
                getViewLifecycleOwner(),
                mediaMetadata.extras!!.getString("type"),
                mediaMetadata.extras!!.getString("artistId")
            )
            playerBottomSheetViewModel!!.setLiveDescription(
                mediaMetadata.extras!!.getString(
                    "description",
                    null
                )
            )

            bind!!.playerHeaderLayout.playerHeaderMediaTitleLabel.setText(
                mediaMetadata.extras!!.getString(
                    "title"
                )
            )
            bind!!.playerHeaderLayout.playerHeaderMediaArtistLabel.setText(
                if (mediaMetadata.artist != null)
                    mediaMetadata.artist
                else
                    ""
            )

            bind!!.playerHeaderLayout.playerHeaderMediaTitleLabel.setVisibility(
                if (mediaMetadata.extras!!.getString(
                        "title"
                    ) != null && mediaMetadata.extras!!.getString("title") != ""
                ) View.VISIBLE else View.GONE
            )
            bind!!.playerHeaderLayout.playerHeaderMediaArtistLabel.setVisibility(
                if (mediaMetadata.extras!!.getString("artist") != null && mediaMetadata.extras!!.getString(
                        "artist"
                    ) != ""
                )
                    View.VISIBLE
                else
                    View.GONE
            )

            from(
                requireContext(),
                mediaMetadata.extras!!.getString("coverArtId"),
                CustomGlideRequest.ResourceType.Song
            )
                .build()
                .into(bind!!.playerHeaderLayout.playerHeaderMediaCoverImage)
        }
    }


    private fun setMediaControllerUI(mediaBrowser: MediaBrowser) {
        if (mediaBrowser.getMediaMetadata().extras != null) {
            when (mediaBrowser.getMediaMetadata().extras!!.getString(
                "type",
                Constants.MEDIA_TYPE_MUSIC
            )) {
                Constants.MEDIA_TYPE_PODCAST -> {
                    bind!!.playerHeaderLayout.playerHeaderFastForwardMediaButton.setVisibility(View.VISIBLE)
                    bind!!.playerHeaderLayout.playerHeaderRewindMediaButton.setVisibility(View.VISIBLE)
                    bind!!.playerHeaderLayout.playerHeaderNextMediaButton.setVisibility(View.GONE)
                }

                Constants.MEDIA_TYPE_MUSIC -> {
                    bind!!.playerHeaderLayout.playerHeaderFastForwardMediaButton.setVisibility(View.GONE)
                    bind!!.playerHeaderLayout.playerHeaderRewindMediaButton.setVisibility(View.GONE)
                    bind!!.playerHeaderLayout.playerHeaderNextMediaButton.setVisibility(View.VISIBLE)
                }

                else -> {
                    bind!!.playerHeaderLayout.playerHeaderFastForwardMediaButton.setVisibility(View.GONE)
                    bind!!.playerHeaderLayout.playerHeaderRewindMediaButton.setVisibility(View.GONE)
                    bind!!.playerHeaderLayout.playerHeaderNextMediaButton.setVisibility(View.VISIBLE)
                }
            }
        }
    }

    private fun setContentDuration(duration: Long) {
        bind!!.playerHeaderLayout.playerHeaderSeekBar.setMax((duration / 1000).toInt())
    }

    private fun setProgress(mediaBrowser: MediaBrowser) {
        if (bind != null) bind!!.playerHeaderLayout.playerHeaderSeekBar.setProgress(
            (mediaBrowser.getCurrentPosition() / 1000).toInt(),
            true
        )
    }

    private fun setPlayingState(isPlaying: Boolean) {
        bind!!.playerHeaderLayout.playerHeaderButton.setChecked(isPlaying)
        runProgressBarHandler(isPlaying)
    }

    private fun setHeaderMediaController() {
        bind!!.playerHeaderLayout.playerHeaderButton.setOnClickListener(View.OnClickListener { view: View? ->
            bind!!.getRoot().findViewById<View>(R.id.exo_play_pause).performClick()
        })
        bind!!.playerHeaderLayout.playerHeaderNextMediaButton.setOnClickListener(View.OnClickListener { view: View? ->
            bind!!.getRoot().findViewById<View>(R.id.exo_next).performClick()
        })
        bind!!.playerHeaderLayout.playerHeaderRewindMediaButton.setOnClickListener(View.OnClickListener { view: View? ->
            bind!!.getRoot().findViewById<View>(R.id.exo_rew).performClick()
        })
        bind!!.playerHeaderLayout.playerHeaderFastForwardMediaButton.setOnClickListener(View.OnClickListener { view: View? ->
            bind!!.getRoot().findViewById<View>(R.id.exo_ffwd).performClick()
        })
    }

    private fun setHeaderNextButtonState(isEnabled: Boolean) {
        bind!!.playerHeaderLayout.playerHeaderNextMediaButton.setEnabled(isEnabled)
        bind!!.playerHeaderLayout.playerHeaderNextMediaButton.setAlpha(if (isEnabled) 1.0.toFloat() else 0.3.toFloat())
    }

    val playerHeader: View
        get() = requireView().findViewById<View>(R.id.player_header_layout)

    fun goBackToFirstPage() {
        bind!!.playerBodyLayout.playerBodyBottomSheetViewPager.setCurrentItem(0, false)
        goToControllerPage()
    }

    fun goToControllerPage() {
        val playerControllerVerticalPager =
            bind!!.playerBodyLayout.playerBodyBottomSheetViewPager.getAdapter() as PlayerControllerVerticalPager?
        if (playerControllerVerticalPager != null) {
            val playerControllerFragment =
                playerControllerVerticalPager.getRegisteredFragment(0) as PlayerControllerFragment?
            if (playerControllerFragment != null) {
                playerControllerFragment.goToControllerPage()
            }
        }
    }

    fun goToLyricsPage() {
        val playerControllerVerticalPager =
            bind!!.playerBodyLayout.playerBodyBottomSheetViewPager.getAdapter() as PlayerControllerVerticalPager?
        if (playerControllerVerticalPager != null) {
            val playerControllerFragment =
                playerControllerVerticalPager.getRegisteredFragment(0) as PlayerControllerFragment?
            if (playerControllerFragment != null) {
                playerControllerFragment.goToLyricsPage()
            }
        }
    }

    fun goToQueuePage() {
        bind!!.playerBodyLayout.playerBodyBottomSheetViewPager.setCurrentItem(1, true)
    }

    fun setPlayerControllerVerticalPagerDraggableState(isDraggable: Boolean) {
        val playerControllerVerticalPager = bind!!.playerBodyLayout.playerBodyBottomSheetViewPager
        playerControllerVerticalPager.setUserInputEnabled(isDraggable)
    }

    private fun defineProgressBarHandler(mediaBrowser: MediaBrowser) {
        progressBarHandler = Handler(Looper.getMainLooper())
        progressBarRunnable = Runnable {
            setProgress(mediaBrowser)
            progressBarHandler!!.postDelayed(progressBarRunnable!!, 1000)
        }
    }

    private fun runProgressBarHandler(isPlaying: Boolean) {
        if (isPlaying) {
            progressBarHandler!!.postDelayed(progressBarRunnable!!, 1000)
        } else {
            progressBarHandler!!.removeCallbacks(progressBarRunnable!!)
        }
    }

    private fun setHeaderBookmarksButton() {
        if (isSyncronizationEnabled()) {
            playerBottomSheetViewModel!!.playQueue.observeForever(object : Observer<PlayQueue?> {
                override fun onChanged(value: PlayQueue?) {
                    val playQueue = value
                    playerBottomSheetViewModel!!.playQueue.removeObserver(this)

                    if (bind == null) return

                    if (playQueue != null && !playQueue.entries!!.isEmpty()) {
                        val index = IntStream.range(0, playQueue.entries!!.size)
                            .filter(IntPredicate { ix: Int -> playQueue.entries!!.get(ix).id == playQueue.current })
                            .findFirst().orElse(-1)

                        if (index != -1) {
                            bind!!.playerHeaderLayout.playerHeaderBookmarkMediaButton.setVisibility(
                                View.VISIBLE
                            )
                            bind!!.playerHeaderLayout.playerHeaderBookmarkMediaButton.setOnClickListener(
                                View.OnClickListener { v: View? ->
                                    @Suppress("UNCHECKED_CAST")
                                    MediaManager.startQueue(
                                        mediaBrowserListenableFuture,
                                        playQueue.entries?.toMutableList() as MutableList<Child?>,
                                        index
                                    )
                                    bind!!.playerHeaderLayout.playerHeaderBookmarkMediaButton.setVisibility(
                                        View.GONE
                                    )
                                })
                        }
                    } else {
                        bind!!.playerHeaderLayout.playerHeaderBookmarkMediaButton.setVisibility(View.GONE)
                        bind!!.playerHeaderLayout.playerHeaderBookmarkMediaButton.setOnClickListener(
                            null
                        )
                    }
                }
            })

            bind!!.playerHeaderLayout.playerHeaderBookmarkMediaButton.setOnLongClickListener(
                OnLongClickListener { v: View? ->
                    bind!!.playerHeaderLayout.playerHeaderBookmarkMediaButton.setVisibility(View.GONE)
                    true
                })

            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                if (bind != null) bind!!.playerHeaderLayout.playerHeaderBookmarkMediaButton.setVisibility(
                    View.GONE
                )
            }, getSyncCountdownTimer() * 1000L)
        }
    }
}
