package com.cappielloantonio.tempo.interfaces

import androidx.annotation.Keep

@Keep
interface ScanCallback {
    fun onError(exception: Exception?) {}
    fun onSuccess(isScanning: Boolean, count: Long) {}
}
