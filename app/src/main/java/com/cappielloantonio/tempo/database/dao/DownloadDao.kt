package com.cappielloantonio.tempo.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.model.Download

@Dao
interface DownloadDao {
    @get:Query("SELECT * FROM download WHERE download_state = 1 ORDER BY artist, album, disc_number, track ASC")
    val all: LiveData<MutableList<Download?>?>?

    @get:Query("SELECT * FROM download WHERE download_state = 1 ORDER BY artist, album, disc_number, track ASC")
    val allSync: MutableList<Download?>?

    @Query("SELECT * FROM download WHERE id = :id")
    fun getOne(id: String?): Download?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(download: Download)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insertAll(downloads: MutableList<Download>)

    @Query("UPDATE download SET download_state = 1 WHERE id = :id")
    fun update(id: String?)

    @Query("DELETE FROM download WHERE id = :id")
    fun delete(id: String?)

    @Query("DELETE FROM download WHERE id IN (:ids)")
    fun deleteByIds(ids: MutableList<String?>?)

    @Query("DELETE FROM download")
    fun deleteAll()
}