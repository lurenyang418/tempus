package com.cappielloantonio.tempo.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.subsonic.models.Playlist

@Dao
interface PlaylistDao {
    @get:Query("SELECT * FROM playlist")
    val all: LiveData<MutableList<Playlist?>?>?

    @get:Query("SELECT * FROM playlist")
    val allSync: MutableList<Playlist?>?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(playlist: Playlist?)

    @Delete
    fun delete(playlist: Playlist?)
}