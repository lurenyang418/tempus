package com.cappielloantonio.tempo.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cappielloantonio.tempo.model.LyricsCache

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_cache WHERE song_id = :songId")
    fun getOne(songId: String?): LyricsCache?

    @Query("SELECT * FROM lyrics_cache WHERE song_id = :songId")
    fun observeOne(songId: String?): LiveData<LyricsCache?>?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(lyricsCache: LyricsCache)

    @Query("DELETE FROM lyrics_cache WHERE song_id = :songId")
    fun delete(songId: String?)
}