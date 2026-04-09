package com.cappielloantonio.tempo.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.repository.DownloadRepository
import com.cappielloantonio.tempo.subsonic.models.Child
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.util.Preferences.getDownloadDirectoryUri
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

object ExternalAudioWriter {
    private val EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
    private const val BUFFER_SIZE = 8192
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 60000

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

    private fun findFile(dir: DocumentFile, fileName: String): DocumentFile? {
        val normalized = normalizeForComparison(fileName)
        for (file in dir.listFiles()) {
            if (file.isDirectory()) continue
            val existing = file.getName()
            if (existing != null && normalizeForComparison(existing) == normalized) {
                return file
            }
        }
        return null
    }

    @JvmStatic
    fun downloadToUserDirectory(context: Context?, child: Child?) {
        if (context == null || child == null) {
            return
        }
        val appContext = context.getApplicationContext()
        val mediaItem = MappingUtil.mapDownload(child)
        val fallbackName = if (child.title != null) child.title else child.id
        EXECUTOR.execute(Runnable { performDownload(appContext, mediaItem, fallbackName, child) })
    }

    private fun performDownload(
        context: Context,
        mediaItem: MediaItem?,
        fallbackName: String?,
        child: Child
    ) {
        val uriString = getDownloadDirectoryUri()
        if (uriString == null) {
            notifyUnavailable(context)
            return
        }

        val directory = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
        if (directory == null || !directory.canWrite()) {
            notifyFailure(context, "Cannot write to folder.")
            return
        }

        val artist = child.artist ?: ""
        val effectiveTitle = child.title ?: fallbackName ?: "download"
        val album = child.album ?: ""
        val titleForFileName = if (artist.isEmpty()) effectiveTitle else "$artist - $effectiveTitle"
        var baseName: String = titleForFileName
        if (album.isNotEmpty()) baseName += " ($album)"
        val metadataKey = normalizeForComparison(baseName)

        val mediaUri = if (mediaItem != null && mediaItem.requestMetadata != null)
            mediaItem.requestMetadata.mediaUri
        else
            null
        if (mediaUri == null) {
            notifyFailure(context, "Invalid media URI.")
            ExternalDownloadMetadataStore.remove(metadataKey)
            return
        }

        val scheme = if (mediaUri.getScheme() != null) mediaUri.getScheme()!!.lowercase() else ""

        var connection: HttpURLConnection? = null
        var sourceDocument: DocumentFile? = null
        var sourceFile: File? = null
        var remoteLength: Long = -1
        var mimeType: String? = null
        var targetFile: DocumentFile? = null

        try {
            if (scheme == "http" || scheme == "https") {
                connection = URL(mediaUri.toString()).openConnection() as HttpURLConnection?
                connection!!.setConnectTimeout(CONNECT_TIMEOUT_MS)
                connection.setReadTimeout(READ_TIMEOUT_MS)
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.connect()

                val responseCode = connection.getResponseCode()
                if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    notifyFailure(context, "Server returned " + responseCode)
                    ExternalDownloadMetadataStore.remove(metadataKey)
                    return
                }

                mimeType = connection.getContentType()
                remoteLength = connection.getContentLengthLong()
            } else if (scheme == "content") {
                sourceDocument = DocumentFile.fromSingleUri(context, mediaUri)
                mimeType = context.getContentResolver().getType(mediaUri)
                if (sourceDocument != null) {
                    remoteLength = sourceDocument.length()
                }
            } else if (scheme == "file") {
                val path = mediaUri.getPath()
                if (path != null) {
                    sourceFile = File(path)
                    if (sourceFile.exists()) {
                        remoteLength = sourceFile.length()
                    }
                }
                val ext = MimeTypeMap.getFileExtensionFromUrl(mediaUri.toString())
                if (ext != null && !ext.isEmpty()) {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                }
            } else {
                notifyFailure(context, "Unsupported media URI.")
                ExternalDownloadMetadataStore.remove(metadataKey)
                return
            }

            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "application/octet-stream"
            }

            var extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if ((extension == null || extension.isEmpty()) && sourceDocument != null && sourceDocument.getName() != null) {
                val name = sourceDocument.getName()
                val dot = name!!.lastIndexOf('.')
                if (dot >= 0 && dot < name.length - 1) {
                    extension = name.substring(dot + 1)
                }
            }
            if ((extension == null || extension.isEmpty()) && sourceFile != null) {
                val name = sourceFile.getName()
                val dot = name.lastIndexOf('.')
                if (dot >= 0 && dot < name.length - 1) {
                    extension = name.substring(dot + 1)
                }
            }
            if (extension == null || extension.isEmpty()) {
                val suffix = child.suffix
                if (suffix != null && !suffix.isEmpty()) {
                    extension = suffix
                } else {
                    extension = "bin"
                }
            }

            var sanitized = sanitizeFileName(baseName)
            if (sanitized.isEmpty()) sanitized =
                ExternalAudioWriter.sanitizeFileName(fallbackName ?: "download")
            if (sanitized.isEmpty()) sanitized = "download"
            val fileName = sanitized + "." + extension

