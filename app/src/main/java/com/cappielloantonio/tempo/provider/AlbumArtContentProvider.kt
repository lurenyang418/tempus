package com.cappielloantonio.tempo.provider

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cappielloantonio.tempo.BuildConfig
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.util.Preferences.getImageSize
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AlbumArtContentProvider : ContentProvider() {
    private var executor: ExecutorService? = null

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = getContext()
        val albumId = uri.getLastPathSegment()
        val artworkUri: Uri?

        if (albumId != null && albumId.startsWith("ir_")) {
            val encodedUrl = albumId.substring("ir_".length)
            val decodedUrl = String(Base64.decode(encodedUrl, Base64.URL_SAFE or Base64.NO_WRAP))
            artworkUri = Uri.parse(decodedUrl)
        } else {
            artworkUri = Uri.parse(CustomGlideRequest.createUrl(albumId, getImageSize()))
        }

        try {
            // use pipe to communicate between background thread and caller of openFile()
            val pipe = ParcelFileDescriptor.createPipe()
            val readSide: ParcelFileDescriptor? = pipe[0]
            val writeSide = pipe[1]

            // perform loading in background thread to avoid blocking UI
            executor!!.execute(Runnable {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { out ->

                        // request artwork from API using Glide
                        val file = Glide.with(context!!)
                            .asFile()
                            .load(artworkUri)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .submit()
                            .get()

                        // copy artwork down pipe returned by ContentProvider
                        try {
                            FileInputStream(file).use { `in` ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while ((`in`.read(buffer).also { bytesRead = it }) != -1) {
                                    out.write(buffer, 0, bytesRead)
                                }
                            }
                        } catch (e: Exception) {
                            writeSide.closeWithError("Failed to load image: " + e.message)
                        }
                    }
                } catch (e: Exception) {
                    try {
                        writeSide.closeWithError("Failed to load image: " + e.message)
                    } catch (ignored: IOException) {
                    }
                }
            })

            return readSide
        } catch (e: IOException) {
            throw FileNotFoundException("Could not create pipe: " + e.message)
        }
    }

    override fun onCreate(): Boolean {
        executor = Executors.newFixedThreadPool(
            max(2, Runtime.getRuntime().availableProcessors() / 2)
        )
        return true
    }

    override fun shutdown() {
        if (executor != null) {
            executor!!.shutdown()
            try {
                if (!executor!!.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor!!.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executor!!.shutdownNow()
            }
        }
    }

    override fun query(
        uri: Uri,
        strings: Array<String?>?,
        s: String?,
        strings1: Array<String?>?,
        s1: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return ""
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String?>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        contentValues: ContentValues?,
        s: String?,
        strings: Array<String?>?
    ): Int {
        return 0
    }

    companion object {
        val AUTHORITY: String = BuildConfig.APPLICATION_ID + ".albumart.provider"
        const val ALBUM_ART: String = "albumArt"
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            uriMatcher.addURI(AUTHORITY, "albumArt/*", 1)
        }

        @JvmStatic
        fun contentUri(artworkId: String?): Uri? {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(ALBUM_ART)
                .appendPath(artworkId)
                .build()
        }
    }
}
