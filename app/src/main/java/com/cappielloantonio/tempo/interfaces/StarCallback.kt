package com.cappielloantonio.tempo.interfaces

import androidx.annotation.Keep

@Keep
interface StarCallback {
    fun onError() {}
    fun onSuccess() {}
}