            val existingFile = findFile(directory, fileName)
            val recordedSize = ExternalDownloadMetadataStore.getSize(metadataKey)
            if (existingFile != null && existingFile.exists()) {
                val localLength = existingFile.length()
                var matches = false
                if (remoteLength > 0 && localLength == remoteLength) {
                    matches = true
                } else if (remoteLength <= 0 && recordedSize != null && localLength == recordedSize) {
                    matches = true
                }
                if (matches) {
                    ExternalDownloadMetadataStore.recordSize(metadataKey, localLength)
                    recordDownload(child, existingFile.getUri())
                    ExternalAudioReader.refreshCacheAsync()
                    notifyExists(context, fileName)
                    return
                } else {
                    existingFile.delete()
                    ExternalDownloadMetadataStore.remove(metadataKey)
                }
            }

            targetFile = directory.createFile(mimeType, fileName)
            if (targetFile == null) {
                notifyFailure(context, "Failed to create file.")
                return
            }

            val targetUri = targetFile.getUri()
            ExternalAudioWriter.openInputStream(
                context,
                mediaUri,
                scheme,
                connection!!,
                sourceFile!!
            ).use { `in` ->
                context.getContentResolver().openOutputStream(targetUri).use { out ->
                    if (out == null) {
                        notifyFailure(context, "Cannot open output stream.")
                        targetFile.delete()
                        return
                    }
                    val buffer = ByteArray(BUFFER_SIZE)
                    var len: Int
                    var total: Long = 0
                    while ((`in`.read(buffer).also { len = it }) != -1) {
                        out.write(buffer, 0, len)
                        total += len.toLong()
                    }
                    out.flush()

                    if (total <= 0) {
                        targetFile.delete()
                        ExternalDownloadMetadataStore.remove(metadataKey)
                        notifyFailure(context, "Empty download.")
                        return
                    }

                    if (remoteLength > 0 && total != remoteLength) {
                        targetFile.delete()
                        ExternalDownloadMetadataStore.remove(metadataKey)
                        notifyFailure(context, "Incomplete download.")
                        return
                    }

                    ExternalDownloadMetadataStore.recordSize(metadataKey, total)
                    recordDownload(child, targetUri)
                    notifySuccess(context, fileName, child, targetUri)
                    ExternalAudioReader.refreshCacheAsync()
                }
            }
        } catch (e: Exception) {
            if (targetFile != null) {
                targetFile.delete()
            }
            ExternalDownloadMetadataStore.remove(metadataKey)
            notifyFailure(context, if (e.message != null) e.message else "Download failed")
        } finally {
            if (connection != null) {
                connection.disconnect()
            }
        }
    }

    private fun notifyUnavailable(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val settingsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.getPackageName(), null)
        )
        val openSettings = PendingIntent.getActivity(
            context, 0, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder =
            NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("No download folder set")
                .setContentText("Tap to set one in settings")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setContentIntent(openSettings)
                .setAutoCancel(true)

        manager.notify(1011, builder.build())
    }

    private fun notifyFailure(context: Context, message: String?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder =
            NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Download failed")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun notifySuccess(context: Context, name: String?, child: Child, fileUri: Uri?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder =
            NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Download complete")
                .setContentText(name)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)

        val playIntent = buildPlayIntent(context, child, fileUri)
        if (playIntent != null) {
            builder.setContentIntent(playIntent)
        }

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun recordDownload(child: Child?, fileUri: Uri?) {
        if (child == null) {
            return
        }

        val download = Download(child)
        download.downloadState = 1
        if (fileUri != null) {
            download.downloadUri = fileUri.toString()
        }

        DownloadRepository().insert(download)
    }

    private fun notifyExists(context: Context, name: String?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder =
            NotificationCompat.Builder(context, DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Already downloaded")
                .setContentText(name)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setAutoCancel(true)
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun buildPlayIntent(context: Context?, child: Child, fileUri: Uri?): PendingIntent? {
        if (fileUri == null) return null
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Constants.ACTION_PLAY_EXTERNAL_DOWNLOAD)
            .putExtra(Constants.EXTRA_DOWNLOAD_URI, fileUri.toString())
            .putExtra(Constants.EXTRA_DOWNLOAD_MEDIA_ID, child.id)
            .putExtra(Constants.EXTRA_DOWNLOAD_TITLE, child.title)
            .putExtra(Constants.EXTRA_DOWNLOAD_ARTIST, child.artist)
            .putExtra(Constants.EXTRA_DOWNLOAD_ALBUM, child.album)
            .putExtra(
                Constants.EXTRA_DOWNLOAD_DURATION,
                if (child.duration != null) child.duration else 0
            )
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val requestCode: Int
        if (child.id != null) {
            requestCode = abs(child.id.hashCode())
        } else {
            requestCode = abs(fileUri.toString().hashCode())
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Throws(IOException::class)
    private fun openInputStream(
        context: Context,
        mediaUri: Uri,
        scheme: String,
        connection: HttpURLConnection,
        sourceFile: File
    ): InputStream {
        when (scheme) {
            "http", "https" -> {
                if (connection == null) {
                    throw IOException("Connection not initialized")
                }
                return connection.getInputStream()
            }

            "content" -> {
                val contentStream: InputStream =
                    context.getContentResolver().openInputStream(mediaUri)!!
                if (contentStream == null) {
                    throw IOException("Cannot open content stream")
                }
                return contentStream
            }

            "file" -> {
                if (sourceFile == null || !sourceFile.exists()) {
                    throw IOException("Missing source file")
                }
                return FileInputStream(sourceFile)
            }

            else -> throw IOException("Unsupported scheme " + scheme)
        }
    }
}