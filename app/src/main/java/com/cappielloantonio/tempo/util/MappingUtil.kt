package com.cappielloantonio.tempo.util

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.App.Companion.getContext
import com.cappielloantonio.tempo.provider.AlbumArtContentProvider.Companion.contentUri
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.InternetRadioStation
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import com.cappielloantonio.tempo.util.Preferences.preferTranscodedDownload
import com.google.common.collect.ImmutableList
import java.nio.charset.StandardCharsets

@OptIn(markerClass = [UnstableApi::class])
object MappingUtil {
    @JvmStatic
    fun mapMediaItems(items: Iterable<Child?>): java.util.ArrayList<MediaItem?> {
        val mediaItems = java.util.ArrayList<MediaItem?>()

        for (item in items) {
            if (item != null) {
                mediaItems.add(mapMediaItem(item))
            }
        }

        return mediaItems
    }

    private const val TAG = "MappingUtil"

    fun mapMediaItem(media: Child?): MediaItem {
        try {
            val uri = MappingUtil.getUri(media!!)
            val coverArtId = media.coverArtId
            var artworkUri: Uri? = null

            if (coverArtId != null) {
                artworkUri = contentUri(coverArtId)
            }

            val bundle = Bundle()
            bundle.putString("id", media.id)
            bundle.putString("parentId", media.parentId)
            bundle.putBoolean("isDir", media.isDir)

            bundle.putString("title", media.title)
            bundle.putString("album", media.album)
            bundle.putString("artist", media.artist)

            bundle.putInt("track", (if (media.track != null) media.track else 0)!!)
            bundle.putInt("year", (if (media.year != null) media.year else 0)!!)
            bundle.putString("genre", media.genre)
            bundle.putString("coverArtId", coverArtId)
            bundle.putLong("size", (if (media.size != null) media.size else 0)!!)
            bundle.putString("contentType", media.contentType)
            bundle.putString("suffix", media.suffix)
            bundle.putString("transcodedContentType", media.transcodedContentType)
            bundle.putString("transcodedSuffix", media.transcodedSuffix)
            bundle.putInt("duration", (if (media.duration != null) media.duration else 0)!!)
            bundle.putInt("bitrate", (if (media.bitrate != null) media.bitrate else 0)!!)
            bundle.putInt(
                "samplingRate",
                (if (media.samplingRate != null) media.samplingRate else 0)!!
            )
            bundle.putInt("bitDepth", (if (media.bitDepth != null) media.bitDepth else 0)!!)
            bundle.putString("path", media.path)
            bundle.putBoolean("isVideo", media.isVideo)
            bundle.putInt(
                "userRating",
                (if (media.userRating != null) media.userRating else 0)!!
            )
            bundle.putDouble(
                "averageRating",
                (if (media.averageRating != null) media.averageRating else 0.0)!!
            )
            bundle.putLong("playCount", (if (media.playCount != null) media.playCount else 0)!!)
            bundle.putInt(
                "discNumber",
                (if (media.discNumber != null) media.discNumber else 0)!!
            )
            bundle.putLong(
                "created",
                if (media.created != null) media.created!!.getTime() else 0
            )
            bundle.putLong(
                "starred",
                if (media.starred != null) media.starred!!.getTime() else 0
            )
            bundle.putString("albumId", media.albumId)
            bundle.putString("artistId", media.artistId)
            bundle.putString("type", Constants.MEDIA_TYPE_MUSIC)
            bundle.putLong(
                "bookmarkPosition",
                (if (media.bookmarkPosition != null) media.bookmarkPosition else 0)!!
            )
            bundle.putInt(
                "originalWidth",
                (if (media.originalWidth != null) media.originalWidth else 0)!!
            )
            bundle.putInt(
                "originalHeight",
                (if (media.originalHeight != null) media.originalHeight else 0)!!
            )
            bundle.putString("uri", uri.toString())

            bundle.putString(
                "assetLinkSong",
                if (media.id != null) AssetLinkUtil.buildLink(
                    AssetLinkUtil.TYPE_SONG,
                    media.id
                ) else null
            )
            bundle.putString(
                "assetLinkAlbum",
                if (media.albumId != null) AssetLinkUtil.buildLink(
                    AssetLinkUtil.TYPE_ALBUM,
                    media.albumId
                ) else null
            )
            bundle.putString(
                "assetLinkArtist",
                if (media.artistId != null) AssetLinkUtil.buildLink(
                    AssetLinkUtil.TYPE_ARTIST,
                    media.artistId
                ) else null
            )
            bundle.putString(
                "assetLinkGenre",
                AssetLinkUtil.buildLink(AssetLinkUtil.TYPE_GENRE, media.genre)
            )
            val year = media.year
            bundle.putString(
                "assetLinkYear",
                if (year != null && year != 0) AssetLinkUtil.buildLink(
                    AssetLinkUtil.TYPE_YEAR,
                    year.toString()
                ) else null
            )

            return MediaItem.Builder()
                .setMediaId(media.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(media.title)
                        .setTrackNumber(if (media.track != null) media.track else 0)
                        .setDiscNumber(if (media.discNumber != null) media.discNumber else 0)
                        .setReleaseYear(if (media.year != null) media.year else 0)
                        .setAlbumTitle(media.album)
                        .setArtist(media.artist)
                        .setArtworkUri(artworkUri)
                        .setUserRating(HeartRating(media.starred != null))
                        .setSupportedCommands(
                            ImmutableList.of<String?>(
                                Constants.CUSTOM_COMMAND_TOGGLE_HEART_ON,
                                Constants.CUSTOM_COMMAND_TOGGLE_HEART_OFF
                            )
                        )
                        .setExtras(bundle)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
                .setRequestMetadata(
                    RequestMetadata.Builder()
                        .setMediaUri(uri)
                        .setExtras(bundle)
                        .build()
                )
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .setUri(uri)
                .build()
        } catch (e: Exception) {
            val id = if (media != null) media.id else "NULL_MEDIA_OBJECT"
            val title = if (media != null) media.title else "N/A"

            Log.e(
                TAG, "Instant Mix CRASH! Failed to map song to MediaItem. " +
                        "Problematic Song ID: " + id +
                        ", Title: " + title +
                        ". Inspect this song's Subsonic data for missing fields.", e
            )
            throw RuntimeException("Mapping failed for song ID: " + id, e)
        }
    }

    fun mapMediaItem(old: MediaItem): MediaItem {
        var mediaId: String? = null
        if (old.requestMetadata.extras != null) mediaId =
            old.requestMetadata.extras!!.getString("id")

        if (mediaId != null && DownloadUtil.getDownloadTracker(getContext()!!)
                ?.isDownloaded(mediaId) == true
        ) {
            return old
        }
        val uri =
            if (old.requestMetadata.mediaUri == null) null else MusicUtil.updateStreamUri(old.requestMetadata.mediaUri)
        return MediaItem.Builder()
            .setMediaId(old.mediaId)
            .setMediaMetadata(old.mediaMetadata)
            .setRequestMetadata(
                RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(old.requestMetadata.extras)
                    .build()
            )
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(uri)
            .build()
    }

    @JvmStatic
    fun mapDownloads(items: Iterable<Child?>): java.util.ArrayList<MediaItem?> {
        val downloads = java.util.ArrayList<MediaItem?>()

        for (item in items) {
            if (item != null) {
                downloads.add(mapDownload(item))
            }
        }

        return downloads
    }

    @JvmStatic
    fun mapDownload(media: Child): MediaItem {
        val bundle = Bundle()
        bundle.putInt("samplingRate", (if (media.samplingRate != null) media.samplingRate else 0)!!)
        bundle.putInt("bitDepth", (if (media.bitDepth != null) media.bitDepth else 0)!!)

        return MediaItem.Builder()
            .setMediaId(media.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(media.title)
                    .setTrackNumber(if (media.track != null) media.track else 0)
                    .setDiscNumber(if (media.discNumber != null) media.discNumber else 0)
                    .setReleaseYear(if (media.year != null) media.year else 0)
                    .setAlbumTitle(media.album)
                    .setArtist(media.artist)
                    .setExtras(bundle)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .setRequestMetadata(
                RequestMetadata.Builder()
                    .setExtras(bundle)
                    .setMediaUri(
                        if (preferTranscodedDownload()) MusicUtil.getTranscodedDownloadUri(
                            media.id
                        ) else MusicUtil.getDownloadUri(media.id)
                    )
                    .build()
            )
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(
                if (preferTranscodedDownload()) MusicUtil.getTranscodedDownloadUri(media.id) else MusicUtil.getDownloadUri(
                    media.id
                )
            )
            .build()
    }

    fun mapInternetRadioStation(internetRadioStation: InternetRadioStation): MediaItem {
        val uri = Uri.parse(internetRadioStation.streamUrl)
        var artworkUri: Uri? = null
        val homePageUrl = internetRadioStation.homePageUrl
        var coverArtId: String? = null

        if (homePageUrl != null && !homePageUrl.isEmpty() && MusicUtil.isImageUrl(homePageUrl)) {
            val encodedUrl = Base64.encodeToString(
                homePageUrl.toByteArray(StandardCharsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP
            )
            coverArtId = "ir_" + encodedUrl
            artworkUri = contentUri(coverArtId)
        }

        val bundle = Bundle()
        bundle.putString("id", internetRadioStation.id)
        bundle.putString("title", internetRadioStation.name)
        bundle.putString("stationName", internetRadioStation.name)
        bundle.putString("uri", uri.toString())
        bundle.putString("type", Constants.MEDIA_TYPE_RADIO)
        bundle.putString("coverArtId", coverArtId)
        if (homePageUrl != null) {
            bundle.putString("homepageUrl", homePageUrl)
        }

        return MediaItem.Builder()
            .setMediaId(internetRadioStation.id!!)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(internetRadioStation.name)
                    .setArtworkUri(artworkUri)
                    .setExtras(bundle)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .setRequestMetadata(
                RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(bundle)
                    .build()
            ) // .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(uri)
            .build()
    }

    fun mapMediaItem(podcastEpisode: PodcastEpisode): MediaItem {
        val uri = getUri(podcastEpisode)
        val artworkUri = contentUri(podcastEpisode.coverArtId)

        val bundle = Bundle()
        bundle.putString("id", podcastEpisode.id)
        bundle.putString("parentId", podcastEpisode.parentId)
        bundle.putBoolean("isDir", podcastEpisode.isDir)
        bundle.putString("title", podcastEpisode.title)
        bundle.putString("album", podcastEpisode.album)
        bundle.putString("artist", podcastEpisode.artist)
        bundle.putInt("year", (if (podcastEpisode.year != null) podcastEpisode.year else 0)!!)
        bundle.putString("coverArtId", podcastEpisode.coverArtId)
        bundle.putLong("size", (if (podcastEpisode.size != null) podcastEpisode.size else 0)!!)
        bundle.putString("contentType", podcastEpisode.contentType)
        bundle.putString("suffix", podcastEpisode.suffix)
        bundle.putInt(
            "duration",
            (if (podcastEpisode.duration != null) podcastEpisode.duration else 0)!!
        )
        bundle.putInt(
            "bitrate",
            (if (podcastEpisode.bitrate != null) podcastEpisode.bitrate else 0)!!
        )
        bundle.putBoolean("isVideo", podcastEpisode.isVideo)
        bundle.putLong(
            "created",
            if (podcastEpisode.created != null) podcastEpisode.created!!.getTime() else 0
        )
        bundle.putString("artistId", podcastEpisode.artistId)
        bundle.putString("description", podcastEpisode.description)
        bundle.putString("type", Constants.MEDIA_TYPE_PODCAST)
        bundle.putString("uri", uri.toString())

        val item = MediaItem.Builder()
            .setMediaId(podcastEpisode.id!!)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(podcastEpisode.title)
                    .setReleaseYear(if (podcastEpisode.year != null) podcastEpisode.year else 0)
                    .setAlbumTitle(podcastEpisode.album)
                    .setArtist(podcastEpisode.artist)
                    .setArtworkUri(artworkUri)
                    .setExtras(bundle)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .setRequestMetadata(
                RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .setExtras(bundle)
                    .build()
            )
            .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
            .setUri(uri)
            .build()

        return item
    }

    private fun getUri(media: Child): Uri {
        // Check if it's in our local SQL Database
        val repo = DownloadRepository()
        val localDownload = repo.getDownload(media.id)

        if (localDownload != null && localDownload.downloadUri != null && !localDownload.downloadUri!!.isEmpty()) {
            Log.d(TAG, "Playing local file for: " + media.title)
            return Uri.parse(localDownload.downloadUri)
        }

        // Legacy check for external directory, i think this was broken/buggy
        if (getDownloadDirectoryUri() != null) {
            val local = ExternalAudioReader.getUri(media)
            if (local != null) return local
        }

        // Fallback to streaming
        Log.d(TAG, "No local file found. Streaming: " + media.title)
        return MusicUtil.getStreamUri(media.id)!!
    }

    private fun getUri(podcastEpisode: PodcastEpisode): Uri {
        if (getDownloadDirectoryUri() != null) {
            val local = ExternalAudioReader.getUri(podcastEpisode)
            return if (local != null) local else MusicUtil.getStreamUri(podcastEpisode.streamId)!!
        }
        return if (DownloadUtil.getDownloadTracker(getContext()!!)
                ?.isDownloaded(podcastEpisode.streamId) == true
        )
            getDownloadUri(podcastEpisode.streamId)
        else
            MusicUtil.getStreamUri(podcastEpisode.streamId)!!
    }

    private fun getDownloadUri(id: String?): Uri {
        val download = DownloadRepository().getDownload(id)
        return if (download != null && !download.downloadUri!!.isEmpty()) Uri.parse(download.downloadUri) else MusicUtil.getDownloadUri(
            id
        )!!
    }

    @JvmStatic
    fun observeExternalAudioRefresh(owner: LifecycleOwner?, onRefresh: Runnable?) {
        if (owner == null || onRefresh == null) {
            return
        }
        ExternalAudioReader.getRefreshEvents()
            .observe(owner, Observer { event: Long? -> onRefresh.run() })
    }
}