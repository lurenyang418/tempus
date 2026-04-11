package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.model.DownloadStack
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.util.ExternalAudioReader
import com.cappielloantonio.tempo.util.Preferences.getDefaultDownloadViewType
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import java.util.stream.Collectors

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val downloadRepository: DownloadRepository

    private val downloadedTrackSample = MutableLiveData<MutableList<Child?>?>(null)
    private val viewStack = MutableLiveData<ArrayList<DownloadStack?>?>(null)
    private val refreshResult = MutableLiveData<Int?>()

    init {
        downloadRepository = DownloadRepository()

        initViewStack(DownloadStack(getDefaultDownloadViewType(), null))
    }

    fun getDownloadedTracks(owner: LifecycleOwner): LiveData<MutableList<Child?>?> {
        downloadRepository.liveDownload.observe(
            owner,
            Observer { downloads: MutableList<Download> ->
                downloadedTrackSample.postValue(
                    downloads.map { it as? Child? }.toMutableList()
                )
            })
        return downloadedTrackSample
    }

    fun getViewStack(): LiveData<ArrayList<DownloadStack?>?> {
        return viewStack
    }

    fun getRefreshResult(): LiveData<Int?> {
        return refreshResult
    }

    fun initViewStack(level: DownloadStack?) {
        val stack = ArrayList<DownloadStack?>()
        stack.add(level)
        viewStack.setValue(stack)
    }

    fun pushViewStack(level: DownloadStack?) {
        val stack = viewStack.getValue()
        stack!!.add(level)
        viewStack.setValue(stack)
    }

    fun popViewStack() {
        val stack = viewStack.getValue()
        stack!!.removeAt(stack.size - 1)
        viewStack.setValue(stack)
    }

    fun refreshExternalDownloads() {
        Thread(Runnable {
            val directoryUri = getDownloadDirectoryUri()
            if (directoryUri == null) {
                refreshResult.postValue(-1)
                return@Runnable
            }

            val downloads: MutableList<Download> = downloadRepository.allDownloads
            if (downloads.isEmpty()) {
                refreshResult.postValue(0)
                return@Runnable
            }

            val toRemove = ArrayList<Download>()

            for (download in downloads) {
                val uriString = download.downloadUri
                if (uriString == null || uriString.isEmpty()) {
                    continue
                }

                val uri = Uri.parse(uriString)
                if (uri.getScheme() == null || !uri.getScheme()
                        .equals("content", ignoreCase = true)
                ) {
                    continue
                }

                var file: DocumentFile?
                try {
                    file = DocumentFile.fromSingleUri(getApplication<Application>(), uri)
                } catch (exception: SecurityException) {
                    file = null
                }

                if (file == null || !file.exists()) {
                    toRemove.add(download)
                }
            }
            if (!toRemove.isEmpty()) {
                val ids = ArrayList<String>()
                for (download in toRemove) {
                    ids.add(download.id)
                    ExternalAudioReader.removeMetadata(download)
                }

                if (ids.isNotEmpty()) {
                    downloadRepository.delete(ids)
                }
                ExternalAudioReader.refreshCache()
                refreshResult.postValue(ids.size)
            } else {
                refreshResult.postValue(0)
            }
        }).start()
    }

    companion object {
        private const val TAG = "DownloadViewModel"
    }
}
