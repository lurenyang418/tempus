package com.cappielloantonio.tempo.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class IPv6StringLoader : ModelLoader<String?, InputStream?> {
    override fun handles(model: String): Boolean {
        return model.startsWith("http://") || model.startsWith("https://")
    }

    override fun buildLoadData(
        model: String,
        width: Int,
        height: Int,
        options: Options
    ): LoadData<InputStream?>? {
        if (!handles(model)) {
            return null
        }
        return LoadData<InputStream?>(ObjectKey(model), IPv6StreamFetcher(model))
    }

    private class IPv6StreamFetcher(private val model: String?) : DataFetcher<InputStream?> {
        private var stream: InputStream? = null
        private var connection: HttpURLConnection? = null

        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in InputStream?>
        ) {
            try {
                val url = URL(model)
                connection = url.openConnection() as HttpURLConnection?
                connection!!.setConnectTimeout(DEFAULT_TIMEOUT_MS)
                connection!!.setReadTimeout(DEFAULT_TIMEOUT_MS)
                connection!!.setUseCaches(true)
                connection!!.setDoInput(true)
                connection!!.connect()

                if (connection!!.getResponseCode() / 100 != 2) {
                    callback.onLoadFailed(IOException("Request failed with status code: " + connection!!.getResponseCode()))
                    return
                }

                stream = connection!!.getInputStream()
                callback.onDataReady(stream)
            } catch (e: IOException) {
                callback.onLoadFailed(e)
            }
        }

        override fun cleanup() {
            if (stream != null) {
                try {
                    stream!!.close()
                } catch (ignored: IOException) {
                }
            }
            if (connection != null) {
                connection!!.disconnect()
            }
        }

        override fun cancel() {
            // HttpURLConnection does not provide a direct cancel mechanism.
        }

        @Suppress("UNCHECKED_CAST")
        override fun getDataClass(): Class<InputStream?> {
            return InputStream::class.java as Class<InputStream?>
        }

        override fun getDataSource(): DataSource {
            return DataSource.REMOTE
        }
    }

    class Factory : ModelLoaderFactory<String?, InputStream?> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String?, InputStream?> {
            return IPv6StringLoader()
        }

        override fun teardown() {
            // No-op
        }
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 2500
    }
}