package com.cappielloantonio.tempo.util

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.ExtensionRendererMode
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.cappielloantonio.tempo.service.DownloaderManager
import com.cappielloantonio.tempo.util.Preferences.getDownloadStoragePreference
import com.cappielloantonio.tempo.util.Preferences.getStreamingCacheSize
import com.cappielloantonio.tempo.util.Preferences.getStreamingCacheStoragePreference
import com.cappielloantonio.tempo.util.Preferences.setDownloadStoragePreference
import com.cappielloantonio.tempo.util.Preferences.setStreamingCacheStoragePreference
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.Locale
import java.util.concurrent.Executors

@UnstableApi
object DownloadUtil {
    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID: String = "download_channel"
    const val DOWNLOAD_NOTIFICATION_SUCCESSFUL_GROUP: String =
        "com.cappielloantonio.tempo.SuccessfulDownload"
    const val DOWNLOAD_NOTIFICATION_FAILED_GROUP: String =
        "com.cappielloantonio.tempo.FailedDownload"

    private const val STREAMING_CACHE_CONTENT_DIRECTORY = "streaming_cache"
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    private var dataSourceFactory: DataSource.Factory? = null

    @get:Synchronized
    var httpDataSourceFactory: DataSource.Factory? = null
        get() {
            if (field == null) {
                val cookieManager = CookieManager()
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
                CookieHandler.setDefault(cookieManager)
                field = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
            }

            return field
        }
        private set
    private var databaseProvider: DatabaseProvider? = null
    private var streamingCacheDirectory: File? = null
    private var downloadDirectory: File? = null
    private var downloadCache: Cache? = null
    private var streamingCache: SimpleCache? = null
    private var downloadManager: DownloadManager? = null
    private var downloaderManager: DownloaderManager? = null
    private var downloadNotificationHelper: DownloadNotificationHelper? = null

    fun useExtensionRenderers(): Boolean {
        return true
    }

