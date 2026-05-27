package com.cappielloantonio.tempo.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaMetadata
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.DialogTrackInfoBinding
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.glide.CustomGlideRequest.Builder.Companion.from
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.AssetLinkUtil.AssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.applyLinkAppearance
import com.cappielloantonio.tempo.util.AssetLinkUtil.buildAssetLink
import com.cappielloantonio.tempo.util.AssetLinkUtil.clearLinkAppearance
import com.cappielloantonio.tempo.util.AssetLinkUtil.copyToClipboard
import com.cappielloantonio.tempo.util.AssetLinkUtil.parseLinkString
import com.cappielloantonio.tempo.util.Constants
import com.cappielloantonio.tempo.util.MusicUtil
import com.cappielloantonio.tempo.util.Preferences.isServerPrioritized
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TrackInfoDialog(private val mediaMetadata: MediaMetadata) : DialogFragment() {
    private var bind: DialogTrackInfoBinding? = null

    private var songLink: AssetLink? = null
    private var albumLink: AssetLink? = null
    private var artistLink: AssetLink? = null
    private var genreLink: AssetLink? = null
    private var yearLink: AssetLink? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bind = DialogTrackInfoBinding.inflate(getLayoutInflater())

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(bind!!.getRoot())
            .setPositiveButton(
                R.string.track_info_dialog_positive_button,
                DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog!!.cancel() })
            .create()
    }

    override fun onStart() {
        super.onStart()

        setTrackInfo()
        setTrackTranscodingInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun setTrackInfo() {
        genreLink = null
        yearLink = null

        bind!!.trakTitleInfoTextView.setText(mediaMetadata.title)
        bind!!.trakArtistInfoTextView.setText(
            if (mediaMetadata.artist != null)
                mediaMetadata.artist
            else
                ""
        )

        if (mediaMetadata.extras != null) {
            songLink =
                buildAssetLink(AssetLinkUtil.TYPE_SONG, mediaMetadata.extras!!.getString("id"))
            albumLink = buildAssetLink(
                AssetLinkUtil.TYPE_ALBUM,
                mediaMetadata.extras!!.getString("albumId")
            )
            artistLink = buildAssetLink(
                AssetLinkUtil.TYPE_ARTIST,
                mediaMetadata.extras!!.getString("artistId")
            )
            genreLink = parseLinkString(mediaMetadata.extras!!.getString("assetLinkGenre"))
            yearLink = parseLinkString(mediaMetadata.extras!!.getString("assetLinkYear"))

            from(
                requireContext(),
                mediaMetadata.extras!!.getString("coverArtId", ""),
                CustomGlideRequest.ResourceType.Song
            )
                .build()
                .into(bind!!.trackCoverInfoImageView)

            bindAssetLink(
                bind!!.trackCoverInfoImageView,
                if (albumLink != null) albumLink else songLink
            )
            bindAssetLink(bind!!.trakTitleInfoTextView, songLink)
            bindAssetLink(
                bind!!.trakArtistInfoTextView,
                if (artistLink != null) artistLink else songLink
            )

            val titleValue =
                mediaMetadata.extras!!.getString("title", getString(R.string.label_placeholder))
            val albumValue =
                mediaMetadata.extras!!.getString("album", getString(R.string.label_placeholder))
            val artistValue =
                mediaMetadata.extras!!.getString("artist", getString(R.string.label_placeholder))
            val genreValue =
                mediaMetadata.extras!!.getString("genre", getString(R.string.label_placeholder))
            val yearValue = mediaMetadata.extras!!.getInt("year", 0)

            if (genreLink == null && genreValue != null && !genreValue.isEmpty() && !getString(R.string.label_placeholder).contentEquals(
                    genreValue
                )
            ) {
                genreLink = buildAssetLink(AssetLinkUtil.TYPE_GENRE, genreValue)
            }

            if (yearLink == null && yearValue != 0) {
                yearLink = buildAssetLink(AssetLinkUtil.TYPE_YEAR, yearValue.toString())
            }

            bind!!.titleValueSector.setText(titleValue)
            bind!!.albumValueSector.setText(albumValue)
            bind!!.artistValueSector.setText(artistValue)
            bind!!.trackNumberValueSector.setText(
                if (mediaMetadata.extras!!.getInt(
                        "track",
                        0
                    ) != 0
                ) mediaMetadata.extras!!.getInt("track", 0)
                    .toString() else getString(R.string.label_placeholder)
            )
            bind!!.yearValueSector.setText(if (yearValue != 0) yearValue.toString() else getString(R.string.label_placeholder))
            bind!!.genreValueSector.setText(genreValue)
            bind!!.sizeValueSector.setText(
                if (mediaMetadata.extras!!.getLong(
                        "size",
                        0
                    ) != 0L
                ) MusicUtil.getReadableByteCount(
                    mediaMetadata.extras!!.getLong(
                        "size",
                        0
                    )
                ) else getString(R.string.label_placeholder)
            )
            bind!!.contentTypeValueSector.setText(
                mediaMetadata.extras!!.getString(
                    "contentType",
                    getString(R.string.label_placeholder)
                )
            )
            bind!!.suffixValueSector.setText(
                mediaMetadata.extras!!.getString(
                    "suffix",
                    getString(R.string.label_placeholder)
                )
            )
            bind!!.transcodedContentTypeValueSector.setText(
                mediaMetadata.extras!!.getString(
                    "transcodedContentType",
                    getString(R.string.label_placeholder)
                )
            )
            bind!!.transcodedSuffixValueSector.setText(
                mediaMetadata.extras!!.getString(
                    "transcodedSuffix",
                    getString(R.string.label_placeholder)
                )
            )
            bind!!.durationValueSector.setText(
                if (mediaMetadata.extras!!.getInt(
                        "duration",
                        0
                    ) != 0
                ) MusicUtil.getReadableDurationString(
                    mediaMetadata.extras!!.getInt("duration", 0),
                    false
                ) else getString(R.string.label_placeholder)
            )
            bind!!.bitrateValueSector.setText(
                if (mediaMetadata.extras!!.getInt(
                        "bitrate",
                        0
                    ) != 0
                ) mediaMetadata.extras!!.getInt("bitrate", 0)
                    .toString() + " kbps" else getString(R.string.label_placeholder)
            )
            bind!!.samplingRateValueSector.setText(
                if (mediaMetadata.extras!!.getInt(
                        "samplingRate",
                        0
                    ) != 0
                ) mediaMetadata.extras!!.getInt("samplingRate", 0)
                    .toString() + " Hz" else getString(R.string.label_placeholder)
            )
            bind!!.bitDepthValueSector.setText(
                if (mediaMetadata.extras!!.getInt(
                        "bitDepth",
                        0
                    ) != 0
                ) mediaMetadata.extras!!.getInt("bitDepth", 0)
                    .toString() + " bits" else getString(R.string.label_placeholder)
            )
            bind!!.pathValueSector.setText(
                mediaMetadata.extras!!.getString(
                    "path",
                    getString(R.string.label_placeholder)
                )
            )
            bind!!.discNumberValueSector.setText(
                if (mediaMetadata.extras!!.getInt(
                        "discNumber",
                        0
                    ) != 0
                ) mediaMetadata.extras!!.getInt("discNumber", 0)
                    .toString() else getString(R.string.label_placeholder)
            )

            bindAssetLink(bind!!.titleValueSector, songLink)
            bindAssetLink(bind!!.albumValueSector, albumLink)
            bindAssetLink(bind!!.artistValueSector, artistLink)
            bindAssetLink(bind!!.genreValueSector, genreLink)
            bindAssetLink(bind!!.yearValueSector, yearLink)
        }
    }

    private fun setTrackTranscodingInfo() {
        val info = StringBuilder()

        val prioritizeServerTranscoding = isServerPrioritized()

        val transcodingExtension = MusicUtil.getTranscodingFormatPreference()
        val transcodingBitrate =
            if (MusicUtil.getBitratePreference().toInt() != 0) MusicUtil.getBitratePreference()
                .toInt().toString() + "kbps" else "Original"

        if (mediaMetadata.extras != null && mediaMetadata.extras!!.getString("uri", "").contains(
                Constants.DOWNLOAD_URI
            )
        ) {
            info.append(getString(R.string.track_info_summary_downloaded_file))

            bind!!.trakTranscodingInfoTextView.setText(info)
            return
        }

        if (prioritizeServerTranscoding) {
            info.append(getString(R.string.track_info_summary_server_prioritized))

            bind!!.trakTranscodingInfoTextView.setText(info)
            return
        }

        if (!prioritizeServerTranscoding && transcodingExtension == "raw" && transcodingBitrate == "Original") {
            info.append(getString(R.string.track_info_summary_original_file))

            bind!!.trakTranscodingInfoTextView.setText(info)
            return
        }

        if (!prioritizeServerTranscoding && (transcodingExtension != "raw") && transcodingBitrate == "Original") {
            info.append(
                getString(
                    R.string.track_info_summary_transcoding_codec,
                    transcodingExtension
                )
            )

            bind!!.trakTranscodingInfoTextView.setText(info)
            return
        }

        if (!prioritizeServerTranscoding && transcodingExtension == "raw" && (transcodingBitrate != "Original")) {
            info.append(
                getString(
                    R.string.track_info_summary_transcoding_bitrate,
                    transcodingBitrate
                )
            )

            bind!!.trakTranscodingInfoTextView.setText(info)
            return
        }

        if (!prioritizeServerTranscoding && (transcodingExtension != "raw") && (transcodingBitrate != "Original")) {
            info.append(
                getString(
                    R.string.track_info_summary_full_transcode,
                    transcodingExtension,
                    transcodingBitrate
                )
            )

            bind!!.trakTranscodingInfoTextView.setText(info)
        }
    }

    private fun bindAssetLink(view: View?, assetLink: AssetLink?) {
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
            dismissAllowingStateLoss()
            val collapse = AssetLinkUtil.TYPE_SONG != assetLink.type
            (requireActivity() as MainActivity).openAssetLink(assetLink, collapse)
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
}
