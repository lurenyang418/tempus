package com.cappielloantonio.tempo.util

import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.cappielloantonio.tempo.util.Preferences.getOpenSubsonicExtensions
import com.cappielloantonio.tempo.util.Preferences.isOpenSubsonic
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

object OpenSubsonicExtensionsUtil {
    private val openSubsonicExtensions: MutableList<OpenSubsonicExtension?>?
        get() {
            var extensions: MutableList<OpenSubsonicExtension?>? = null

            if (isOpenSubsonic() && getOpenSubsonicExtensions() != null) {
                extensions =
                    Gson().fromJson<MutableList<OpenSubsonicExtension?>?>(
                        getOpenSubsonicExtensions(),
                        object :
                            TypeToken<MutableList<OpenSubsonicExtension?>?>() {
                        }.getType()
                    )
            }

            return extensions
        }

    private fun getOpenSubsonicExtension(extensionName: String?): OpenSubsonicExtension? {
        if (openSubsonicExtensions == null) return null

        return openSubsonicExtensions!!.stream()
            .filter { openSubsonicExtension: OpenSubsonicExtension? -> openSubsonicExtension!!.name == extensionName }
            .findAny().orElse(null)
    }

    val isTranscodeOffsetExtensionAvailable: Boolean
        get() = getOpenSubsonicExtension("transcodeOffset") != null

    val isFormPostExtensionAvailable: Boolean
        get() = getOpenSubsonicExtension("formPost") != null

    val isSongLyricsExtensionAvailable: Boolean
        get() = getOpenSubsonicExtension("songLyrics") != null
}
