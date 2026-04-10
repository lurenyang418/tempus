package com.cappielloantonio.tempo.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.cappielloantonio.tempo.R
import com.google.android.material.color.MaterialColors
import java.util.Objects

object AssetLinkUtil {
    const val SCHEME: String = "tempo"
    const val HOST_ASSET: String = "asset"

    const val TYPE_SONG: String = "song"
    const val TYPE_ALBUM: String = "album"
    const val TYPE_ARTIST: String = "artist"
    const val TYPE_PLAYLIST: String = "playlist"
    const val TYPE_GENRE: String = "genre"
    const val TYPE_YEAR: String = "year"

    @JvmStatic
    fun parse(intent: Intent?): AssetLink? {
        if (intent == null) return null
        return parse(intent.getData())
    }

    fun parse(uri: Uri?): AssetLink? {
        if (uri == null) {
            return null
        }

        if (!SCHEME.equals(uri.getScheme(), ignoreCase = true)) {
            return null
        }

        val host = uri.getHost()
        if (!HOST_ASSET.equals(host, ignoreCase = true)) {
            return null
        }

        if (uri.getPathSegments().size < 2) {
            return null
        }

        val type = uri.getPathSegments().get(0)
        val id = uri.getPathSegments().get(1)
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(id)) {
            return null
        }

        if (!isSupportedType(type)) {
            return null
        }

        return AssetLink(type!!, id!!, uri)
    }

    fun isSupportedType(type: String?): Boolean {
        if (type == null) return false
        when (type) {
            TYPE_SONG, TYPE_ALBUM, TYPE_ARTIST, TYPE_PLAYLIST, TYPE_GENRE, TYPE_YEAR -> return true
            else -> return false
        }
    }

    fun buildUri(type: String, id: String): Uri {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_ASSET)
            .appendPath(type)
            .appendPath(id)
            .build()
    }

    fun buildLink(type: String?, id: String?): String? {
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(id) || !isSupportedType(type)) {
            return null
        }
        return buildUri(
            Objects.requireNonNull<String?>(type),
            Objects.requireNonNull<String?>(id)
        ).toString()
    }

    @JvmStatic
    fun buildAssetLink(type: String?, id: String?): AssetLink? {
        val link = buildLink(type, id)
        return parseLinkString(link)
    }

    @JvmStatic
    fun parseLinkString(link: String?): AssetLink? {
        if (TextUtils.isEmpty(link)) {
            return null
        }
        return parse(Uri.parse(link))
    }

    @JvmStatic
    fun copyToClipboard(context: Context, assetLink: AssetLink) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboardManager == null) {
            return
        }
        val clipData = ClipData.newPlainText(
            context.getString(R.string.asset_link_clipboard_label),
            assetLink.uri.toString()
        )
        clipboardManager.setPrimaryClip(clipData)
    }

    @JvmStatic
    @StringRes
    fun getLabelRes(type: String): Int {
        when (type) {
            TYPE_SONG -> return R.string.asset_link_label_song
            TYPE_ALBUM -> return R.string.asset_link_label_album
            TYPE_ARTIST -> return R.string.asset_link_label_artist
            TYPE_PLAYLIST -> return R.string.asset_link_label_playlist
            TYPE_GENRE -> return R.string.asset_link_label_genre
            TYPE_YEAR -> return R.string.asset_link_label_year
            else -> return R.string.asset_link_label_unknown
        }
    }

    @JvmStatic
    fun applyLinkAppearance(view: View) {
        if (view is TextView) {
            val textView = view
            if (textView.getTag(R.id.tag_link_original_color) == null) {
                textView.setTag(R.id.tag_link_original_color, textView.getCurrentTextColor())
            }
            val accent = MaterialColors.getColor(
                view.context, androidx.appcompat.R.attr.colorPrimary,
                ContextCompat.getColor(view.context, android.R.color.holo_blue_light)
            )
            textView.setTextColor(accent)
        }
    }

    @JvmStatic
    fun clearLinkAppearance(view: View) {
        if (view is TextView) {
            val textView = view
            val original = textView.getTag(R.id.tag_link_original_color)
            if (original is Int) {
                textView.setTextColor(original)
            } else {
                val defaultColor = MaterialColors.getColor(
                    view, com.google.android.material.R.attr.colorOnSurface,
                    ContextCompat.getColor(view.getContext(), android.R.color.primary_text_light)
                )
                textView.setTextColor(defaultColor)
            }
        }
    }

    class AssetLink internal constructor(@JvmField val type: String, @JvmField val id: String, val uri: Uri)
}
