package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.subsonic.models.Share

class ShareBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val sharingRepository: SharingRepository

    private var share: Share? = null

    init {
        sharingRepository = SharingRepository()
    }

    fun getShare(): Share {
        return share!!
    }

    fun setShare(share: Share) {
        this.share = share
    }

    fun updateShare(description: String?, expires: Long) {
        sharingRepository.updateShare(share!!.id, description, expires)
    }

    fun deleteShare() {
        sharingRepository.deleteShare(share!!.id)
    }
}
