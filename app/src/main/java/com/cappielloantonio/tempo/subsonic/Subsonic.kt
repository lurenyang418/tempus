package com.cappielloantonio.tempo.subsonic

import com.cappielloantonio.tempo.subsonic.api.albumsonglist.AlbumSongListClient
import com.cappielloantonio.tempo.subsonic.api.bookmarks.BookmarksClient
import com.cappielloantonio.tempo.subsonic.api.browsing.BrowsingClient
import com.cappielloantonio.tempo.subsonic.api.mediaannotation.MediaAnnotationClient
import com.cappielloantonio.tempo.subsonic.api.medialibraryscanning.MediaLibraryScanningClient
import com.cappielloantonio.tempo.subsonic.api.mediaretrieval.MediaRetrievalClient
import com.cappielloantonio.tempo.subsonic.api.open.OpenClient
import com.cappielloantonio.tempo.subsonic.api.playlist.PlaylistClient
import com.cappielloantonio.tempo.subsonic.api.podcast.PodcastClient
import com.cappielloantonio.tempo.subsonic.api.searching.SearchingClient
import com.cappielloantonio.tempo.subsonic.api.sharing.SharingClient
import com.cappielloantonio.tempo.subsonic.api.system.SystemClient
import com.cappielloantonio.tempo.subsonic.base.Version

class Subsonic(private val preferences: SubsonicPreferences) {
    val apiVersion: Version? = API_MAX_VERSION

    var systemClient: SystemClient? = null
        get() {
            if (field == null) {
                field = SystemClient(this)
            }
            return field
        }
        private set
    var browsingClient: BrowsingClient? = null
        get() {
            if (field == null) {
                field = BrowsingClient(this)
            }
            return field
        }
        private set
    var mediaRetrievalClient: MediaRetrievalClient? = null
        get() {
            if (field == null) {
                field = MediaRetrievalClient(this)
            }
            return field
        }
        private set
    var playlistClient: PlaylistClient? = null
        get() {
            if (field == null) {
                field = PlaylistClient(this)
            }
            return field
        }
        private set
    var searchingClient: SearchingClient? = null
        get() {
            if (field == null) {
                field = SearchingClient(this)
            }
            return field
        }
        private set
    var albumSongListClient: AlbumSongListClient? = null
        get() {
            if (field == null) {
                field = AlbumSongListClient(this)
            }
            return field
        }
        private set
    var mediaAnnotationClient: MediaAnnotationClient? = null
        get() {
            if (field == null) {
                field = MediaAnnotationClient(this)
            }
            return field
        }
        private set
    var podcastClient: PodcastClient? = null
        get() {
            if (field == null) {
                field = PodcastClient(this)
            }
            return field
        }
        private set
    var mediaLibraryScanningClient: MediaLibraryScanningClient? = null
        get() {
            if (field == null) {
                field = MediaLibraryScanningClient(this)
            }
            return field
        }
        private set
    var bookmarksClient: BookmarksClient? = null
        get() {
            if (field == null) {
                field = BookmarksClient(this)
            }
            return field
        }
        private set
    var sharingClient: SharingClient? = null
        get() {
            if (field == null) {
                field = SharingClient(this)
            }
            return field
        }
        private set
    var openClient: OpenClient? = null
        get() {
            if (field == null) {
                field = OpenClient(this)
            }
            return field
        }
        private set

    val url: String
        get() {
            val url = preferences.serverUrl + "/rest/"
            return url.replace("//rest", "/rest")
        }

    val params: MutableMap<String?, String?>
        get() {
            val params: MutableMap<String?, String?> =
                HashMap<String?, String?>()
            params.put("u", preferences.username)

            if (preferences.authentication?.password != null) params.put(
                "p",
                preferences.authentication?.password
            )
            if (preferences.authentication?.salt != null) params.put(
                "s",
                preferences.authentication?.salt
            )
            if (preferences.authentication?.token != null) params.put(
                "t",
                preferences.authentication?.token
            )

            params.put("v", this.apiVersion!!.versionString)
            params.put("c", preferences.clientName)
            params.put("f", "json")

            return params
        }

    companion object {
        private val API_MAX_VERSION: Version = Version.of("1.15.0")
    }
}
