package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.subsonic.models.Indexes
import com.cappielloantonio.tempo.subsonic.models.MusicFolder

class IndexViewModel(application: Application) : AndroidViewModel(application) {
    private val directoryRepository: DirectoryRepository

    private var musicFolder: MusicFolder? = null

    init {
        directoryRepository = DirectoryRepository()
    }

    fun getIndexes(musicFolderId: String?): MutableLiveData<Indexes?> {
        return directoryRepository.getIndexes(musicFolderId, null)
    }

    val musicFolderName: String?
        get() = if (musicFolder != null) musicFolder!!.name else ""

    fun setMusicFolder(musicFolder: MusicFolder?) {
        this.musicFolder = musicFolder
    }
}
