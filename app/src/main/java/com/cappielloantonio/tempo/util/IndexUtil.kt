package com.cappielloantonio.tempo.util

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.subsonic.models.Artist
import com.cappielloantonio.tempo.subsonic.models.Indexes

@OptIn(markerClass = [UnstableApi::class])
object IndexUtil {
    @JvmStatic
    fun getArtist(indexes: Indexes): MutableList<Artist?> {
        if (indexes.indices == null) return mutableListOf<Artist?>()

        val toReturn = ArrayList<Artist?>()

        for (index in indexes.indices) {
            if (index.artists != null) {
                toReturn.addAll(index.artists!!)
            }
        }

        return toReturn
    }
}