package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.model.Server
import com.cappielloantonio.tempo.repository.ServerRepository

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val serverRepository: ServerRepository

    var serverToEdit: Server? = null

    init {
        serverRepository = ServerRepository()
    }

    val serverList: LiveData<MutableList<Server?>?>?
        get() = serverRepository.liveServer

    fun addServer(server: Server?) {
        serverRepository.insert(server)
    }

    fun deleteServer(server: Server?) {
        if (server != null) {
            serverRepository.delete(server)
        } else if (this.serverToEdit != null) {
            serverRepository.delete(this.serverToEdit)
        }
    }
}
