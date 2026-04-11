package com.cappielloantonio.tempo.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.model.Chronology

@Dao
interface ChronologyDao {
    @Query("SELECT * FROM chronology WHERE server == :server GROUP BY id ORDER BY timestamp DESC LIMIT :count")
    fun getLastPlayed(server: String?, count: Int): LiveData<MutableList<Chronology>>

    @Query("SELECT * FROM chronology WHERE timestamp >= :endDate AND timestamp < :startDate AND server == :server GROUP BY id ORDER BY COUNT(id) DESC LIMIT 20")
    fun getAllFrom(
        startDate: Long,
        endDate: Long,
        server: String?
    ): LiveData<MutableList<Chronology>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(chronologyObject: Chronology)
}