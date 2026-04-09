package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cappielloantonio.tempo.interfaces.ScanCallback
import com.cappielloantonio.tempo.repository.ScanRepository

class SettingViewModel(application: Application) : AndroidViewModel(application) {
    private val scanRepository: ScanRepository

    init {
        scanRepository = ScanRepository()
    }

    fun launchScan(callback: ScanCallback) {
        scanRepository.startScan(object : ScanCallback {
            override fun onError(exception: Exception?) {
                callback.onError(exception)
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                callback.onSuccess(isScanning, count)
            }
        })
    }

    fun getScanStatus(callback: ScanCallback) {
        scanRepository.getScanStatus(object : ScanCallback {
            override fun onError(exception: Exception?) {
                callback.onError(exception)
            }

            override fun onSuccess(isScanning: Boolean, count: Long) {
                callback.onSuccess(isScanning, count)
            }
        })
    }

    companion object {
        private const val TAG = "SettingViewModel"
    }
}
