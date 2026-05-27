package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.repository.QueueRepository
import com.cappielloantonio.tempo.repository.SystemRepository
import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension
import com.cappielloantonio.tempo.subsonic.models.SubsonicResponse

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val systemRepository: SystemRepository

    init {
        systemRepository = SystemRepository()
    }

    val isQueueLoaded: Boolean
        get() {
            val queueRepository = QueueRepository()
            return queueRepository.count() != 0
        }

    fun ping(): LiveData<SubsonicResponse?> {
        return systemRepository.ping()
    }

    val openSubsonicExtensions: LiveData<MutableList<OpenSubsonicExtension?>?>
        get() = systemRepository.openSubsonicExtensions

    companion object {
        private const val TAG = "SearchViewModel"
    }
}
