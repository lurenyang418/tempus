package com.cappielloantonio.tempo.github.utils

import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.github.models.LatestRelease

object UpdateUtil {
    fun showUpdateDialog(release: LatestRelease): Boolean {
        if (release.tagName == null) return false
        val remoteTag = release.tagName!!.replace("^\\D+".toRegex(), "")

        try {
            val local: Array<String?> =
                BuildConfig.VERSION_NAME.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val remote: Array<String?> =
                remoteTag.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (i in local.indices) {
                val localPart = local[i]!!.toInt()
                val remotePart = remote[i]!!.toInt()

                if (localPart > remotePart) {
                    return false
                } else if (localPart < remotePart) {
                    return true
                }
            }
        } catch (exception: Exception) {
            return false
        }

        return false
    }
}