    fun buildRenderersFactory(
        context: Context,
        preferExtensionRenderer: Boolean
    ): RenderersFactory {
        val extensionRendererMode: @ExtensionRendererMode Int =
            if (useExtensionRenderers())
                (if (preferExtensionRenderer) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            else
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF

        return DefaultRenderersFactory(context.getApplicationContext()).setExtensionRendererMode(
            extensionRendererMode
        )
    }

    @get:Synchronized
    val httpDataSourceFactoryForRadio: DataSource.Factory
        get() {
            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
            CookieHandler.setDefault(cookieManager)


            // Create a factory with ICY metadata support for radio streams
            val defaultRequestProperties: MutableMap<String, String> =
                HashMap()
            defaultRequestProperties.put("Icy-MetaData", "1")
            defaultRequestProperties.put("User-Agent", "Tempus/1.0")

            return DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(defaultRequestProperties)
        }

    @Synchronized
    fun getUpstreamDataSourceFactory(context: Context): DataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(
            context,
            httpDataSourceFactory!!
        )
        dataSourceFactory = buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context))
        return dataSourceFactory!!
    }

    @Synchronized
    fun getUpstreamDataSourceFactoryForRadio(context: Context): DataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(
            context,
            httpDataSourceFactoryForRadio
        )
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context))
    }

    @Synchronized
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val streamCacheFactory = CacheDataSource.Factory()
            .setCache(getStreamingCache(context))
            .setUpstreamDataSourceFactory(getUpstreamDataSourceFactory(context))

        val resolvingFactory = ResolvingDataSource.Factory(
            StreamingCacheDataSource.Factory(streamCacheFactory),
            ResolvingDataSource.Resolver { dataSpec: DataSpec? ->
                val builder = dataSpec!!.buildUpon()
                builder.setFlags(dataSpec.flags and DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN.inv())
                builder.build()
            }
        )
        dataSourceFactory =
            buildReadOnlyCacheDataSource(resolvingFactory, getDownloadCache(context))
        return dataSourceFactory!!
    }

    @Synchronized
    fun getDownloadNotificationHelper(context: Context): DownloadNotificationHelper {
        if (downloadNotificationHelper == null) {
            downloadNotificationHelper =
                DownloadNotificationHelper(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        }

        return downloadNotificationHelper!!
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        ensureDownloadManagerInitialized(context)
        return downloadManager!!
    }

    @Synchronized
    fun getDownloadTracker(context: Context): DownloaderManager {
        ensureDownloadManagerInitialized(context)
        return downloaderManager!!
    }

    @Synchronized
    private fun getDownloadCache(context: Context): Cache {
        if (downloadCache == null) {
            val downloadContentDirectory =
                File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            downloadCache = SimpleCache(
                downloadContentDirectory,
                NoOpCacheEvictor(),
                getDatabaseProvider(context)
            )
        }

        return downloadCache!!
    }

    @Synchronized
    private fun getStreamingCache(context: Context): SimpleCache {
        if (streamingCache == null) {
            val streamingCacheDirectory =
                File(getStreamingCacheDirectory(context), STREAMING_CACHE_CONTENT_DIRECTORY)

            streamingCache = SimpleCache(
                streamingCacheDirectory,
                LeastRecentlyUsedCacheEvictor(getStreamingCacheSize() * 1024 * 1024),
                getDatabaseProvider(context)
            )
        }

        return streamingCache!!
    }

    @Synchronized
    private fun ensureDownloadManagerInitialized(context: Context) {
        if (downloadManager == null) {
            downloadManager = DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                httpDataSourceFactory!!,
                Executors.newFixedThreadPool(6)
            )

            downloaderManager = DownloaderManager(
                context,
                httpDataSourceFactory, downloadManager!!
            )
        }
    }

    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider {
        if (databaseProvider == null) {
            databaseProvider = StandaloneDatabaseProvider(context)
        }

        return databaseProvider!!
    }

    @Synchronized
    private fun getStreamingCacheDirectory(context: Context): File? {
        if (streamingCacheDirectory == null) {
            if (getStreamingCacheStoragePreference() == 0) {
                streamingCacheDirectory = context.getExternalFilesDirs(null)[0]
                if (streamingCacheDirectory == null) {
                    streamingCacheDirectory = context.getFilesDir()
                }
            } else {
                try {
                    streamingCacheDirectory = context.getExternalFilesDirs(null)[1]
                } catch (exception: Exception) {
                    streamingCacheDirectory = context.getExternalFilesDirs(null)[0]
                    setStreamingCacheStoragePreference(0)
                }
            }
        }

        return streamingCacheDirectory
    }

    @Synchronized
    private fun getDownloadDirectory(context: Context): File {
        if (downloadDirectory == null) {
            val pref = getDownloadStoragePreference()
            if (pref == 0) {
                downloadDirectory = context.getExternalFilesDirs(null)[0]
                if (downloadDirectory == null) {
                    downloadDirectory = context.getFilesDir()
                }
            } else if (pref == 1) {
                try {
                    downloadDirectory = context.getExternalFilesDirs(null)[1]
                } catch (exception: Exception) {
                    downloadDirectory = context.getExternalFilesDirs(null)[0]
                    setDownloadStoragePreference(0)
                }
            } else {
                downloadDirectory = context.getExternalFilesDirs(null)[0]
            }
        }

        return downloadDirectory!!
    }

    private fun buildReadOnlyCacheDataSource(
        upstreamFactory: DataSource.Factory?,
        cache: Cache
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    fun eraseDownloadFolder(context: Context) {
        val directory = getDownloadDirectory(context)

        val files = listFiles(directory, ArrayList<File>())

        for (file in files) {
            file.delete()
        }
    }

    @Synchronized
    private fun listFiles(directory: File, files: ArrayList<File>): ArrayList<File> {
        if (directory.isDirectory()) {
            val list = directory.listFiles()

            if (list != null) {
                for (file in list) {
                    if (file.isFile() && file.getName().lowercase(Locale.getDefault())
                            .endsWith(".exo")
                    ) {
                        files.add(file)
                    } else if (file.isDirectory()) {
                        listFiles(file, files)
                    }
                }
            }
        }

        return files
    }

    @Synchronized
    fun getStreamingCacheSize(context: Context): Long {
        return getStreamingCache(context).getCacheSpace()
    }

    fun buildGroupSummaryNotification(
        context: Context,
        channelId: String,
        groupId: String?,
        icon: Int,
        title: String?
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setSmallIcon(icon)
            .setGroup(groupId)
            .setGroupSummary(true)
            .build()
    }
}
