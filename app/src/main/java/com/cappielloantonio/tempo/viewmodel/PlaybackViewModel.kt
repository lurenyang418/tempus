package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlaybackViewModel : ViewModel() {
    private val currentSongId = MutableLiveData<String?>(null)
    private val isPlaying = MutableLiveData<Boolean?>(false)

    fun getCurrentSongId(): LiveData<String?> {
        return currentSongId
    }

    fun getIsPlaying(): LiveData<Boolean?> {
        return isPlaying
    }

    fun update(songId: String?, playing: Boolean) {
        if (currentSongId.getValue() != songId) {
            currentSongId.postValue(songId)
        }
        if (isPlaying.getValue() != playing) {
            isPlaying.postValue(playing)
        }
    }

    fun clear() {
        currentSongId.postValue(null)
        isPlaying.postValue(false)
    }
}