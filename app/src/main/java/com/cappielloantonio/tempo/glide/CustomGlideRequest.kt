package com.cappielloantonio.tempo.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.signature.ObjectKey
import com.cappielloantonio.tempo.App.Companion.getSubsonicClientInstance
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.Preferences.getImageSize
import com.cappielloantonio.tempo.util.Preferences.getRoundedCornerSize
import com.cappielloantonio.tempo.util.Preferences.isCornerRoundingEnabled
import com.cappielloantonio.tempo.util.Preferences.isDataSavingMode
import com.cappielloantonio.tempo.util.Util
import com.google.android.material.elevation.SurfaceColors

object CustomGlideRequest {
    private const val TAG = "CustomGlideRequest"

    @JvmField
    val CORNER_RADIUS: Int = if (isCornerRoundingEnabled()) getRoundedCornerSize() else 1

    val DEFAULT_DISK_CACHE_STRATEGY: DiskCacheStrategy = DiskCacheStrategy.ALL

    fun createRequestOptions(context: Context, item: String?, type: ResourceType): RequestOptions {
        return RequestOptions()
            .placeholder(ColorDrawable(SurfaceColors.SURFACE_5.getColor(context)))
            .fallback(getPlaceholder(context, type))
            .error(getPlaceholder(context, type))
            .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .signature(ObjectKey(if (item != null) item else 0))
            .transform(CenterCrop(), RoundedCorners(CORNER_RADIUS))
    }

    private fun getPlaceholder(context: Context, type: ResourceType): Drawable? {
        when (type) {
            ResourceType.Album -> return AppCompatResources.getDrawable(
                context,
                R.drawable.ic_placeholder_album
            )

            ResourceType.Artist -> return AppCompatResources.getDrawable(
                context,
                R.drawable.ic_placeholder_artist
            )

            ResourceType.Folder -> return AppCompatResources.getDrawable(
                context,
                R.drawable.ic_placeholder_folder
            )

            ResourceType.Directory -> return AppCompatResources.getDrawable(
                context,
                R.drawable.ic_placeholder_directory
            )

            ResourceType.Playlist -> return AppCompatResources.getDrawable(
                context,
                R.drawable.ic_placeholder_playlist
            )

            ResourceType.Podcast -> return AppCompatResources.getDrawable(
                context,
                R.drawable.ic_placeholder_podcast
            )

            ResourceType.Radio -> return AppCompatResources.getDrawable(
                context,
                R.drawable.ic_placeholder_radio
            )

            ResourceType.Song -> return AppCompatResources.getDrawable(
                context,
                R.drawable.ic_placeholder_song
            )

            ResourceType.Unknown -> return ColorDrawable(SurfaceColors.SURFACE_5.getColor(context))
            else -> return ColorDrawable(SurfaceColors.SURFACE_5.getColor(context))
        }
    }

    fun createUrl(item: String?, size: Int): String {
        val params = getSubsonicClientInstance(false).params

        val uri = StringBuilder()

        uri.append(getSubsonicClientInstance(false).url)
        uri.append("getCoverArt")

        if (params.containsKey("u") && params.get("u") != null) uri.append("?u=")
            .append(Util.encode(params.get("u")))
        if (params.containsKey("p") && params.get("p") != null) uri.append("&p=")
            .append(params.get("p"))
        if (params.containsKey("s") && params.get("s") != null) uri.append("&s=")
            .append(params.get("s"))
        if (params.containsKey("t") && params.get("t") != null) uri.append("&t=")
            .append(params.get("t"))
        if (params.containsKey("v") && params.get("v") != null) uri.append("&v=")
            .append(params.get("v"))
        if (params.containsKey("c") && params.get("c") != null) uri.append("&c=")
            .append(params.get("c"))
        if (size != -1) uri.append("&size=").append(size)

        uri.append("&id=").append(item)

        Log.d(TAG, "createUrl() " + uri)

        return uri.toString()
    }

    fun loadAlbumArtBitmap(
        context: Context,
        coverId: String?,
        size: Int,
        target: CustomTarget<Bitmap?>
    ) {
        val url = createUrl(coverId, size)
        Glide.with(context)
            .asBitmap()
            .load(url)
            .apply(createRequestOptions(context, coverId, ResourceType.Album))
            .into<CustomTarget<Bitmap?>?>(target)
    }

    enum class ResourceType {
        Unknown,
        Album,
        Artist,
        Folder,
        Directory,
        Playlist,
        Podcast,
        Radio,
        Song,
    }

    class Builder private constructor(context: Context, item: String?, type: ResourceType) {
        private val requestManager: RequestManager
        private var item: String? = null

        init {
            this.requestManager = Glide.with(context)

            if (item != null && !isDataSavingMode()) {
                this.item = createUrl(item, getImageSize())
            }

            requestManager.applyDefaultRequestOptions(createRequestOptions(context, item, type))
        }

        fun build(): RequestBuilder<Drawable?> {
            return requestManager
                .load(item)
                .transition(DrawableTransitionOptions.withCrossFade())
        }

        companion object {
            @JvmStatic
            fun from(context: Context, item: String?, type: ResourceType): Builder {
                return Builder(context, item, type)
            }
        }
    }
}
