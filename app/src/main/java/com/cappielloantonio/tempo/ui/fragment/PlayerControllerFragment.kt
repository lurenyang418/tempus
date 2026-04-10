package com.cappielloantonio.tempo.ui.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.RepeatModeUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.transition.ChangeBounds
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.InnerFragmentPlayerControllerBinding
import com.cappielloantonio.tempo.service.BaseMediaService
import com.cappielloantonio.tempo.service.BaseMediaService.LocalBinder
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.ui.dialog.PlaybackSpeedDialog
import com.cappielloantonio.tempo.ui.dialog.PlaybackSpeedDialog.PlaybackSpeedListener
import com.cappielloantonio.tempo.ui.dialog.RatingDialog
import com.cappielloantonio.tempo.ui.dialog.TrackInfoDialog
import com.cappielloantonio.tempo.ui.fragment.pager.PlayerControllerHorizontalPager
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.AssetLinkUtil.AssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.applyLinkAppearance
import com.cappielloantonio.tempo.util.AssetLinkUtil.buildAssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.clearLinkAppearance
import com.cappielloantonio.tempo.util.AssetLinkUtil.copyToClipboard
import com.cappielloantonio.tempo.util.AssetLinkUtil.getLabelRes
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.getBitrateVisible
import com.cappielloantonio.tempo.util.Preferences.getPlaybackSpeed
import com.cappielloantonio.tempo.util.Preferences.getRepeatMode
import com.cappielloantonio.tempo.util.Preferences.isShuffleModeEnabled
import com.cappielloantonio.tempo.util.Preferences.isSkipSilenceMode
import com.cappielloantonio.tempo.util.Preferences.setBitrateVisible
import com.cappielloantonio.tempo.util.Preferences.setRepeatMode
import com.cappielloantonio.tempo.util.Preferences.setShuffleModeEnabled
import com.cappielloantonio.tempo.util.Preferences.setSkipSilenceMode
import com.cappielloantonio.tempo.util.Preferences.showItemStarRating
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel
import com.cappielloantonio.tempo.viewmodel.RatingViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.elevation.SurfaceColors
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.text.DecimalFormat

@UnstableApi
class PlayerControllerFragment : Fragment() {
    private var bind: InnerFragmentPlayerControllerBinding? = null
    private var playerMediaCoverViewPager: ViewPager2? = null
    private var buttonFavorite: ToggleButton? = null
    private var ratingViewModel: RatingViewModel? = null
    private var songRatingBar: RatingBar? = null
    private var playerMediaTitleLabel: TextView? = null
    private var playerArtistNameLabel: TextView? = null
    private var playbackSpeedButton: Button? = null
    private var skipSilenceToggleButton: ToggleButton? = null
    private var playerMediaExtension: Chip? = null
    private var playerMediaBitrate: TextView? = null
    private var playerQuickActionView: ConstraintLayout? = null
    private var playerOpenQueueButton: ImageButton? = null
    private var playerTrackInfo: ImageButton? = null
    private var ratingContainer: LinearLayout? = null
    private var equalizerButton: ImageButton? = null
    private var assetLinkChipGroup: ChipGroup? = null
    private var playerSongLinkChip: Chip? = null
    private var playerAlbumLinkChip: Chip? = null
    private var playerArtistLinkChip: Chip? = null

    private var activity: MainActivity? = null
    private var playerBottomSheetViewModel: PlayerBottomSheetViewModel? = null
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    private var mediaServiceBinder: LocalBinder? = null
    private var isServiceBound = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = getActivity() as MainActivity?

        bind = InnerFragmentPlayerControllerBinding.inflate(inflater, container, false)
        val view: View = bind!!.getRoot()

        playerBottomSheetViewModel =
            ViewModelProvider(requireActivity()).get<PlayerBottomSheetViewModel>(
                PlayerBottomSheetViewModel::class.java
            )
        ratingViewModel =
            ViewModelProvider(requireActivity()).get<RatingViewModel>(RatingViewModel::class.java)

        init()
        initQuickActionView()
        initCoverLyricsSlideView()
        initMediaListenable()
        initMediaLabelButton()
        initArtistLabelButton()
        initEqualizerButton()

