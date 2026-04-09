package com.cappielloantonio.tempo.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.model.Server

@Dao
interface ServerDao {
    @get:Query("SELECT * FROM server")
    val all: LiveData<MutableList<Server?>?>?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(server: Server?)

    @Delete
    fun delete(server: Server?)
}