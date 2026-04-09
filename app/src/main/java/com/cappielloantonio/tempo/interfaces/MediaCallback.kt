package com.cappielloantonio.tempo.interfaces

import androidx.annotation.Keep

@Keep
interface MediaCallback {
    fun onError(exception: Exception?) {}
    fun onLoadMedia(media: MutableList<*>?) {}
}