        return view
    }

    override fun onStart() {
        super.onStart()
        initializeBrowser()
        bindMediaController()
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        playerMediaCoverViewPager =
            bind!!.getRoot().findViewById<ViewPager2>(R.id.player_media_cover_view_pager)
        buttonFavorite = bind!!.getRoot().findViewById<ToggleButton>(R.id.button_favorite)
        playerMediaTitleLabel =
            bind!!.getRoot().findViewById<TextView>(R.id.player_media_title_label)
        playerArtistNameLabel =
            bind!!.getRoot().findViewById<TextView>(R.id.player_artist_name_label)
        playbackSpeedButton =
            bind!!.getRoot().findViewById<Button>(R.id.player_playback_speed_button)
        skipSilenceToggleButton =
            bind!!.getRoot().findViewById<ToggleButton>(R.id.player_skip_silence_toggle_button)
        playerMediaExtension = bind!!.getRoot().findViewById<Chip>(R.id.player_media_extension)
        playerMediaBitrate = bind!!.getRoot().findViewById<TextView>(R.id.player_media_bitrate)
        playerQuickActionView =
            bind!!.getRoot().findViewById<ConstraintLayout>(R.id.player_quick_action_view)
        playerOpenQueueButton =
            bind!!.getRoot().findViewById<ImageButton>(R.id.player_open_queue_button)
        playerTrackInfo = bind!!.getRoot().findViewById<ImageButton>(R.id.player_info_track)
        songRatingBar = bind!!.getRoot().findViewById<RatingBar>(R.id.song_rating_bar)
        ratingContainer = bind!!.getRoot().findViewById<LinearLayout?>(R.id.rating_container)
        equalizerButton =
            bind!!.getRoot().findViewById<ImageButton?>(R.id.player_open_equalizer_button)
        assetLinkChipGroup = bind!!.getRoot().findViewById<ChipGroup?>(R.id.asset_link_chip_group)
        playerSongLinkChip = bind!!.getRoot().findViewById<Chip?>(R.id.asset_link_song_chip)
        playerAlbumLinkChip = bind!!.getRoot().findViewById<Chip?>(R.id.asset_link_album_chip)
        playerArtistLinkChip = bind!!.getRoot().findViewById<Chip?>(R.id.asset_link_artist_chip)
        checkAndSetRatingContainerVisibility()
    }

    private fun initQuickActionView() {
        playerQuickActionView!!.setBackgroundColor(
            SurfaceColors.getColorForElevation(
                requireContext(),
                8f
            )
        )

        playerOpenQueueButton!!.setOnClickListener(View.OnClickListener { view: View? ->
            val playerBottomSheetFragment = requireActivity().getSupportFragmentManager()
                .findFragmentByTag("PlayerBottomSheet") as PlayerBottomSheetFragment?
            if (playerBottomSheetFragment != null) {
                playerBottomSheetFragment.goToQueuePage()
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

    private fun releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture!!)
    }

    private fun bindMediaController() {
        mediaBrowserListenableFuture!!.addListener(Runnable {
            try {
                val mediaBrowser = mediaBrowserListenableFuture!!.get()

                bind!!.nowPlayingMediaControllerView.setPlayer(mediaBrowser)
                mediaBrowser.setShuffleModeEnabled(isShuffleModeEnabled())
                mediaBrowser.setRepeatMode(getRepeatMode())
                setMediaControllerListener(mediaBrowser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setMediaControllerListener(mediaBrowser: MediaBrowser) {
        setMediaControllerUI(mediaBrowser)
        setMetadata(mediaBrowser.getMediaMetadata())
        setMediaInfo(mediaBrowser.getMediaMetadata())

        mediaBrowser.addListener(object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                setMediaControllerUI(mediaBrowser)
                setMetadata(mediaMetadata)
                setMediaInfo(mediaMetadata)
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
        val type =
            if (mediaMetadata.extras != null) mediaMetadata.extras!!.getString("type") else null

        if (type == Constants.MEDIA_TYPE_RADIO) {
            // For radio: always read from extras first (radioArtist, radioTitle, stationName)
            // MediaMetadata.title/artist are formatted for notification
            val stationName = if (mediaMetadata.extras != null)
                mediaMetadata.extras!!.getString(
                    "stationName",
                    if (mediaMetadata.artist != null) mediaMetadata.artist.toString() else ""
                )
            else
                if (mediaMetadata.artist != null) mediaMetadata.artist.toString() else ""

            val artist = if (mediaMetadata.extras != null)
                mediaMetadata.extras!!.getString("radioArtist", "")
            else
                ""

            val title = if (mediaMetadata.extras != null)
                mediaMetadata.extras!!.getString("radioTitle", "")
            else
                ""

            // Format: "Artist - Song" or fallback to title or station name
            val mainTitle: String?
            if (!TextUtils.isEmpty(artist) && !TextUtils.isEmpty(title)) {
                mainTitle = artist + " - " + title
            } else if (!TextUtils.isEmpty(title)) {
                mainTitle = title
            } else if (!TextUtils.isEmpty(artist)) {
                mainTitle = artist
            } else {
                mainTitle = stationName
            }

            playerMediaTitleLabel!!.setText(mainTitle)
            playerArtistNameLabel!!.setText(stationName)

            playerMediaTitleLabel!!.setSelected(true)
            playerArtistNameLabel!!.setSelected(true)

            playerMediaTitleLabel!!.setVisibility(if (!TextUtils.isEmpty(mainTitle)) View.VISIBLE else View.GONE)
            playerArtistNameLabel!!.setVisibility(if (!TextUtils.isEmpty(stationName)) View.VISIBLE else View.GONE)

            updateAssetLinkChips(mediaMetadata)
            return
        }

        playerMediaTitleLabel!!.setText(mediaMetadata.title.toString())
        playerArtistNameLabel!!.setText(
            if (mediaMetadata.artist != null) mediaMetadata.artist.toString() else
                ""
        )

        playerMediaTitleLabel!!.setSelected(true)
        playerArtistNameLabel!!.setSelected(true)

        playerMediaTitleLabel!!.setVisibility(if (mediaMetadata.title != null && mediaMetadata.title != "") View.VISIBLE else View.GONE)
        playerArtistNameLabel!!.setVisibility(
            if ((mediaMetadata.artist != null && mediaMetadata.artist != "")
                || mediaMetadata.extras != null && mediaMetadata.extras!!.getString("type") == Constants.MEDIA_TYPE_RADIO && mediaMetadata.extras!!.getString(
                    "uri"
                ) != null
            )
                View.VISIBLE
            else
                View.GONE
        )

        updateAssetLinkChips(mediaMetadata)
    }

    private fun setMediaInfo(mediaMetadata: MediaMetadata) {
        var isLocal = false

        if (mediaBrowserListenableFuture != null && mediaBrowserListenableFuture!!.isDone()) {
            try {
                val browser = mediaBrowserListenableFuture!!.get()
                if (browser != null && browser.getCurrentMediaItem() != null) {
                    val currentUri = browser.getCurrentMediaItem()!!.requestMetadata.mediaUri
                    if (currentUri != null) {
                        val scheme = currentUri.getScheme()
                        isLocal = "content" == scheme || "file" == scheme
                    }
                }
            } catch (e: Exception) {
                Log.e("DEBUG_PLAYER", "Error getting browser for UI update", e)
            }
        }

        if (mediaMetadata.extras != null) {
            val extension = mediaMetadata.extras!!.getString(
                "suffix",
                getString(R.string.player_unknown_format)
            )
            val rawBitrate = mediaMetadata.extras!!.getInt("bitrate", 0)
            val bitrate = if (rawBitrate != 0) rawBitrate.toString() + "kbps" else "Original"
            val samplingRate = if (mediaMetadata.extras!!.getInt(
                    "samplingRate",
                    0
                ) != 0
            ) DecimalFormat("0.#").format(
                mediaMetadata.extras!!.getInt(
                    "samplingRate",
                    0
                ) / 1000.0
            ) + "kHz" else ""
            val bitDepth = if (mediaMetadata.extras!!.getInt(
                    "bitDepth",
                    0
                ) != 0
            ) mediaMetadata.extras!!.getInt("bitDepth", 0).toString() + "b" else ""

            playerMediaExtension!!.setText(extension)

            if (bitrate == "Original" && !isLocal) {
                playerMediaBitrate!!.setVisibility(View.GONE)
            } else {
                val items: MutableList<String?> = ArrayList<String?>()
                if (!bitrate.trim { it <= ' ' }.isEmpty()) items.add(bitrate)
                if (!bitDepth.trim { it <= ' ' }.isEmpty()) items.add(bitDepth)
                if (!samplingRate.trim { it <= ' ' }.isEmpty()) items.add(samplingRate)
                val mediaQuality = TextUtils.join(" • ", items)

                playerMediaBitrate!!.setVisibility(if (getBitrateVisible()) View.VISIBLE else View.GONE)
                playerMediaBitrate!!.setText(if (isLocal) mediaQuality else mediaQuality)
            }
        }


        if (!isLocal) {
            val isTranscodingExtension = MusicUtil.getTranscodingFormatPreference() != "raw"
            val isTranscodingBitrate = MusicUtil.getBitratePreference() != "0"
            if (isTranscodingExtension || isTranscodingBitrate) {
                playerMediaExtension!!.setText(
                    MusicUtil.getTranscodingFormatPreference() + " (" + getString(
                        R.string.player_transcoding
                    ) + ")"
                )
                playerMediaBitrate!!.setText(
                    if (MusicUtil.getBitratePreference() != "0") MusicUtil.getBitratePreference() + "kbps" else getString(
                        R.string.player_transcoding_requested
                    )
                )
            }
        }

        playerTrackInfo!!.setOnClickListener(View.OnClickListener { view: View? ->
            val dialog = TrackInfoDialog(mediaMetadata)
            dialog.show(activity!!.getSupportFragmentManager(), null)
        })

        playerMediaExtension!!.setOnClickListener(View.OnClickListener { v: View? -> toggleBitrateVisibility() })
        playerMediaBitrate!!.setOnClickListener(View.OnClickListener { v: View? -> toggleBitrateVisibility() })
    }

    private fun toggleBitrateVisibility() {
        val parent = playerMediaBitrate!!.getParent() as ViewGroup

        val transition = TransitionSet()
            .addTransition(Slide(Gravity.START))
            .addTransition(ChangeBounds())
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
        TransitionManager.beginDelayedTransition(parent, transition)

        playerMediaBitrate!!.setVisibility(if (getBitrateVisible()) View.GONE else View.VISIBLE)
        setBitrateVisible(!getBitrateVisible())
    }

    private fun updateAssetLinkChips(mediaMetadata: MediaMetadata) {
        if (assetLinkChipGroup == null) return
        val mediaType = if (mediaMetadata.extras != null) mediaMetadata.extras!!.getString(
            "type",
            Constants.MEDIA_TYPE_MUSIC
        ) else Constants.MEDIA_TYPE_MUSIC
        if (Constants.MEDIA_TYPE_MUSIC != mediaType) {
            clearAssetLinkChip(playerSongLinkChip)
            clearAssetLinkChip(playerAlbumLinkChip)
            clearAssetLinkChip(playerArtistLinkChip)
            syncAssetLinkGroupVisibility()
            return
        }

        val songId =
            if (mediaMetadata.extras != null) mediaMetadata.extras!!.getString("id") else null
        val albumId =
            if (mediaMetadata.extras != null) mediaMetadata.extras!!.getString("albumId") else null
        val artistId =
            if (mediaMetadata.extras != null) mediaMetadata.extras!!.getString("artistId") else null

        val songLink = bindAssetLinkChip(playerSongLinkChip, AssetLinkUtil.TYPE_SONG, songId)
        val albumLink = bindAssetLinkChip(playerAlbumLinkChip, AssetLinkUtil.TYPE_ALBUM, albumId)
        val artistLink =
            bindAssetLinkChip(playerArtistLinkChip, AssetLinkUtil.TYPE_ARTIST, artistId)
        bindAssetLinkView(playerMediaTitleLabel, songLink)
        bindAssetLinkView(playerArtistNameLabel, if (artistLink != null) artistLink else songLink)
        bindAssetLinkView(playerMediaCoverViewPager, songLink)
        syncAssetLinkGroupVisibility()
    }

    private fun bindAssetLinkChip(chip: Chip?, type: String, id: String?): AssetLink? {
        if (chip == null) return null
        if (TextUtils.isEmpty(id)) {
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
            if (assetLink != null) {
                activity!!.openAssetLink(assetLink)
            }
        })

        chip.setOnLongClickListener(OnLongClickListener { v: View? ->
            if (assetLink != null) {
                copyToClipboard(requireContext(), assetLink)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.asset_link_copied_toast, id),
                    Toast.LENGTH_SHORT
                ).show()
            }
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
            val collapse = AssetLinkUtil.TYPE_SONG != assetLink.type
            activity!!.openAssetLink(assetLink, collapse)
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

    private fun setMediaControllerUI(mediaBrowser: MediaBrowser) {
        initPlaybackSpeedButton(mediaBrowser)

        if (mediaBrowser.getMediaMetadata().extras != null) {
            when (mediaBrowser.getMediaMetadata().extras!!.getString(
                "type",
                Constants.MEDIA_TYPE_MUSIC
            )) {
                Constants.MEDIA_TYPE_PODCAST -> {
                    bind!!.getRoot().setShowShuffleButton(false)
                    bind!!.getRoot().setShowRewindButton(true)
                    bind!!.getRoot().setShowPreviousButton(false)
                    bind!!.getRoot().setShowNextButton(false)
                    bind!!.getRoot().setShowFastForwardButton(true)
                    bind!!.getRoot().setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE)
                    bind!!.getRoot().findViewById<View>(R.id.player_playback_speed_button)
                        .setVisibility(
                            View.VISIBLE
                        )
                    bind!!.getRoot().findViewById<View>(R.id.player_skip_silence_toggle_button)
                        .setVisibility(
                            View.VISIBLE
                        )
                    bind!!.getRoot().findViewById<View>(R.id.button_favorite)
                        .setVisibility(View.GONE)
                    setPlaybackParameters(mediaBrowser)
                }

                Constants.MEDIA_TYPE_RADIO -> {
                    bind!!.getRoot().setShowShuffleButton(false)
                    bind!!.getRoot().setShowRewindButton(false)
                    bind!!.getRoot().setShowPreviousButton(false)
                    bind!!.getRoot().setShowNextButton(false)
                    bind!!.getRoot().setShowFastForwardButton(false)
                    bind!!.getRoot().setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE)
                    bind!!.getRoot().findViewById<View>(R.id.player_playback_speed_button)
                        .setVisibility(
                            View.GONE
                        )
                    bind!!.getRoot().findViewById<View>(R.id.player_skip_silence_toggle_button)
                        .setVisibility(
                            View.GONE
                        )
                    bind!!.getRoot().findViewById<View>(R.id.button_favorite)
                        .setVisibility(View.GONE)
                    setPlaybackParameters(mediaBrowser)
                }

                Constants.MEDIA_TYPE_MUSIC -> {
                    bind!!.getRoot().setShowShuffleButton(true)
                    bind!!.getRoot().setShowRewindButton(false)
                    bind!!.getRoot().setShowPreviousButton(true)
                    bind!!.getRoot().setShowNextButton(true)
                    bind!!.getRoot().setShowFastForwardButton(false)
                    bind!!.getRoot()
                        .setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL or RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE)
                    bind!!.getRoot().findViewById<View>(R.id.player_playback_speed_button)
                        .setVisibility(
                            View.VISIBLE
                        )
                    bind!!.getRoot().findViewById<View>(R.id.player_skip_silence_toggle_button)
                        .setVisibility(
                            View.GONE
                        )
                    bind!!.getRoot().findViewById<View>(R.id.button_favorite)
                        .setVisibility(View.VISIBLE)
                    setPlaybackParameters(mediaBrowser)
                }

                else -> {
                    bind!!.getRoot().setShowShuffleButton(true)
                    bind!!.getRoot().setShowRewindButton(false)
                    bind!!.getRoot().setShowPreviousButton(true)
                    bind!!.getRoot().setShowNextButton(true)
                    bind!!.getRoot().setShowFastForwardButton(false)
                    bind!!.getRoot()
                        .setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL or RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE)
                    bind!!.getRoot().findViewById<View>(R.id.player_playback_speed_button)
                        .setVisibility(
                            View.VISIBLE
                        )
                    bind!!.getRoot().findViewById<View>(R.id.player_skip_silence_toggle_button)
                        .setVisibility(
                            View.GONE
                        )
                    bind!!.getRoot().findViewById<View>(R.id.button_favorite)
                        .setVisibility(View.VISIBLE)
                    setPlaybackParameters(mediaBrowser)
                }
            }
        }
    }

    private fun initCoverLyricsSlideView() {
        playerMediaCoverViewPager!!.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL)
        playerMediaCoverViewPager!!.setAdapter(PlayerControllerHorizontalPager(this))

        playerMediaCoverViewPager!!.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val playerBottomSheetFragment = requireActivity().getSupportFragmentManager()
                    .findFragmentByTag("PlayerBottomSheet") as PlayerBottomSheetFragment?

                if (position == 0) {
                    activity!!.setBottomSheetDraggableState(true)

                    if (playerBottomSheetFragment != null) {
                        playerBottomSheetFragment.setPlayerControllerVerticalPagerDraggableState(
                            true
                        )
                    }
                } else if (position == 1) {
                    activity!!.setBottomSheetDraggableState(false)

                    if (playerBottomSheetFragment != null) {
                        playerBottomSheetFragment.setPlayerControllerVerticalPagerDraggableState(
                            false
                        )
                    }
                }
            }
        })
    }

    private fun initMediaListenable() {
        playerBottomSheetViewModel!!.getLiveMedia()
            .observe(getViewLifecycleOwner(), Observer { media: Child? ->
                if (media != null) {
                    ratingViewModel!!.setSong(media)
                    buttonFavorite!!.setChecked(media.starred != null)
                    buttonFavorite!!.setOnClickListener(View.OnClickListener { v: View? ->
                        playerBottomSheetViewModel!!.setFavorite(
                            requireContext(),
                            media
                        )
                    })
                    buttonFavorite!!.setOnLongClickListener(OnLongClickListener { v: View? ->
                        val bundle = Bundle()
                        bundle.putParcelable(Constants.TRACK_OBJECT, media)

                        val dialog = RatingDialog()
                        dialog.setArguments(bundle)
                        dialog.show(requireActivity().getSupportFragmentManager(), null)
                        true
                    })

                    val currentRating = media.userRating

                    if (currentRating != null) {
                        songRatingBar!!.setRating(currentRating.toFloat())
                    } else {
                        songRatingBar!!.setRating(0f)
                    }

                    songRatingBar!!.setOnRatingBarChangeListener(object :
                        OnRatingBarChangeListener {
                        override fun onRatingChanged(
                            ratingBar: RatingBar?,
                            rating: Float,
                            fromUser: Boolean
                        ) {
                            if (fromUser) {
                                ratingViewModel!!.rate(rating.toInt())
                                media.userRating = rating.toInt()
                            }
                        }
                    })


                    if (getActivity() != null) {
                        playerBottomSheetViewModel!!.refreshMediaInfo(requireActivity(), media)
                    }
                }
            })
    }

    private fun initMediaLabelButton() {
        playerBottomSheetViewModel!!.getLiveAlbum()
            .observe(getViewLifecycleOwner(), Observer { album: AlbumID3? ->
                if (album != null) {
                    playerMediaTitleLabel!!.setOnClickListener(View.OnClickListener { view: View? ->
                        val bundle = Bundle()
                        bundle.putParcelable(Constants.ALBUM_OBJECT, album)
                        NavHostFragment.findNavController(this)
                            .navigate(R.id.albumPageFragment, bundle)
                        activity!!.collapseBottomSheetDelayed()
                    })
                }
            })
    }

    private fun initArtistLabelButton() {
        playerBottomSheetViewModel!!.getLiveArtist()
            .observe(getViewLifecycleOwner(), Observer { artist: ArtistID3? ->
                if (artist != null) {
                    playerArtistNameLabel!!.setOnClickListener(View.OnClickListener { view: View? ->
                        val bundle = Bundle()
                        bundle.putParcelable(Constants.ARTIST_OBJECT, artist)
                        NavHostFragment.findNavController(this)
                            .navigate(R.id.artistPageFragment, bundle)
                        activity!!.collapseBottomSheetDelayed()
                    })
                }
            })
    }

    private fun initPlaybackSpeedButton(mediaBrowser: MediaBrowser) {
        playbackSpeedButton!!.setOnClickListener(View.OnClickListener { view: View? ->
            val dialog = PlaybackSpeedDialog()
            dialog.setPlaybackSpeedListener(object : PlaybackSpeedDialog.PlaybackSpeedListener {
                override fun onSpeedSelected(speed: Float) {
                    mediaBrowser.setPlaybackParameters(PlaybackParameters(speed))
                    playbackSpeedButton!!.setText(getString(R.string.player_playback_speed, speed))
                }
            })
            dialog.show(requireActivity().getSupportFragmentManager(), null)
        })

        skipSilenceToggleButton!!.setOnClickListener(View.OnClickListener { view: View? ->
            setSkipSilenceMode(!skipSilenceToggleButton!!.isChecked())
        })
    }

    private fun initEqualizerButton() {
        equalizerButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            val navController = NavHostFragment.findNavController(this)
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.equalizerFragment, true)
                .build()
            navController.navigate(R.id.equalizerFragment, null, navOptions)
            if (activity != null) activity!!.collapseBottomSheetDelayed()
        })
    }

    fun goToControllerPage() {
        playerMediaCoverViewPager!!.setCurrentItem(0, false)
    }

    fun goToLyricsPage() {
        playerMediaCoverViewPager!!.setCurrentItem(1, true)
    }

    private fun checkAndSetRatingContainerVisibility() {
        if (ratingContainer == null) return

        if (showItemStarRating()) {
            ratingContainer!!.setVisibility(View.VISIBLE)
        } else {
            ratingContainer!!.setVisibility(View.GONE)
        }
    }

    private fun setPlaybackParameters(mediaBrowser: MediaBrowser) {
        val playbackSpeedButton =
            bind!!.getRoot().findViewById<Button>(R.id.player_playback_speed_button)
        val currentSpeed = getPlaybackSpeed()
        val skipSilence = isSkipSilenceMode()

        mediaBrowser.setPlaybackParameters(PlaybackParameters(currentSpeed))
        playbackSpeedButton.setText(getString(R.string.player_playback_speed, currentSpeed))

        // TODO Skippare il silenzio
        skipSilenceToggleButton!!.setChecked(skipSilence)
    }

    private fun resetPlaybackParameters(mediaBrowser: MediaBrowser) {
        mediaBrowser.setPlaybackParameters(PlaybackParameters(1.0f))
        // TODO Resettare lo skip del silenzio
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mediaServiceBinder = service as LocalBinder?
            isServiceBound = true
            checkEqualizerBands()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mediaServiceBinder = null
            isServiceBound = false
        }
    }

    private fun bindMediaService() {
        val intent = Intent(requireActivity(), MediaService::class.java)
        intent.setAction(BaseMediaService.ACTION_BIND_EQUALIZER)
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        isServiceBound = true
    }

    private fun checkEqualizerBands() {
        if (mediaServiceBinder != null) {
            val eqManager = mediaServiceBinder!!.getEqualizerManager()
            val numBands = eqManager.getNumberOfBands()

            if (equalizerButton != null) {
                if (numBands.toInt() == 0) {
                    equalizerButton!!.setVisibility(View.GONE)

                    val params =
                        playerOpenQueueButton!!.getLayoutParams() as ConstraintLayout.LayoutParams
                    params.startToEnd = ConstraintLayout.LayoutParams.UNSET
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    playerOpenQueueButton!!.setLayoutParams(params)
                } else {
                    equalizerButton!!.setVisibility(View.VISIBLE)

                    val params =
                        playerOpenQueueButton!!.getLayoutParams() as ConstraintLayout.LayoutParams
                    params.startToStart = ConstraintLayout.LayoutParams.UNSET
                    params.startToEnd = R.id.player_open_equalizer_button
                    playerOpenQueueButton!!.setLayoutParams(params)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bindMediaService()
    }

    override fun onPause() {
        super.onPause()
        if (isServiceBound) {
            requireActivity().unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    companion object {
        private const val TAG = "PlayerCoverFragment"
    }
}
