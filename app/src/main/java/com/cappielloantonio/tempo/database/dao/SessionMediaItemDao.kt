package com.cappielloantonio.tempo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.model.SessionMediaItem

@Dao
interface SessionMediaItemDao {
    @Query("SELECT * FROM session_media_item WHERE id = :id")
    fun get(id: String?): SessionMediaItem?

    @Query("SELECT * FROM session_media_item WHERE timestamp = :timestamp")
    fun get(timestamp: Long): MutableList<SessionMediaItem?>?

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    fun insert(sessionMediaItem: SessionMediaItem?)

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    fun insertAll(sessionMediaItems: MutableList<SessionMediaItem?>?)

    @Query("DELETE FROM session_media_item")
    fun deleteAll()
}