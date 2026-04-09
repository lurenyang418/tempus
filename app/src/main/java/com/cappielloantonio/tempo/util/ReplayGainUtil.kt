package com.cappielloantonio.tempo.util

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.id3.InternalFrame
import com.cappielloantonio.tempo.model.ReplayGain
import com.cappielloantonio.tempo.util.Preferences.getReplayGainMode
import kotlin.math.pow

@OptIn(markerClass = [UnstableApi::class])
object ReplayGainUtil {
    private val tags = arrayOf<String?>(
        "REPLAYGAIN_TRACK_GAIN",
        "REPLAYGAIN_ALBUM_GAIN",
        "R128_TRACK_GAIN",
        "R128_ALBUM_GAIN"
    )

    fun setReplayGain(player: Player, tracks: Tracks?) {
        val metadata = getMetadata(tracks)
        val gains = getReplayGains(metadata)

        applyReplayGain(player, gains)
    }

    private fun getMetadata(tracks: Tracks?): MutableList<Metadata?> {
        val metadata: MutableList<Metadata?> = ArrayList<Metadata?>()

        if (tracks != null && !tracks.getGroups().isEmpty()) {
            for (i in tracks.getGroups().indices) {
                val group = tracks.getGroups().get(i)

                if (group != null && group.getMediaTrackGroup() != null) {
                    for (j in 0..<group.getMediaTrackGroup().length) {
                        metadata.add(group.getTrackFormat(j).metadata)
                    }
                }
            }
        }

        return metadata
    }

    private fun getReplayGains(metadata: MutableList<Metadata?>?): MutableList<ReplayGain?> {
        val gains: MutableList<ReplayGain?> = ArrayList<ReplayGain?>()

        if (metadata != null) {
            for (i in metadata.indices) {
                val singleMetadata = metadata.get(i)

                if (singleMetadata != null) {
                    for (j in 0..<singleMetadata.length()) {
                        val entry = singleMetadata.get(j)

                        if (checkReplayGain(entry)) {
                            val replayGain = setReplayGains(entry)
                            gains.add(replayGain)
                        }
                    }
                }
            }
        }

        if (gains.isEmpty()) gains.add(0, ReplayGain())
        if (gains.size == 1) gains.add(1, ReplayGain())

        return gains
    }

    private fun checkReplayGain(entry: Metadata.Entry): Boolean {
        for (tag in tags) {
            if (entry.toString().contains(tag!!)) {
                return true
            }
        }

        return false
    }

    private fun setReplayGains(entry: Metadata.Entry): ReplayGain {
        val replayGain = ReplayGain()

        // The logic below assumes .toString() contains the dB value. That's not the case for InternalFrame
        var str = entry.toString()
        if (entry is InternalFrame) {
            str = entry.description + entry.text
        }

        if (str.contains(tags[0]!!)) {
            replayGain.trackGain = parseReplayGainTag(str)
        }

        if (str.contains(tags[1]!!)) {
            replayGain.albumGain = parseReplayGainTag(str)
        }

        if (str.contains(tags[2]!!)) {
            replayGain.trackGain = parseReplayGainTag(str) / 256f
        }

        if (str.contains(tags[3]!!)) {
            replayGain.albumGain = parseReplayGainTag(str) / 256f
        }

        return replayGain
    }

    private fun parseReplayGainTag(entry: String): Float {
        try {
            return entry.toString().replace("[^\\d.-]".toRegex(), "").toFloat()
        } catch (exception: NumberFormatException) {
            return 0f
        }
    }

    private fun applyReplayGain(player: Player, gains: MutableList<ReplayGain?>?) {
        if (getReplayGainMode() == "disabled" || gains == null || gains.isEmpty()) {
            setNoReplayGain(player)
            return
        }

        if (getReplayGainMode() == "auto") {
            if (areTracksConsecutive(player)) {
                setAutoReplayGain(player, gains)
            } else {
                setTrackReplayGain(player, gains)
            }

            return
        }

        if (getReplayGainMode() == "track") {
            setTrackReplayGain(player, gains)
            return
        }

        if (getReplayGainMode() == "album") {
            setAlbumReplayGain(player, gains)
            return
        }

        setNoReplayGain(player)
    }

    private fun setNoReplayGain(player: Player) {
        setReplayGain(player, 0f)
    }

    private fun setTrackReplayGain(player: Player, gains: MutableList<ReplayGain?>) {
        val trackGain =
            if (gains.get(0)!!.trackGain != 0f) gains.get(0)!!.trackGain else gains.get(1)!!.trackGain

        setReplayGain(player, if (trackGain != 0f) trackGain else 0f)
    }

    private fun setAlbumReplayGain(player: Player, gains: MutableList<ReplayGain?>) {
        val albumGain =
            if (gains.get(0)!!.albumGain != 0f) gains.get(0)!!.albumGain else gains.get(1)!!.albumGain

        setReplayGain(player, if (albumGain != 0f) albumGain else 0f)
    }

    private fun setAutoReplayGain(player: Player, gains: MutableList<ReplayGain?>) {
        val albumGain =
            if (gains.get(0)!!.albumGain != 0f) gains.get(0)!!.albumGain else gains.get(1)!!.albumGain
        val trackGain =
            if (gains.get(0)!!.trackGain != 0f) gains.get(0)!!.trackGain else gains.get(1)!!.trackGain

        setReplayGain(player, if (albumGain != 0f) albumGain else trackGain)
    }

    private fun areTracksConsecutive(player: Player): Boolean {
        val currentMediaItem = player.getCurrentMediaItem()
        val prevMediaItemIndex = player.getPreviousMediaItemIndex()
        val pastMediaItem =
            if (prevMediaItemIndex == C.INDEX_UNSET) null else player.getMediaItemAt(
                prevMediaItemIndex
            )

        return currentMediaItem != null && pastMediaItem != null && pastMediaItem.mediaMetadata.albumTitle != null && currentMediaItem.mediaMetadata.albumTitle != null &&
                pastMediaItem.mediaMetadata.albumTitle.toString() == currentMediaItem.mediaMetadata.albumTitle.toString()
    }

    private fun setReplayGain(player: Player, gain: Float) {
        player.setVolume(10.0.pow((gain / 20f).toDouble()).toFloat())
    }
}
