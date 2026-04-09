package com.cappielloantonio.tempo.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.util.DownloadUtil
import java.io.IOException

@UnstableApi
class DownloaderManager(
    context: Context,
    private val dataSourceFactory: DataSource.Factory?,
    downloadManager: DownloadManager
) {
    private val context: Context
    private val downloadIndex: DownloadIndex

    init {
        this.context = context.getApplicationContext()

        downloadIndex = downloadManager.getDownloadIndex()

        loadDownloads()
    }

    private fun buildDownloadRequest(mediaItem: MediaItem): DownloadRequest {
        return DownloadHelper
            .forMediaItem(
                context,
                mediaItem,
                DownloadUtil.buildRenderersFactory(context, false),
                dataSourceFactory
            )
            .getDownloadRequest(Util.getUtf8Bytes(Assertions.checkNotNull<String?>(mediaItem.mediaId)))
            .copyWithId(mediaItem.mediaId)
    }

    fun isDownloaded(mediaId: String?): Boolean {
        val download: Download? = downloads.get(mediaId)
        return download != null && download.state != Download.STATE_FAILED
    }

    fun isDownloaded(mediaItem: MediaItem): Boolean {
        return isDownloaded(mediaItem.mediaId)
    }

    fun areDownloaded(mediaItems: MutableList<MediaItem?>): Boolean {
        return mediaItems.stream()
            .anyMatch { mediaItem: MediaItem? -> this.isDownloaded(mediaItem!!) }
    }

    fun download(mediaItem: MediaItem, download: com.cappielloantonio.tempo.model.Download) {
        download.downloadUri = mediaItem.requestMetadata.mediaUri.toString()

        DownloadService.sendAddDownload(
            context,
            DownloaderService::class.java,
            buildDownloadRequest(mediaItem),
            false
        )
        insertDatabase(download)
    }

    fun download(
        mediaItems: MutableList<MediaItem?>,
        downloads: MutableList<com.cappielloantonio.tempo.model.Download?>
    ) {
        for (counter in mediaItems.indices) {
            download(mediaItems.get(counter)!!, downloads.get(counter)!!)
        }
    }

    fun remove(mediaItem: MediaItem, download: com.cappielloantonio.tempo.model.Download) {
        DownloadService.sendRemoveDownload(
            context,
            DownloaderService::class.java,
            buildDownloadRequest(mediaItem).id,
            false
        )
        deleteDatabase(download.id)
        downloads.remove(download.id)
    }

    fun remove(
        mediaItems: MutableList<MediaItem?>,
        downloads: MutableList<com.cappielloantonio.tempo.model.Download?>
    ) {
        for (counter in mediaItems.indices) {
            remove(mediaItems.get(counter)!!, downloads.get(counter)!!)
        }
    }

    fun removeAll() {
        DownloadService.sendRemoveAllDownloads(context, DownloaderService::class.java, false)
        deleteAllDatabase()
        DownloadUtil.eraseDownloadFolder(context)
    }

    private fun loadDownloads() {
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.getDownload()
                    downloads.put(download.request.id, download)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
    }

    companion object {
        private const val TAG = "DownloaderManager"

        private val downloads: HashMap<String?, Download?> = HashMap()

        fun getDownloadNotificationMessage(id: String?): String? {
            val download: com.cappielloantonio.tempo.model.Download? =
                downloadRepository.getDownload(id)
            return if (download != null) download.title else null
        }

        fun updateRequestDownload(download: Download) {
            updateDatabase(download.request.id)
            downloads.put(download.request.id, download)
        }

        fun removeRequestDownload(download: Download) {
            deleteDatabase(download.request.id)
            downloads.remove(download.request.id)
        }

        private val downloadRepository: DownloadRepository
            get() = DownloadRepository()

        private fun insertDatabase(download: com.cappielloantonio.tempo.model.Download?) {
            download?.let { downloadRepository.insert(it) }
        }

        private fun deleteDatabase(id: String?) {
            downloadRepository.delete(id)
        }

        private fun deleteAllDatabase() {
            downloadRepository.deleteAll()
        }

        private fun updateDatabase(id: String?) {
            downloadRepository.update(id)
        }
    }
}