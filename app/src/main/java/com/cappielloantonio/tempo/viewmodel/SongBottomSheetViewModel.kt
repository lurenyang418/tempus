package com.cappielloantonio.tempo.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.interfaces.MediaCallback
import com.cappielloantonio.tempo.interfaces.StarCallback
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.AlbumRepository
import com.cappielloantonio.tempo.repository.ArtistRepository
import com.cappielloantonio.tempo.repository.FavoriteRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository
import com.cappielloantonio.tempo.repository.PlaylistRepository.AddToPlaylistCallback
import com.cappielloantonio.tempo.repository.SharingRepository
import com.cappielloantonio.tempo.repository.SongRepository
import com.cappielloantonio.tempo.repository.SongRepository.MediaCallbackInternal
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Share
import com.cappielloantonio.tempo.util.Constants.SeedType
import com.cappielloantonio.tempo.util.DownloadUtil
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.NetworkUtil
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.isStarredSyncEnabled
import java.util.Date

@UnstableApi
class SongBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val songRepository: SongRepository
    private val albumRepository: AlbumRepository
    private val artistRepository: ArtistRepository
    private val favoriteRepository: FavoriteRepository
    private val sharingRepository: SharingRepository
    private val playlistRepository: PlaylistRepository

    private var song: Child? = null

    private val instantMix = MutableLiveData<MutableList<Child?>?>(null)

    init {
        songRepository = SongRepository()
        albumRepository = AlbumRepository()
        artistRepository = ArtistRepository()
        favoriteRepository = FavoriteRepository()
        sharingRepository = SharingRepository()
        playlistRepository = PlaylistRepository()
    }

    fun getSong(): Child {
        return song!!
    }

    fun setSong(song: Child) {
        this.song = song
    }

    fun removeFromPlaylist(playlistId: String?, index: Int, callback: AddToPlaylistCallback?) {
        playlistRepository.removeSongFromPlaylist(playlistId, index, callback)
    }

    fun setFavorite(context: Context?) {
        if (song!!.starred != null) {
            if (NetworkUtil.isOffline) {
                removeFavoriteOffline(song!!)
            } else {
                removeFavoriteOnline(song!!)
            }
        } else {
            if (NetworkUtil.isOffline) {
                setFavoriteOffline(song!!)
            } else {
                setFavoriteOnline(context, song!!)
            }
        }
    }

    private fun removeFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, false)
        media.starred = null
    }

    private fun removeFavoriteOnline(media: Child) {
        favoriteRepository.unstar(media.id, null, null, object : StarCallback {
            override fun onError() {
                // media.setStarred(new Date());
                favoriteRepository.starLater(media.id, null, null, false)
            }
        })

        media.starred = null
    }

    private fun setFavoriteOffline(media: Child) {
        favoriteRepository.starLater(media.id, null, null, true)
        media.starred = Date()
    }

    private fun setFavoriteOnline(context: Context?, media: Child) {
        favoriteRepository.star(media.id, null, null, object : StarCallback {
            override fun onError() {
                // media.setStarred(null);
                favoriteRepository.starLater(media.id, null, null, true)
            }
        })

        media.starred = Date()

        if (isStarredSyncEnabled() && getDownloadDirectoryUri() == null) {
            DownloadUtil.getDownloadTracker(context).download(
                MappingUtil.mapDownload(media),
                Download(media)
            )
        }
    }

    val album: LiveData<AlbumID3?>
        get() = albumRepository.getAlbum(song!!.albumId)

    val artist: LiveData<ArtistID3?>
        get() = artistRepository.getArtist(song!!.artistId)

    fun getInstantMix(owner: LifecycleOwner, media: Child): LiveData<MutableList<Child?>?> {
        instantMix.setValue(mutableListOf<Child?>())

        songRepository.getInstantMix(media.id, SeedType.TRACK, 30)
            .observe(owner, Observer { value: MutableList<Child?>? -> instantMix.postValue(value) })

        return instantMix
    }

    fun getInstantMix(media: Child, count: Int, callback: MediaCallback) {
        songRepository.getInstantMix(
            media.id,
            SeedType.TRACK,
            count,
            object : SongRepository.MediaCallbackInternal {
                override fun onSongsAvailable(songs: MutableList<Child>?) {
                    if (songs != null && !songs.isEmpty()) {
                        callback.onLoadMedia(songs)
                    } else {
                        callback.onLoadMedia(mutableListOf<Any?>())
                    }
                }
            })
    }

    fun shareTrack(): MutableLiveData<Share?> {
        return sharingRepository.createShare(song!!.id, song!!.title, null)
    }
}
