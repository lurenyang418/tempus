package com.cappielloantonio.tempo.repository

import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.database.AppDatabase.Companion.instance
import com.cappielloantonio.tempo.database.dao.SessionMediaItemDao
import com.cappielloantonio.tempo.model.Chronology
import com.cappielloantonio.tempo.model.SessionMediaItem
import com.cappielloantonio.tempo.provider.AlbumArtContentProvider.Companion.contentUri
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.AlbumID3
import com.cappielloantonio.tempo.subsonic.models.ArtistID3
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Genre
import com.cappielloantonio.tempo.subsonic.models.Index
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.subsonic.models.MusicFolder
import com.cappielloantonio.tempo.subsonic.models.Playlist
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.util.MappingUtil
import com.cappielloantonio.tempo.util.MusicUtil
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.collections.sort

class AutomotiveRepository {
    private val sessionMediaItemDao = instance!!.sessionMediaItemDao()
    private val chronologyDao = instance!!.chronologyDao()

    fun getAlbums(
        prefix: String?,
        type: String?,
        size: Int
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .getAlbumList2(type, size, 0, null, null)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.albumList2 != null && response.body()!!.subsonicResponse.albumList2!!.albums != null) {
                        val albums: List<AlbumID3>? =
                            response.body()!!.subsonicResponse.albumList2!!.albums

                        // add by MFO
                        // Hack for artist view
                        if ("alphabeticalByArtist" == type) for (album in albums!!) {
                            val artistName = album.artist
                            val albumName = album.name
                            album.name = artistName
                            album.artist = albumName
                        }

                        // end add by MFO
                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (album in albums!!) {
                            val artworkUri = contentUri(album.coverArtId)

                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(album.name)
                                .setAlbumTitle(album.name)
                                .setArtist(album.artist)
                                .setGenre(album.genre)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                .setArtworkUri(artworkUri)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(prefix + album.id)
                                .setMediaMetadata(mediaMetadata)
                                .setUri("")
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    val starredSongs: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>
        get() {
            val listenableFuture =
                SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

            getSubsonicClientInstance(false)
                .albumSongListClient!!
                .starred2
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.starred2 != null && response.body()!!.subsonicResponse.starred2!!.songs != null) {
                            val songs: List<Child>? =
                                response.body()!!.subsonicResponse.starred2!!.songs

                            setChildrenMetadata(ArrayList(songs!!))

                            val mediaItems =
                                MappingUtil.mapMediaItems(songs!!.toMutableList() as MutableList<Child?>)

                            val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                                LibraryResult.ofItemList(
                                    ImmutableList.copyOf(
                                        mediaItems.filterNotNull()
                                    ), null
                                )

                            listenableFuture.set(libraryResult)
                        } else {
                            listenableFuture.set(
                                LibraryResult.ofError<ImmutableList<MediaItem>>(
                                    LibraryResult.RESULT_ERROR_BAD_VALUE
                                )
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<ApiResponse?>,
                        t: Throwable
                    ) {
                        listenableFuture.setException(t)
                    }
                })

            return listenableFuture
        }

