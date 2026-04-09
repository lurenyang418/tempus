package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child

class RatingViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository

    private var song: Child? = null
    private var album: AlbumID3? = null
    private var artist: ArtistID3? = null

    init {
        songRepository = SongRepository()
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
    }

    fun getSong(): Child? {
        return song
    }

    val liveSong: LiveData<Child?>
        get() = songRepository.getSong(song!!.id)

    fun setSong(song: Child?) {
        this.song = song
        this.album = null
        this.artist = null
    }

    fun getAlbum(): AlbumID3? {
        return album
    }

    val liveAlbum: LiveData<AlbumID3?>
        get() = albumRepository.getAlbum(album!!.id)

    fun setAlbum(album: AlbumID3?) {
        this.song = null
        this.album = album
        this.artist = null
    }

    fun getArtist(): ArtistID3? {
        return artist
    }

    val liveArtist: LiveData<ArtistID3?>
        get() = artistRepository.getArtist(artist!!.id)

    fun setArtist(artist: ArtistID3?) {
        this.song = null
        this.album = null
        this.artist = artist
    }

    fun rate(star: Int) {
        if (song != null) {
            songRepository.setRating(song!!.id, star)
        } else if (album != null) {
            albumRepository.setRating(album!!.id, star)
        } else if (artist != null) {
            artistRepository.setRating(artist!!.id, star)
        }
    }
}
