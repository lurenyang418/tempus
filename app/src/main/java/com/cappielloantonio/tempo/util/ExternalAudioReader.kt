package com.cappielloantonio.tempo.util

import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cappielloantonio.tempo.App.Companion.getContext
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.subsonic.models.PodcastEpisode
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.Volatile

object ExternalAudioReader {
    private val cache: MutableMap<String?, DocumentFile?> =
        ConcurrentHashMap<String?, DocumentFile?>()
    private val LOCK = Any()
    private val REFRESH_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
    private val refreshEvents = MutableLiveData<Long?>()

    @Volatile
    private var cachedDirUri: String? = null

    @Volatile
    private var refreshInProgress = false

    @Volatile
    private var refreshQueued = false

    private fun sanitizeFileName(name: String): String {
        var sanitized = name.replace("[\\/:*?\\\"<>|]".toRegex(), "_")
        sanitized = sanitized.replace("\\s+".toRegex(), " ").trim { it <= ' ' }
        return sanitized
    }

    private fun normalizeForComparison(name: String): String {
        var s = sanitizeFileName(name)
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
        s = s.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return s.lowercase()
    }

    private fun ensureCache() {
        val uriString = getDownloadDirectoryUri()
        if (uriString == null) {
            synchronized(LOCK) {
                cache.clear()
                cachedDirUri = null
            }
            ExternalDownloadMetadataStore.clear()
            return
        }

        if (uriString == cachedDirUri) {
            return
        }

        var runSynchronously = false
        synchronized(LOCK) {
            if (refreshInProgress) {
                return
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                scheduleRefreshLocked()
                return
            }

            refreshInProgress = true
            runSynchronously = true
        }

        if (runSynchronously) {
            try {
                rebuildCache()
            } finally {
                onRefreshFinished()
            }
        }
    }

    @JvmStatic
    fun refreshCache() {
        refreshCacheAsync()
    }

    fun refreshCacheAsync() {
        synchronized(LOCK) {
            cachedDirUri = null
            cache.clear()
        }
        requestRefresh()
    }

    fun getRefreshEvents(): LiveData<Long?> {
        return refreshEvents
    }

    private fun buildKey(artist: String?, title: String?, album: String?): String {
        var name: String =
            (if (artist != null && !artist.isEmpty()) artist + " - " + title else title)!!
        if (album != null && !album.isEmpty()) name += " (" + album + ")"
        return normalizeForComparison(name)
    }

    private fun findUri(artist: String?, title: String?, album: String?): Uri? {
        ensureCache()
        if (cachedDirUri == null) return null

        val file = cache.get(buildKey(artist, title, album))
        return if (file != null && file.exists()) file.getUri() else null
    }

    @JvmStatic
    fun getUri(media: Child): Uri? {
        return findUri(media.artist, media.title, media.album)
    }

    fun getUri(episode: PodcastEpisode): Uri? {
        return findUri(episode.artist, episode.title, episode.album)
    }

    @Synchronized
    fun removeMetadata(media: Child?) {
        if (media == null) {
            return
        }

        val key = buildKey(media.artist, media.title, media.album)
        cache.remove(key)
        ExternalDownloadMetadataStore.remove(key)
    }

    @JvmStatic
    fun delete(media: Child): Boolean {
        ensureCache()
        if (cachedDirUri == null) return false

        val key = buildKey(media.artist, media.title, media.album)
        val file = cache.get(key)
        var deleted = false
        if (file != null && file.exists()) {
            deleted = file.delete()
        }
        if (deleted) {
            cache.remove(key)
            ExternalDownloadMetadataStore.remove(key)
        }
        return deleted
    }

    private fun requestRefresh() {
        synchronized(LOCK) {
            scheduleRefreshLocked()
        }
    }

    private fun scheduleRefreshLocked() {
        if (refreshInProgress) {
            refreshQueued = true
            return
        }

        refreshInProgress = true
        REFRESH_EXECUTOR.execute(Runnable {
            try {
                rebuildCache()
            } finally {
                onRefreshFinished()
            }
        })
    }

    private fun rebuildCache() {
        val uriString = getDownloadDirectoryUri()
        if (uriString == null) {
            synchronized(LOCK) {
                cache.clear()
                cachedDirUri = null
            }
            ExternalDownloadMetadataStore.clear()
            return
        }

        val directory = DocumentFile.fromTreeUri(getContext()!!, Uri.parse(uriString))
        val expectedSizes = ExternalDownloadMetadataStore.snapshot()
        val verifiedKeys: MutableSet<String?> = HashSet<String?>()
        val newEntries: MutableMap<String?, DocumentFile?> = HashMap<String?, DocumentFile?>()

        if (directory != null && directory.canRead()) {
            for (file in directory.listFiles()) {
                if (file == null || file.isDirectory()) continue
                val existing = file.getName()
                if (existing == null) continue

                val base = existing.replaceFirst("\\.[^\\.]+$".toRegex(), "")
                val key = normalizeForComparison(base)
                val expected = expectedSizes.get(key)
                val actualLength = file.length()

                if (expected != null && expected > 0 && actualLength == expected) {
                    newEntries.put(key, file)
                    verifiedKeys.add(key)
                } else {
                    ExternalDownloadMetadataStore.remove(key)
                }
            }
        }

        if (!expectedSizes.isEmpty()) {
            if (verifiedKeys.isEmpty()) {
                ExternalDownloadMetadataStore.clear()
            } else {
                for (key in expectedSizes.keys) {
                    if (!verifiedKeys.contains(key)) {
                        ExternalDownloadMetadataStore.remove(key)
                    }
                }
            }
        }

        synchronized(LOCK) {
            cache.clear()
            cache.putAll(newEntries)
            cachedDirUri = uriString
        }
    }

    private fun onRefreshFinished() {
        val runAgain: Boolean
        synchronized(LOCK) {
            refreshInProgress = false
            runAgain = refreshQueued
            refreshQueued = false
        }

        refreshEvents.postValue(SystemClock.elapsedRealtime())

        if (runAgain) {
            requestRefresh()
        }
    }
}