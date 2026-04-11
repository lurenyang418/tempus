package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.database.AppDatabase.Companion.instance
import com.cappielloantonio.tempo.database.dao.QueueDao
import com.cappielloantonio.tempo.model.Queue
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PlayQueue
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.stream.Collectors

class QueueRepository {
    private val queueDao = instance!!.queueDao()

    val liveQueue: LiveData<MutableList<Queue?>?>?
        get() = queueDao!!.all

    val media: MutableList<Child?>
        get() {
            var media: MutableList<Child?> = ArrayList<Child?>()

            val getMedia = GetMediaThreadSafe(queueDao!!)
            val thread = Thread(getMedia)
            thread.start()

            try {
                thread.join()
                media = getMedia.getMedia()
                    .mapNotNull { obj -> Child::class.java.cast(obj) }
                    .toMutableList()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return media
        }

    val playQueue: MutableLiveData<PlayQueue?>
        get() {
            val playQueue = MutableLiveData<PlayQueue?>()

            Log.d(TAG, "Getting play queue from server...")

            getSubsonicClientInstance(false)
                .bookmarksClient!!
                .playQueue
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.playQueue != null) {
                            val serverQueue = response.body()!!.subsonicResponse.playQueue
                            Log.d(
                                TAG, "Server returned play queue with " +
                                        (if (serverQueue!!.entries != null) serverQueue.entries!!.size else 0) + " items"
                            )
                            playQueue.setValue(serverQueue)
                        } else {
                            Log.d(
                                TAG,
                                "Server returned no play queue"
                            )
                            playQueue.setValue(null)
                        }
                    }

