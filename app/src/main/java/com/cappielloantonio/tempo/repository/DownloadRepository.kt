package com.cappielloantonio.tempo.repository

import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.database.AppDatabase.Companion.instance
import com.cappielloantonio.tempo.database.dao.DownloadDao
import com.cappielloantonio.tempo.model.Download

class DownloadRepository {
    private val downloadDao = instance!!.downloadDao()

    val liveDownload: LiveData<MutableList<Download?>?>?
        get() = downloadDao!!.all

    val allDownloads: MutableList<Download?>?
        get() {
            val getDownloads = GetAllDownloadsThreadSafe(downloadDao!!)
            val thread = Thread(getDownloads)
            thread.start()

            try {
                thread.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return getDownloads.downloads
        }

    fun getDownload(id: String?): Download? {
        var download: Download? = null

        val getDownloadThreadSafe = GetDownloadThreadSafe(downloadDao!!, id)
        val thread = Thread(getDownloadThreadSafe)
        thread.start()

        try {
            thread.join()
            download = getDownloadThreadSafe.download
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return download
    }

    private class GetAllDownloadsThreadSafe(private val downloadDao: DownloadDao) : Runnable {
        var downloads: MutableList<Download?>? = null
            private set

        override fun run() {
            downloads = downloadDao.allSync
        }
    }

    private class GetDownloadThreadSafe(
        private val downloadDao: DownloadDao,
        private val id: String?
    ) : Runnable {
        var download: Download? = null
            private set

        override fun run() {
            download = downloadDao.getOne(id)
        }
    }

    fun insert(download: Download) {
        val insert = DownloadRepository.InsertThreadSafe(downloadDao!!, download)
        val thread = Thread(insert)
        thread.start()
    }

    private class InsertThreadSafe(
        private val downloadDao: DownloadDao,
        private val download: Download
    ) : Runnable {
        override fun run() {
            downloadDao.insert(download)
        }
    }

    fun update(id: String?) {
        val update = UpdateThreadSafe(downloadDao!!, id)
        val thread = Thread(update)
        thread.start()
    }

    private class UpdateThreadSafe(private val downloadDao: DownloadDao, private val id: String?) :
        Runnable {
        override fun run() {
            downloadDao.update(id)
        }
    }

    fun insertAll(downloads: MutableList<Download>) {
        val insertAll = DownloadRepository.InsertAllThreadSafe(downloadDao!!, downloads)
        val thread = Thread(insertAll)
        thread.start()
    }

    private class InsertAllThreadSafe(
        private val downloadDao: DownloadDao,
        private val downloads: MutableList<Download>
    ) : Runnable {
        override fun run() {
            downloadDao.insertAll(downloads)
        }
    }

    fun deleteAll() {
        val deleteAll = DownloadRepository.DeleteAllThreadSafe(downloadDao!!)
        val thread = Thread(deleteAll)
        thread.start()
    }

    private class DeleteAllThreadSafe(private val downloadDao: DownloadDao) : Runnable {
        override fun run() {
            downloadDao.deleteAll()
        }
    }

    fun delete(id: String?) {
        val delete = DownloadRepository.DeleteThreadSafe(downloadDao!!, id)
        val thread = Thread(delete)
        thread.start()
    }

    fun delete(ids: MutableList<String?>?) {
        val delete = DeleteMultipleThreadSafe(downloadDao!!, ids)
        val thread = Thread(delete)
        thread.start()
    }

    private class DeleteThreadSafe(private val downloadDao: DownloadDao, private val id: String?) :
        Runnable {
        override fun run() {
            downloadDao.delete(id)
        }
    }

    private class DeleteMultipleThreadSafe(
        private val downloadDao: DownloadDao,
        private val ids: MutableList<String?>?
    ) : Runnable {
        override fun run() {
            downloadDao.deleteByIds(ids)
        }
    }
}
