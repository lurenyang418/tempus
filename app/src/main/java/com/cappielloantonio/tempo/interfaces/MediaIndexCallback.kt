package com.cappielloantonio.tempo.interfaces

import androidx.annotation.Keep

@Keep
interface MediaIndexCallback {
    fun onRecovery(index: Int) {}
}
