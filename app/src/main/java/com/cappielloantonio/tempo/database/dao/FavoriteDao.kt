package com.cappielloantonio.tempo.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.model.Favorite

@Dao
interface FavoriteDao {
    @get:Query("SELECT * FROM favorite")
    val all: MutableList<Favorite?>?

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    fun insert(favorite: Favorite?)

    @Delete
    fun delete(favorite: Favorite?)

    @Query("DELETE FROM favorite")
    fun deleteAll()
}