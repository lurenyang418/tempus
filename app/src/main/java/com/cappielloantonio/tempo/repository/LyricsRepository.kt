package com.cappielloantonio.tempo.repository

import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.database.AppDatabase.Companion.instance
import com.cappielloantonio.tempo.database.dao.LyricsDao
import com.cappielloantonio.tempo.model.LyricsCache

class LyricsRepository {
    private val lyricsDao = instance!!.lyricsDao()

    fun getLyrics(songId: String?): LyricsCache? {
        val getLyricsThreadSafe = GetLyricsThreadSafe(lyricsDao!!, songId)
        val thread = Thread(getLyricsThreadSafe)
        thread.start()

        try {
            thread.join()
            return getLyricsThreadSafe.lyrics
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return null
    }

    fun observeLyrics(songId: String?): LiveData<LyricsCache?>? {
        return lyricsDao!!.observeOne(songId)
    }

    fun insert(lyricsCache: LyricsCache) {
        val insert = LyricsRepository.InsertThreadSafe(lyricsDao!!, lyricsCache)
        val thread = Thread(insert)
        thread.start()
    }

    fun delete(songId: String?) {
        val delete = LyricsRepository.DeleteThreadSafe(lyricsDao!!, songId)
        val thread = Thread(delete)
        thread.start()
    }

    private class GetLyricsThreadSafe(
        private val lyricsDao: LyricsDao,
        private val songId: String?
    ) : Runnable {
        var lyrics: LyricsCache? = null
            private set

        override fun run() {
            this.lyrics = lyricsDao.getOne(songId)
        }
    }

    private class InsertThreadSafe(
        private val lyricsDao: LyricsDao,
        private val lyricsCache: LyricsCache
    ) : Runnable {
        override fun run() {
            lyricsDao.insert(lyricsCache)
        }
    }

    private class DeleteThreadSafe(private val lyricsDao: LyricsDao, private val songId: String?) :
        Runnable {
        override fun run() {
            lyricsDao.delete(songId)
        }
    }
}