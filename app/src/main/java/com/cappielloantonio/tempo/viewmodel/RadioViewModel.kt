package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.RadioRepository
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    private val radioRepository: RadioRepository

    private val internetRadioStations = MutableLiveData<MutableList<InternetRadioStation?>?>(null)

    init {
        radioRepository = RadioRepository()
    }

    fun getInternetRadioStations(owner: LifecycleOwner): LiveData<MutableList<InternetRadioStation?>?> {
        radioRepository.internetRadioStations.observe(
            owner,
            Observer { value: MutableList<InternetRadioStation?>? ->
                internetRadioStations.postValue(value)
            })
        return internetRadioStations
    }

    fun refreshInternetRadioStations(owner: LifecycleOwner) {
        radioRepository.internetRadioStations.observe(
            owner,
            Observer { value: MutableList<InternetRadioStation?>? ->
                internetRadioStations.postValue(value)
            })
    }
}