                    override fun onFailure(
                        call: Call<ApiResponse?>,
                        t: Throwable
                    ) {
                        Log.e(
                            TAG,
                            "Failed to get play queue",
                            t
                        )
                        playQueue.setValue(null)
                    }
                })

            return playQueue
        }

    fun savePlayQueue(ids: MutableList<String?>, current: String?, position: Long) {
        Log.d(TAG, "Saving play queue to server - Items: " + ids.size + ", Current: " + current)

        getSubsonicClientInstance(false)
            .bookmarksClient!!
            .savePlayQueue(ids, current, position)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Play queue saved successfully")
                    } else {
                        Log.d(TAG, "Play queue save failed with code: " + response.code())
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    Log.e(TAG, "Play queue save failed", t)
                }
            })
    }

    fun insert(media: Child, reset: Boolean, afterIndex: Int) {
        try {
            var mediaList: MutableList<Queue?> = ArrayList<Queue?>()

            if (!reset) {
                val getMediaThreadSafe = GetMediaThreadSafe(queueDao!!)
                val getMediaThread = Thread(getMediaThreadSafe)
                getMediaThread.start()
                getMediaThread.join()

                mediaList = getMediaThreadSafe.getMedia()
            }

            val queueItem = Queue(media)
            mediaList.add(afterIndex, queueItem)

            for (i in mediaList.indices) {
                mediaList.get(i)!!.trackOrder = i
            }

            val delete = Thread(QueueRepository.DeleteAllThreadSafe(queueDao!!))
            delete.start()
            delete.join()

            val insertAll = Thread(QueueRepository.InsertAllThreadSafe(queueDao, mediaList.filterNotNull().toMutableList()))
            insertAll.start()
            insertAll.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun isMediaInQueue(queue: MutableList<Queue?>?, media: Child?): Boolean {
        if (queue == null || media == null) return false
        return queue.any { queueItem ->
                queueItem != null &&
                    queueItem.id == media.id
        }
    }

    fun insertAll(toAdd: MutableList<Child?>, reset: Boolean, afterIndex: Int) {
        try {
            var media: MutableList<Queue?> = ArrayList<Queue?>()

            if (!reset) {
                val getMediaThreadSafe = GetMediaThreadSafe(queueDao!!)
                val getMediaThread = Thread(getMediaThreadSafe)
                getMediaThread.start()
                getMediaThread.join()

                media = getMediaThreadSafe.getMedia()
            }

            var filteredToAdd: MutableList<Child?>? = toAdd
            val finalMedia: MutableList<Queue?>? = media
            filteredToAdd = toAdd
                .filter { child -> !isMediaInQueue(finalMedia, child) }
                .toMutableList()

            for (i in filteredToAdd.indices) {
                val queueItem = Queue(filteredToAdd.get(i)!!)
                media.add(afterIndex + i, queueItem)
            }

            for (i in media.indices) {
                media.get(i)!!.trackOrder = i
            }

            val delete = Thread(QueueRepository.DeleteAllThreadSafe(queueDao!!))
            delete.start()
            delete.join()

            val insertAll = Thread(QueueRepository.InsertAllThreadSafe(queueDao, media.filterNotNull().toMutableList()))
            insertAll.start()
            insertAll.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun delete(position: Int) {
        val delete = QueueRepository.DeleteThreadSafe(queueDao!!, position)
        val thread = Thread(delete)
        thread.start()
    }

    fun deleteAll() {
        val deleteAll = QueueRepository.DeleteAllThreadSafe(queueDao!!)
        val thread = Thread(deleteAll)
        thread.start()
    }

    fun count(): Int {
        var count = 0

        val countThread = CountThreadSafe(queueDao!!)
        val thread = Thread(countThread)
        thread.start()

        try {
            thread.join()
            count = countThread.count
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return count
    }

    fun setLastPlayedTimestamp(id: String?) {
        val timestamp = SetLastPlayedTimestampThreadSafe(queueDao!!, id)
        val thread = Thread(timestamp)
        thread.start()
    }

    fun setPlayingPausedTimestamp(id: String?, ms: Long) {
        val timestamp = SetPlayingPausedTimestampThreadSafe(queueDao!!, id, ms)
        val thread = Thread(timestamp)
        thread.start()
    }

    val lastPlayedMediaIndex: Int
        get() {
            var index = 0

            val getLastPlayedMediaThreadSafe =
                GetLastPlayedMediaThreadSafe(queueDao!!)
            val thread = Thread(getLastPlayedMediaThreadSafe)
            thread.start()

            try {
                thread.join()
                val lastMediaPlayed =
                    getLastPlayedMediaThreadSafe.queueItem
                index = lastMediaPlayed?.trackOrder ?: 0
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return index
        }

    val lastPlayedMediaTimestamp: Long
        get() {
            var timestamp: Long = 0

            val getLastPlayedMediaThreadSafe =
                GetLastPlayedMediaThreadSafe(queueDao!!)
            val thread = Thread(getLastPlayedMediaThreadSafe)
            thread.start()

            try {
                thread.join()
                val lastMediaPlayed =
                    getLastPlayedMediaThreadSafe.queueItem
                timestamp = lastMediaPlayed?.playingChanged ?: 0
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return timestamp
        }

    private class GetMediaThreadSafe(private val queueDao: QueueDao) : Runnable {
        private var media: MutableList<Queue?>? = null

        override fun run() {
            media = queueDao.allSimple
        }

        fun getMedia(): MutableList<Queue?> {
            return media!!
        }
    }

    private class InsertAllThreadSafe(
        private val queueDao: QueueDao,
        private val media: MutableList<Queue>
    ) : Runnable {
        override fun run() {
            queueDao.insertAll(media)
        }
    }

    private class DeleteThreadSafe(private val queueDao: QueueDao, private val position: Int) :
        Runnable {
        override fun run() {
            queueDao.delete(position)
        }
    }

    private class DeleteAllThreadSafe(private val queueDao: QueueDao) : Runnable {
        override fun run() {
            queueDao.deleteAll()
        }
    }

    private class CountThreadSafe(private val queueDao: QueueDao) : Runnable {
        var count: Int = 0
            private set

        override fun run() {
            count = queueDao.count()
        }
    }

    private class SetLastPlayedTimestampThreadSafe(
        private val queueDao: QueueDao,
        private val mediaId: String?
    ) : Runnable {
        override fun run() {
            queueDao.setLastPlay(mediaId, System.currentTimeMillis())
        }
    }

    private class SetPlayingPausedTimestampThreadSafe(
        private val queueDao: QueueDao,
        private val mediaId: String?,
        private val ms: Long
    ) : Runnable {
        override fun run() {
            queueDao.setPlayingChanged(mediaId, ms)
        }
    }

    private class GetLastPlayedMediaThreadSafe(private val queueDao: QueueDao) : Runnable {
        private var lastMediaPlayed: Queue? = null

        override fun run() {
            lastMediaPlayed = queueDao.lastPlayed
        }

        val queueItem: Queue?
            get() = lastMediaPlayed
    }

    companion object {
        private const val TAG = "QueueRepository"
    }
}
