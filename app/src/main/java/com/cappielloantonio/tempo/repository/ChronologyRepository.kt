package com.cappielloantonio.tempo.repository

import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.database.AppDatabase.Companion.instance
import com.cappielloantonio.tempo.database.dao.ChronologyDao
import com.cappielloantonio.tempo.model.Chronology

class ChronologyRepository {
    private val chronologyDao = instance!!.chronologyDao()

    fun getChronology(
        server: String?,
        start: Long,
        end: Long
    ): LiveData<MutableList<Chronology>> {
        return chronologyDao!!.getAllFrom(start, end, server)
    }

    fun insert(item: Chronology) {
        val insert = ChronologyRepository.InsertThreadSafe(chronologyDao!!, item)
        val thread = Thread(insert)
        thread.start()
    }

    private class InsertThreadSafe(
        private val chronologyDao: ChronologyDao,
        private val item: Chronology
    ) : Runnable {
        override fun run() {
            chronologyDao.insert(item)
        }
    }
}
