package com.cappielloantonio.tempo.repository

import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.database.AppDatabase.Companion.instance
import com.cappielloantonio.tempo.database.dao.PlaylistDao
import com.cappielloantonio.tempo.subsonic.base.ApiResponse
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.Playlist
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import kotlin.math.min

class PlaylistRepository {
    val playlistUpdateTrigger: LiveData<Boolean?>
        get() = Companion.playlistUpdateTrigger

    fun notifyPlaylistChanged() {
        Companion.playlistUpdateTrigger.postValue(true)
        refreshAllPlaylists()
    }

    @OptIn(UnstableApi::class)
    private val playlistDao = instance!!.playlistDao()
    fun getAllPlaylists(owner: LifecycleOwner?): LiveData<MutableList<Playlist>?> {
        refreshAllPlaylists()
        return allPlaylistsLiveData
    }

    fun refreshAllPlaylists() {
        getSubsonicClientInstance(false)
            .playlistClient!!
            .playlists
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.playlists != null) {
                        val playlists: List<Playlist>? =
                            response.body()!!.subsonicResponse.playlists!!.playlists
                        allPlaylistsLiveData.postValue(ArrayList(playlists!!))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })
    }

    fun getPlaylists(random: Boolean, size: Int): MutableLiveData<MutableList<Playlist?>?> {
        val listLivePlaylists = MutableLiveData<MutableList<Playlist?>?>(ArrayList<Playlist?>())

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

                        val playlistArrayList = ArrayList(playlists!!)
                        if (random) {
                            Collections.shuffle(playlistArrayList)
                            listLivePlaylists.setValue(
                                ArrayList(playlistArrayList.subList(
                                    0,
                                    min(playlistArrayList.size, size)
                                ))
                            )
                        } else {
                            listLivePlaylists.setValue(playlistArrayList)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return listLivePlaylists
    }

    fun getPlaylistSongs(id: String?): MutableLiveData<MutableList<Child?>?> {
        val listLivePlaylistSongs = MutableLiveData<MutableList<Child?>?>()

        getSubsonicClientInstance(false)
            .playlistClient!!
            .getPlaylist(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.playlist != null) {
                        val songs: List<Child>? =
                            response.body()!!.subsonicResponse.playlist!!.entries
                        listLivePlaylistSongs.setValue(ArrayList(songs!!))
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })

        return listLivePlaylistSongs
    }

    fun getPlaylist(id: String?): MutableLiveData<Playlist?> {
        val playlistLiveData = MutableLiveData<Playlist?>()

        getSubsonicClientInstance(false)
            .playlistClient!!
            .getPlaylist(id)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()
                        && response.body() != null && response.body()!!.subsonicResponse.playlist != null
                    ) {
                        playlistLiveData.setValue(response.body()!!.subsonicResponse.playlist)
                    } else {
                        playlistLiveData.setValue(null)
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    playlistLiveData.setValue(null)
                }
            })

        return playlistLiveData
    }

    interface AddToPlaylistCallback {
        fun onSuccess()
        fun onFailure()
        fun onAllSkipped()
    }

    @JvmOverloads
    fun addSongToPlaylist(
        playlistId: String?,
        songsId: ArrayList<String?>,
        playlistVisibilityIsPublic: Boolean,
        callback: AddToPlaylistCallback? = null
    ) {
        Log.d("PlaylistRepository", "addSongToPlaylist: id=" + playlistId + ", songs=" + songsId)
        if (songsId.isEmpty()) {
            if (callback != null) callback.onAllSkipped()
        } else {
            getSubsonicClientInstance(false)
                .playlistClient!!
                .updatePlaylist(playlistId, null, playlistVisibilityIsPublic, songsId, null)
                ?.enqueue(object : Callback<ApiResponse?> {
                    override fun onResponse(
                        call: Call<ApiResponse?>,
                        response: Response<ApiResponse?>
                    ) {
                        if (response.isSuccessful()) notifyPlaylistChanged()
                        if (callback != null) callback.onSuccess()
                    }

                    override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        if (callback != null) callback.onFailure()
                    }
                })
        }
    }

    fun removeSongFromPlaylist(playlistId: String?, index: Int, callback: AddToPlaylistCallback?) {
        val indexes = ArrayList<Int?>()
        indexes.add(index)
        getSubsonicClientInstance(false)
            .playlistClient!!
            .updatePlaylist(playlistId, null, true, null, indexes)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()) notifyPlaylistChanged()
                    if (callback != null) {
                        if (response.isSuccessful()) callback.onSuccess()
                        else callback.onFailure()
                    }
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                    if (callback != null) callback.onFailure()
                }
            })
    }

    fun createPlaylist(playlistId: String?, name: String?, songsId: ArrayList<String?>?) {
        getSubsonicClientInstance(false)
            .playlistClient!!
            .createPlaylist(playlistId, name, songsId)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()) notifyPlaylistChanged()
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })
    }

    fun updatePlaylist(playlistId: String?, name: String?, songsId: ArrayList<String?>?) {
        getSubsonicClientInstance(false)
            .playlistClient!!
            .updatePlaylist(playlistId, name, true, null, null)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()) {
                        // After renaming, we need to handle the song list update.
                        // Subsonic doesn't have a "replace all songs" in updatePlaylist.
                        // So we might still need to recreate if the songs changed significantly,
                        // but if we just renamed, we should update the local pinned database.
                        updateLocalPinnedPlaylistName(playlistId, name)
                        notifyPlaylistChanged()
                    }

                    // If songsId is provided, we might want to re-sync them.
                    // For now, let's at least fix the name duplication issue.
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })
    }

    @OptIn(UnstableApi::class)
    private fun updateLocalPinnedPlaylistName(id: String?, newName: String?) {
        Thread(Runnable {
            val pinned: List<Playlist> = playlistDao!!.allSync?.filterNotNull() ?: emptyList()
            for (p in pinned) {
                if (p.id == id) {
                    p.name = newName
                    playlistDao.insert(p) // Replace strategy will update it
                    break
                }
            }
        }).start()
    }

    fun deletePlaylist(playlistId: String?) {
        getSubsonicClientInstance(false)
            .playlistClient!!
            .deletePlaylist(playlistId)
            ?.enqueue(object : Callback<ApiResponse?> {
                override fun onResponse(
                    call: Call<ApiResponse?>,
                    response: Response<ApiResponse?>
                ) {
                    if (response.isSuccessful()) notifyPlaylistChanged()
                }

                override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                }
            })
    }

    @OptIn(UnstableApi::class)
    val pinnedPlaylists: LiveData<MutableList<Playlist?>?>?
        get() = playlistDao!!.all

    @OptIn(UnstableApi::class)
    fun insert(playlist: Playlist) {
        val insert = PlaylistRepository.InsertThreadSafe(playlistDao!!, playlist)
        val thread = Thread(insert)
        thread.start()
    }

    @UnstableApi
    fun delete(playlist: Playlist) {
        val delete = PlaylistRepository.DeleteThreadSafe(playlistDao!!, playlist)
        val thread = Thread(delete)
        thread.start()
    }

    @UnstableApi
    fun updatePinnedPlaylists() {
        updatePinnedPlaylists(null)
    }

    @OptIn(UnstableApi::class)
    fun updatePinnedPlaylists(forceIds: MutableList<String?>?) {
        Thread(Runnable {
            val pinned: List<Playlist> = playlistDao!!.allSync?.filterNotNull() ?: emptyList()
            if (pinned.isNotEmpty()) {
                getSubsonicClientInstance(false)
                    .playlistClient!!
                    .playlists
                    ?.enqueue(object : Callback<ApiResponse?> {
                        override fun onResponse(
                            call: Call<ApiResponse?>,
                            response: Response<ApiResponse?>
                        ) {
                            if (response.isSuccessful() && response.body() != null && response.body()!!.subsonicResponse.playlists != null) {
                                val remotes: List<Playlist>? =
                                    response.body()!!.subsonicResponse.playlists!!.playlists
                                Thread(Runnable {
                                    for (p in pinned) {
                                        for (r in remotes!!) {
                                            if (p.id == r.id) {
                                                p.name = r.name
                                                p.songCount = r.songCount
                                                p.duration = r.duration
                                                p.coverArtId = r.coverArtId
                                                playlistDao.insert(p)
                                                break
                                            }
                                        }
                                    }
                                }).start()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                        }
                    })
            }
        }).start()
    }

    private class InsertThreadSafe(
        private val playlistDao: PlaylistDao,
        private val playlist: Playlist
    ) : Runnable {
        override fun run() {
            playlistDao.insert(playlist)
        }
    }

    private class DeleteThreadSafe(
        private val playlistDao: PlaylistDao,
        private val playlist: Playlist
    ) : Runnable {
        override fun run() {
            playlistDao.delete(playlist)
        }
    }

    companion object {
        @JvmField var playlistUpdateTrigger = MutableLiveData<Boolean?>()

        private val allPlaylistsLiveData = MutableLiveData<MutableList<Playlist>?>()
    }
}
