package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.subsonic.models.Directory

class DirectoryViewModel(application: Application) : AndroidViewModel(application) {
    private val directoryRepository: DirectoryRepository

    init {
        directoryRepository = DirectoryRepository()
    }

    fun loadMusicDirectory(id: String?): LiveData<Directory?> {
        return directoryRepository.getMusicDirectory(id)
    }
}
