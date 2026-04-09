package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.DirectoryRepository
import com.cappielloantonio.tempo.repository.GenreRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.subsonic.models.Indexes
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.subsonic.models.Playlist

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val directoryRepository: DirectoryRepository
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val genreRepository: GenreRepository
    private val playlistRepository: PlaylistRepository

    private val musicFolders = MutableLiveData<MutableList<MusicFolder?>?>(null)
    private val indexes = MutableLiveData<Indexes?>(null)
    private val playlistSample = MutableLiveData<MutableList<Playlist?>?>(null)
    private val sampleAlbum = MutableLiveData<MutableList<AlbumID3?>?>(null)
    private val sampleArtist = MutableLiveData<MutableList<ArtistID3?>?>(null)
    private val sampleGenres = MutableLiveData<MutableList<Genre?>?>(null)

    init {
        directoryRepository = DirectoryRepository()
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        genreRepository = GenreRepository()
        playlistRepository = PlaylistRepository()
    }

    fun getMusicFolders(owner: LifecycleOwner): LiveData<MutableList<MusicFolder?>?> {
        if (musicFolders.getValue() == null) {
            directoryRepository.musicFolders.observe(
                owner,
                Observer { value: MutableList<MusicFolder?>? -> musicFolders.postValue(value) })
        }

        return musicFolders
    }

    fun getIndexes(owner: LifecycleOwner): LiveData<Indexes?> {
        if (indexes.getValue() == null) {
            directoryRepository.getIndexes("0", null)
                .observe(owner, Observer { value: Indexes? -> indexes.postValue(value) })
        }

        return indexes
    }

    fun getAlbumSample(owner: LifecycleOwner): LiveData<MutableList<AlbumID3?>?> {
        if (sampleAlbum.getValue() == null) {
            albumRepository.getAlbums("random", 10, null, null).observe(
                owner,
                Observer { value: MutableList<AlbumID3?>? -> sampleAlbum.postValue(value) })
        }

        return sampleAlbum
    }

    fun getArtistSample(owner: LifecycleOwner): LiveData<MutableList<ArtistID3?>?> {
        if (sampleArtist.getValue() == null) {
            artistRepository.getArtists(true, 10).observe(
                owner,
                Observer { value: MutableList<ArtistID3?>? -> sampleArtist.postValue(value) })
        }

        return sampleArtist
    }

    fun getGenreSample(owner: LifecycleOwner): LiveData<MutableList<Genre?>?> {
        if (sampleGenres.getValue() == null) {
            genreRepository.getGenres(true, 15).observe(
                owner,
                Observer { value: MutableList<Genre?>? -> sampleGenres.postValue(value) })
        }

        return sampleGenres
    }

    fun getPlaylistSample(owner: LifecycleOwner): LiveData<MutableList<Playlist?>?> {
        if (playlistSample.getValue() == null) {
            playlistRepository.getPlaylists(true, 10).observe(
                owner,
                Observer { value: MutableList<Playlist?>? -> playlistSample.postValue(value) })
        }

        return playlistSample
    }

    fun refreshAlbumSample(owner: LifecycleOwner) {
        albumRepository.getAlbums("random", 10, null, null).observe(
            owner,
            Observer { value: MutableList<AlbumID3?>? -> sampleAlbum.postValue(value) })
    }

    fun refreshArtistSample(owner: LifecycleOwner) {
        artistRepository.getArtists(true, 10).observe(
            owner,
            Observer { value: MutableList<ArtistID3?>? -> sampleArtist.postValue(value) })
    }

    fun refreshGenreSample(owner: LifecycleOwner) {
        genreRepository.getGenres(true, 15).observe(
            owner,
            Observer { value: MutableList<Genre?>? -> sampleGenres.postValue(value) })
    }

    fun refreshPlaylistSample(owner: LifecycleOwner) {
        playlistRepository.getPlaylists(true, 10).observe(
            owner,
            Observer { value: MutableList<Playlist?>? -> playlistSample.postValue(value) })
    }

    companion object {
        private const val TAG = "LibraryViewModel"
    }
}