    fun getRandomSongs(count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .getRandomSongs(100, null, null)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.randomSongs != null && response.body()!!.subsonicResponse.randomSongs!!.songs != null) {
                        val songs: List<Child>? =
                            response.body()!!.subsonicResponse.randomSongs!!.songs

                        setChildrenMetadata(ArrayList(songs!!))

                        val mediaItems = MappingUtil.mapMediaItems(songs!!.toMutableList() as MutableList<Child?>)

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getRecentlyPlayedSongs(
        server: String?,
        count: Int
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        chronologyDao!!.getLastPlayed(server, count)!!
            .observeForever(object : Observer<MutableList<Chronology?>?> {
                override fun onChanged(chronology: MutableList<Chronology?>?) {
                    if (chronology != null && !chronology.isEmpty()) {
                        val songs: List<Child> = chronology.filterNotNull()

                        setChildrenMetadata(ArrayList(songs))

                        val mediaItems = MappingUtil.mapMediaItems(songs.toMutableList() as MutableList<Child?>)

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }

                    chronologyDao.getLastPlayed(server, count)!!.removeObserver(this)
                }
            })

        return listenableFuture
    }

    fun getStarredAlbums(prefix: String?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .starred2
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.starred2 != null && response.body()!!.subsonicResponse.starred2!!.albums != null) {
                        val albums: List<AlbumID3>? =
                            response.body()!!.subsonicResponse.starred2!!.albums

                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (album in albums!!) {
                            val artworkUri = contentUri(album.coverArtId)

                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(album.name)
                                .setArtist(album.artist)
                                .setGenre(album.genre)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                .setArtworkUri(artworkUri)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(prefix + album.id)
                                .setMediaMetadata(mediaMetadata)
                                .setUri("")
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return listenableFuture
    }

    fun getStarredArtists(prefix: String?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .albumSongListClient!!
            .starred2
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.starred2 != null && response.body()!!.subsonicResponse.starred2!!.artists != null) {
                        val artists: List<ArtistID3>? =
                            response.body()!!.subsonicResponse.starred2!!.artists

                        val shuffledArtists = ArrayList(artists!!)
                        Collections.shuffle(shuffledArtists)

                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (artist in shuffledArtists) {
                            val artworkUri = contentUri(artist.coverArtId)

                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(artist.name)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                .setArtworkUri(artworkUri)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(prefix + artist.id)
                                .setMediaMetadata(mediaMetadata)
                                .setUri("")
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getMusicFolders(prefix: String?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .musicFolders
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.musicFolders != null && response.body()!!.subsonicResponse.musicFolders!!.musicFolders != null) {
                        val musicFolders: List<MusicFolder>? =
                            response.body()!!.subsonicResponse.musicFolders!!.musicFolders

                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (musicFolder in musicFolders!!) {
                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(musicFolder.name)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(prefix + musicFolder.id)
                                .setMediaMetadata(mediaMetadata)
                                .setUri("")
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getIndexes(
        prefix: String?,
        id: String?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getIndexes(id, null)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.indexes != null) {
                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        if (response.body()!!.subsonicResponse.indexes!!.indices != null) {
                            val indices: List<Index>? =
                                response.body()!!.subsonicResponse.indexes!!.indices

                            for (index in indices!!) {
                                if (index.artists != null) {
                                    for (artist in index.artists) {
                                        val mediaMetadata = MediaMetadata.Builder()
                                            .setTitle(artist.name)
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                            .build()

                                        val mediaItem = MediaItem.Builder()
                                            .setMediaId(prefix + artist.id)
                                            .setMediaMetadata(mediaMetadata)
                                            .setUri("")
                                            .build()

                                        mediaItems.add(mediaItem)
                                    }
                                }
                            }
                        }

                        if (response.body()!!.subsonicResponse.indexes!!.children != null) {
                            val children: List<Child>? =
                                response.body()!!.subsonicResponse.indexes!!.children

                            for (song in children!!) {
                                val artworkUri = contentUri(song.coverArtId)

                                val mediaMetadata = MediaMetadata.Builder()
                                    .setTitle(song.title)
                                    .setAlbumTitle(song.album)
                                    .setArtist(song.artist)
                                    .setIsBrowsable(false)
                                    .setIsPlayable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                    .setArtworkUri(artworkUri)
                                    .build()

                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(prefix + song.id)
                                    .setMediaMetadata(mediaMetadata)
                                    .setUri(MusicUtil.getStreamUri(song.id))
                                    .build()

                                mediaItems.add(mediaItem)
                            }

                            setChildrenMetadata(ArrayList(children))
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getDirectories(
        prefix: String?,
        id: String?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getMusicDirectory(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.directory != null && response.body()!!.subsonicResponse.directory!!.children != null) {
                        val directory = response.body()!!.subsonicResponse.directory

                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (child in directory!!.children!!) {
                            val artworkUri = contentUri(child.coverArtId)

                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(child.title)
                                .setIsBrowsable(child.isDir)
                                .setIsPlayable(!child.isDir)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                .setArtworkUri(artworkUri)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(if (child.isDir) prefix + child.id else child.id)
                                .setMediaMetadata(mediaMetadata)
                                .setUri(
                                    if (!child.isDir) MusicUtil.getStreamUri(child.id) else Uri.parse(
                                        ""
                                    )
                                )
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        setChildrenMetadata(
                            ArrayList(directory.children!!.stream()
                                .filter { child: Child? -> !child!!.isDir }.collect(
                                    Collectors.toList()
                                ))
                        )

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getPlaylists(prefix: String?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .playlistClient!!
            .playlists
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.playlists != null && response.body()!!.subsonicResponse.playlists!!.playlists != null) {
                        val playlists: List<Playlist>? =
                            response.body()!!.subsonicResponse.playlists!!.playlists

                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (playlist in playlists!!) {
                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(playlist.name)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(prefix + playlist.id)
                                .setMediaMetadata(mediaMetadata)
                                .setUri("")
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getNewestPodcastEpisodes(count: Int): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .podcastClient!!
            .getNewestPodcasts(count)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.newestPodcasts != null && response.body()!!.subsonicResponse.newestPodcasts!!.episodes != null) {
                        val episodes: List<PodcastEpisode>? =
                            response.body()!!.subsonicResponse.newestPodcasts!!.episodes

                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (episode in episodes!!) {
                            val artworkUri = contentUri(episode.coverArtId)

                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(episode.title)
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                                .setArtworkUri(artworkUri)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(episode.id!!)
                                .setMediaMetadata(mediaMetadata)
                                .setUri(MusicUtil.getStreamUri(episode.streamId))
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        setPodcastEpisodesMetadata(ArrayList(episodes))

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    val internetRadioStations: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>
        get() {
            val listenableFuture =
                SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

            getSubsonicClientInstance(false)
                .internetRadioClient!!
                .internetRadioStations
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.internetRadioStations != null && response.body()!!.subsonicResponse.internetRadioStations!!.internetRadioStations != null) {
                            val radioStations: List<InternetRadioStation>? =
                                response.body()!!.subsonicResponse.internetRadioStations!!.internetRadioStations

                            val mediaItems: MutableList<MediaItem?> =
                                ArrayList<MediaItem?>()

                            for (radioStation in radioStations!!) {
                                mediaItems.add(MappingUtil.mapInternetRadioStation(radioStation))
                            }

                            setInternetRadioStationsMetadata(ArrayList(radioStations))

                            val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                                LibraryResult.ofItemList(
                                    ImmutableList.copyOf(
                                        mediaItems.filterNotNull()
                                    ), null
                                )

                            listenableFuture.set(libraryResult)
                        } else {
                            listenableFuture.set(
                                LibraryResult.ofError<ImmutableList<MediaItem>>(
                                    LibraryResult.RESULT_ERROR_BAD_VALUE
                                )
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<ApiResponse?>,
                        t: Throwable
                    ) {
                        listenableFuture.setException(t)
                    }
                })

            return listenableFuture
        }

    fun getAlbumTracks(id: String?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getAlbum(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.album != null && response.body()!!.subsonicResponse.album!!.songs != null) {
                        val tracks: List<Child>? =
                            response.body()!!.subsonicResponse.album!!.songs

                        setChildrenMetadata(ArrayList(tracks!!))

                        val mediaItems = MappingUtil.mapMediaItems(tracks!!.toMutableList())

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getArtistAlbum(
        prefix: String?,
        id: String?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getArtist(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.artist != null && response.body()!!.subsonicResponse.artist!!.albums != null) {
                        val albums: List<AlbumID3>? =
                            response.body()!!.subsonicResponse.artist!!.albums

                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (album in albums!!) {
                            val artworkUri = contentUri(album.coverArtId)

                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(album.name)
                                .setAlbumTitle(album.name)
                                .setArtist(album.artist)
                                .setGenre(album.genre)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                .setArtworkUri(artworkUri)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(prefix + album.id)
                                .setMediaMetadata(mediaMetadata)
                                .setUri("")
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getPlaylistSongs(id: String?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .playlistClient!!
            .getPlaylist(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.playlist != null && response.body()!!.subsonicResponse.playlist!!.entries != null) {
                        val tracks: List<Child>? =
                            response.body()!!.subsonicResponse.playlist!!.entries

                        setChildrenMetadata(ArrayList(tracks!!))

                        val mediaItems = MappingUtil.mapMediaItems(tracks!!.toMutableList())

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getMadeForYou(
        id: String?,
        count: Int
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .getSimilarSongs2(id, count)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.similarSongs2 != null && response.body()!!.subsonicResponse.similarSongs2!!.songs != null) {
                        val tracks: List<Child>? =
                            response.body()!!.subsonicResponse.similarSongs2!!.songs

                        setChildrenMetadata(ArrayList(tracks!!))

                        val mediaItems = MappingUtil.mapMediaItems(tracks!!.toMutableList())

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun search(
        query: String?,
        albumPrefix: String?,
        artistPrefix: String?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .searchingClient!!
            .search3(query, 20, 0, 20, 0, 20, 0)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.searchResult3 != null) {
                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        if (response.body()!!.subsonicResponse.searchResult3!!.artists != null) {
                            for (artist in response.body()!!.subsonicResponse.searchResult3!!.artists!!) {
                                val artworkUri = contentUri(artist.coverArtId)

                                val mediaMetadata = MediaMetadata.Builder()
                                    .setTitle(artist.name)
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                    .setArtworkUri(artworkUri)
                                    .build()

                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(artistPrefix + artist.id)
                                    .setMediaMetadata(mediaMetadata)
                                    .setUri("")
                                    .build()

                                mediaItems.add(mediaItem)
                            }
                        }

                        if (response.body()!!.subsonicResponse.searchResult3!!.albums != null) {
                            for (album in response.body()!!.subsonicResponse.searchResult3!!.albums!!) {
                                val artworkUri = contentUri(album.coverArtId)

                                val mediaMetadata = MediaMetadata.Builder()
                                    .setTitle(album.name)
                                    .setAlbumTitle(album.name)
                                    .setArtist(album.artist)
                                    .setGenre(album.genre)
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                    .setArtworkUri(artworkUri)
                                    .build()

                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(albumPrefix + album.id)
                                    .setMediaMetadata(mediaMetadata)
                                    .setUri("")
                                    .build()

                                mediaItems.add(mediaItem)
                            }
                        }

                        if (response.body()!!.subsonicResponse.searchResult3!!.songs != null) {
                            val tracks: List<Child>? =
                                response.body()!!.subsonicResponse.searchResult3!!.songs
                            setChildrenMetadata(ArrayList(tracks!!))
                            mediaItems.addAll(MappingUtil.mapMediaItems(tracks!!.toMutableList() as MutableList<Child?>))
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    @OptIn(UnstableApi::class)
    fun setChildrenMetadata(children: MutableList<Child>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = ArrayList<SessionMediaItem?>()

        for (child in children) {
            val sessionMediaItem = SessionMediaItem(child)
            sessionMediaItem.timestamp = timestamp
            sessionMediaItems.add(sessionMediaItem)
        }

        val insertAll =
            AutomotiveRepository.InsertAllThreadSafe(sessionMediaItemDao!!, sessionMediaItems)
        val thread = Thread(insertAll)
        thread.start()
    }

    @OptIn(UnstableApi::class)
    fun setPodcastEpisodesMetadata(podcastEpisodes: MutableList<PodcastEpisode>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = ArrayList<SessionMediaItem?>()

        for (podcastEpisode in podcastEpisodes) {
            val sessionMediaItem = SessionMediaItem(podcastEpisode)
            sessionMediaItem.timestamp = timestamp
            sessionMediaItems.add(sessionMediaItem)
        }

        val insertAll =
            AutomotiveRepository.InsertAllThreadSafe(sessionMediaItemDao!!, sessionMediaItems)
        val thread = Thread(insertAll)
        thread.start()
    }

    @OptIn(UnstableApi::class)
    fun setInternetRadioStationsMetadata(internetRadioStations: MutableList<InternetRadioStation>) {
        val timestamp = System.currentTimeMillis()
        val sessionMediaItems = ArrayList<SessionMediaItem?>()

        for (internetRadioStation in internetRadioStations) {
            val sessionMediaItem = SessionMediaItem(internetRadioStation)
            sessionMediaItem.timestamp = timestamp
            sessionMediaItems.add(sessionMediaItem)
        }

        val insertAll =
            AutomotiveRepository.InsertAllThreadSafe(sessionMediaItemDao!!, sessionMediaItems)
        val thread = Thread(insertAll)
        thread.start()
    }

    fun getSessionMediaItem(id: String?): SessionMediaItem? {
        var sessionMediaItem: SessionMediaItem? = null

        val getMediaItemThreadSafe = GetMediaItemThreadSafe(sessionMediaItemDao!!, id)
        val thread = Thread(getMediaItemThreadSafe)
        thread.start()

        try {
            thread.join()
            sessionMediaItem = getMediaItemThreadSafe.sessionMediaItem
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return sessionMediaItem
    }

    fun getMetadatas(timestamp: Long): MutableList<MediaItem?> {
        var mediaItems = mutableListOf<MediaItem?>()

        val getMediaItemsThreadSafe = GetMediaItemsThreadSafe(sessionMediaItemDao!!, timestamp)
        val thread = Thread(getMediaItemsThreadSafe)
        thread.start()

        try {
            thread.join()
            mediaItems = getMediaItemsThreadSafe.mediaItems
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return mediaItems
    }

    fun deleteMetadata() {
        val delete = AutomotiveRepository.DeleteAllThreadSafe(sessionMediaItemDao!!)
        val thread = Thread(delete)
        thread.start()
    }

    fun getGenres(prefix: String?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        getSubsonicClientInstance(false)
            .browsingClient!!
            .genres
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.genres != null && response.body()!!.subsonicResponse.genres!!.genres != null) {
                        val genres: List<Genre>? =
                            response.body()!!.subsonicResponse.genres!!.genres

                        // Sort genres alphabetically by name
                        val sortedGenres = ArrayList(genres!!)
                        sortedGenres.sortWith(Comparator { g1: Genre?, g2: Genre? ->
                            val name1 = if (g1!!.genre != null) g1.genre else ""
                            val name2 = if (g2!!.genre != null) g2.genre else ""
                            name1!!.compareTo(name2!!, ignoreCase = true)
                        })

                        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

                        for (genre in sortedGenres) {
                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(genre.genre)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                .build()

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(prefix + genre.genre)
                                .setMediaMetadata(mediaMetadata)
                                .setUri("")
                                .build()

                            mediaItems.add(mediaItem)
                        }

                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )

                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    listenableFuture.setException(t)
                }
            })

        return listenableFuture
    }

    fun getSongsByGenre(
        genre: String?,
        count: Int,
        shuffle: Boolean
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val listenableFuture = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        val call: Call<ApiResponse?>?
        if (shuffle) {
            call = getSubsonicClientInstance(false)
                .albumSongListClient!!
                .getRandomSongs(count, null, null, genre)
        } else {
            call = getSubsonicClientInstance(false)
                .albumSongListClient!!
                .getSongsByGenre(genre, count, 0)
        }

        call?.enqueue(object : Callback<ApiResponse?> {
            override fun onResponse(call: Call<ApiResponse?>, response: Response<ApiResponse?>) {
                if (response.isSuccessful() && response.body() != null) {
                    val songs: List<Child>?
                    if (shuffle) {
                        songs = if (response.body()!!.subsonicResponse.randomSongs != null)
                            response.body()!!.subsonicResponse.randomSongs!!.songs
                        else
                            null
                    } else {
                        songs = if (response.body()!!.subsonicResponse.songsByGenre != null)
                            response.body()!!.subsonicResponse.songsByGenre!!.songs
                        else
                            null
                    }

                    if (songs != null) {
                        setChildrenMetadata(ArrayList(songs))
                        val mediaItems = MappingUtil.mapMediaItems(songs.toMutableList() as MutableList<Child?>)
                        val libraryResult: LibraryResult<ImmutableList<MediaItem>> =
                            LibraryResult.ofItemList(
                                ImmutableList.copyOf(mediaItems.filterNotNull()),
                                null
                            )
                        listenableFuture.set(libraryResult)
                    } else {
                        listenableFuture.set(
                            LibraryResult.ofError<ImmutableList<MediaItem>>(
                                LibraryResult.RESULT_ERROR_BAD_VALUE
                            )
                        )
                    }
                } else {
                    listenableFuture.set(
                        LibraryResult.ofError<ImmutableList<MediaItem>>(
                            LibraryResult.RESULT_ERROR_BAD_VALUE
                        )
                    )
                }
            }

            override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                listenableFuture.setException(t)
            }
        })

        return listenableFuture
    }

    fun getSongsByGenre(
        genre: String?,
        count: Int
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return getSongsByGenre(genre, count, false)
    }

    private class GetMediaItemThreadSafe(
        private val sessionMediaItemDao: SessionMediaItemDao,
        private val id: String?
    ) : Runnable {
        var sessionMediaItem: SessionMediaItem? = null
            private set

        override fun run() {
            sessionMediaItem = sessionMediaItemDao.get(id)
        }
    }

    @OptIn(UnstableApi::class)
    private class GetMediaItemsThreadSafe(
        private val sessionMediaItemDao: SessionMediaItemDao,
        private val timestamp: Long
    ) : Runnable {
        val mediaItems: MutableList<MediaItem?> = ArrayList<MediaItem?>()

        override fun run() {
            val sessionMediaItems = sessionMediaItemDao.get(timestamp)
            sessionMediaItems!!.forEach(Consumer { sessionMediaItem: SessionMediaItem? ->
                mediaItems.add(
                    sessionMediaItem!!.getMediaItem()
                )
            })
        }
    }

    private class InsertAllThreadSafe(
        private val sessionMediaItemDao: SessionMediaItemDao,
        private val sessionMediaItems: MutableList<SessionMediaItem?>?
    ) : Runnable {
        override fun run() {
            sessionMediaItemDao.insertAll(sessionMediaItems)
        }
    }

    private class DeleteAllThreadSafe(private val sessionMediaItemDao: SessionMediaItemDao) :
        Runnable {
        override fun run() {
            sessionMediaItemDao.deleteAll()
        }
    }
}
