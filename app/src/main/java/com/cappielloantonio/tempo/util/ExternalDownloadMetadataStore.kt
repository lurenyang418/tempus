package com.cappielloantonio.tempo.util

import android.content.SharedPreferences
import com.cappielloantonio.tempo.App
import org.json.JSONException
import org.json.JSONObject

object ExternalDownloadMetadataStore {
    private const val PREF_KEY = "external_download_metadata"

    private fun preferences(): SharedPreferences? {
        return App.Companion.preferences
    }

    private fun readAll(): JSONObject {
        val raw: String = preferences()
            ?.getString(ExternalDownloadMetadataStore.PREF_KEY, "{}") ?: "{}"
        try {
            return JSONObject(raw)
        } catch (e: JSONException) {
            return JSONObject()
        }
    }

    private fun writeAll(`object`: JSONObject) {
        preferences()?.edit()?.putString(PREF_KEY, `object`.toString())?.apply()
    }

    @JvmStatic
    @Synchronized
    fun clear() {
        writeAll(JSONObject())
    }

    @Synchronized
    fun recordSize(key: String?, size: Long) {
        if (key == null || size <= 0) {
            return
        }
        val `object` = readAll()
        try {
            `object`.put(key, size)
        } catch (ignored: JSONException) {
        }
        writeAll(`object`)
    }

    @Synchronized
    fun remove(key: String?) {
        if (key == null) {
            return
        }
        val `object` = readAll()
        `object`.remove(key)
        writeAll(`object`)
    }

    @Synchronized
    fun getSize(key: String?): Long? {
        if (key == null) {
            return null
        }
        val `object` = readAll()
        if (!`object`.has(key)) {
            return null
        }
        val size = `object`.optLong(key, -1L)
        return if (size > 0) size else null
    }

    @Synchronized
    fun snapshot(): MutableMap<String?, Long?> {
        val `object` = readAll()
        if (`object`.length() == 0) {
            return mutableMapOf<String?, Long?>()
        }
        val sizes: MutableMap<String?, Long?> = HashMap<String?, Long?>()
        val keys = `object`.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val size = `object`.optLong(key, -1L)
            if (size > 0) {
                sizes.put(key, size)
            }
        }
        return sizes
    }

    @Synchronized
    fun retainOnly(keysToKeep: MutableSet<String?>?) {
        if (keysToKeep == null || keysToKeep.isEmpty()) {
            clear()
            return
        }
        val `object` = readAll()
        if (`object`.length() == 0) {
            return
        }
        val keys: MutableSet<String?> = HashSet<String?>()
        val iterator = `object`.keys()
        while (iterator.hasNext()) {
            keys.add(iterator.next())
        }
        var changed = false
        for (key in keys) {
            if (!keysToKeep.contains(key)) {
                `object`.remove(key)
                changed = true
            }
        }
        if (changed) {
            writeAll(`object`)
        }
    }
}