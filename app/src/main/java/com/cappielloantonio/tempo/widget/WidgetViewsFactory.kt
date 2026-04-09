package com.cappielloantonio.tempo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import com.cappielloantonio.tempo.R
import kotlin.math.min

object WidgetViewsFactory {
    const val PROGRESS_MAX: Int = 1000
    private const val ALBUM_ART_CORNER_RADIUS_DP = 6f

    fun buildCompact(ctx: Context): RemoteViews {
        return build(ctx, R.layout.widget_layout_compact, false, false)
    }

    fun buildMedium(ctx: Context): RemoteViews {
        return build(ctx, R.layout.widget_layout_medium, false, false)
    }

    fun buildLarge(ctx: Context): RemoteViews {
        return build(ctx, R.layout.widget_layout_large_short, true, true)
    }

    fun buildExpanded(ctx: Context): RemoteViews {
        return build(ctx, R.layout.widget_layout_large, true, true)
    }

    private fun build(
        ctx: Context,
        layoutRes: Int,
        showAlbum: Boolean,
        showSecondaryControls: Boolean
    ): RemoteViews {
        val rv = RemoteViews(ctx.getPackageName(), layoutRes)
        rv.setTextViewText(R.id.title, ctx.getString(R.string.widget_not_playing))
        rv.setTextViewText(R.id.subtitle, ctx.getString(R.string.widget_placeholder_subtitle))
        rv.setTextViewText(R.id.album, "")
        rv.setViewVisibility(R.id.album, if (showAlbum) View.INVISIBLE else View.GONE)
        rv.setTextViewText(
            R.id.time_elapsed,
            ctx.getString(R.string.widget_time_elapsed_placeholder)
        )
        rv.setTextViewText(
            R.id.time_total,
            ctx.getString(R.string.widget_time_duration_placeholder)
        )
        rv.setProgressBar(R.id.progress, PROGRESS_MAX, 0, false)
        rv.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_play)
        rv.setImageViewResource(R.id.album_art, R.drawable.ic_splash_logo)
        applySecondaryControlsDefaults(ctx, rv, showSecondaryControls)
        return rv
    }

    private fun applySecondaryControlsDefaults(
        ctx: Context,
        rv: RemoteViews,
        show: Boolean
    ) {
        val visibility = if (show) View.VISIBLE else View.GONE
        rv.setViewVisibility(R.id.controls_secondary, visibility)
        rv.setViewVisibility(R.id.btn_shuffle, visibility)
        rv.setViewVisibility(R.id.btn_repeat, visibility)
        if (show) {
            val defaultColor = ContextCompat.getColor(ctx, R.color.widget_icon_tint)
            rv.setImageViewResource(R.id.btn_shuffle, R.drawable.ic_shuffle)
            rv.setImageViewResource(R.id.btn_repeat, R.drawable.ic_repeat)
            rv.setInt(R.id.btn_shuffle, "setColorFilter", defaultColor)
            rv.setInt(R.id.btn_repeat, "setColorFilter", defaultColor)
        }
    }

    fun populateCompact(
        ctx: Context,
        title: String?,
        subtitle: String?,
        album: String?,
        art: Bitmap?,
        playing: Boolean,
        elapsedText: String,
        totalText: String,
        progress: Int,
        shuffleEnabled: Boolean,
        repeatMode: Int
    ): RemoteViews {
        return populateWithLayout(
            ctx, title, subtitle, album, art, playing, elapsedText, totalText,
            progress, R.layout.widget_layout_compact, false, false, shuffleEnabled, repeatMode
        )
    }

    fun populateMedium(
        ctx: Context,
        title: String?,
        subtitle: String?,
        album: String?,
        art: Bitmap?,
        playing: Boolean,
        elapsedText: String,
        totalText: String,
        progress: Int,
        shuffleEnabled: Boolean,
        repeatMode: Int
    ): RemoteViews {
        return populateWithLayout(
            ctx, title, subtitle, album, art, playing, elapsedText, totalText,
            progress, R.layout.widget_layout_medium, true, true, shuffleEnabled, repeatMode
        )
    }

    fun populateLarge(
        ctx: Context,
        title: String?,
        subtitle: String?,
        album: String?,
        art: Bitmap?,
        playing: Boolean,
        elapsedText: String,
        totalText: String,
        progress: Int,
        shuffleEnabled: Boolean,
        repeatMode: Int
    ): RemoteViews {
        return populateWithLayout(
            ctx, title, subtitle, album, art, playing, elapsedText, totalText,
            progress, R.layout.widget_layout_large_short, true, true, shuffleEnabled, repeatMode
        )
    }

    fun populateExpanded(
        ctx: Context,
        title: String?,
        subtitle: String?,
        album: String?,
        art: Bitmap?,
        playing: Boolean,
        elapsedText: String,
        totalText: String,
        progress: Int,
        shuffleEnabled: Boolean,
        repeatMode: Int
    ): RemoteViews {
        return populateWithLayout(
            ctx, title, subtitle, album, art, playing, elapsedText, totalText,
            progress, R.layout.widget_layout_large, true, true, shuffleEnabled, repeatMode
        )
    }

    private fun populateWithLayout(
        ctx: Context,
        title: String?,
        subtitle: String?,
        album: String?,
        art: Bitmap?,
        playing: Boolean,
        elapsedText: String,
        totalText: String,
        progress: Int,
        layoutRes: Int,
        showAlbum: Boolean,
        showSecondaryControls: Boolean,
        shuffleEnabled: Boolean,
        repeatMode: Int
    ): RemoteViews {
        val rv = RemoteViews(ctx.getPackageName(), layoutRes)
        rv.setTextViewText(R.id.title, title)
        rv.setTextViewText(R.id.subtitle, subtitle)

        if (showAlbum && !TextUtils.isEmpty(album)) {
            rv.setTextViewText(R.id.album, album)
            rv.setViewVisibility(R.id.album, View.VISIBLE)
        } else {
            rv.setTextViewText(R.id.album, "")
            rv.setViewVisibility(R.id.album, View.GONE)
        }

        if (art != null) {
            val rounded = maybeRoundBitmap(ctx, art)
            rv.setImageViewBitmap(R.id.album_art, if (rounded != null) rounded else art)
        } else {
            rv.setImageViewResource(R.id.album_art, R.drawable.ic_splash_logo)
        }

        rv.setImageViewResource(
            R.id.btn_play_pause,
            if (playing) R.drawable.ic_pause else R.drawable.ic_play
        )

        val elapsed = if (!TextUtils.isEmpty(elapsedText))
            elapsedText
        else
            ctx.getString(R.string.widget_time_elapsed_placeholder)
        val total = if (!TextUtils.isEmpty(totalText))
            totalText
        else
            ctx.getString(R.string.widget_time_duration_placeholder)

        var safeProgress = progress
        if (safeProgress < 0) safeProgress = 0
        if (safeProgress > PROGRESS_MAX) safeProgress = PROGRESS_MAX

        rv.setTextViewText(R.id.time_elapsed, elapsed)
        rv.setTextViewText(R.id.time_total, total)
        rv.setProgressBar(R.id.progress, PROGRESS_MAX, safeProgress, false)

        applySecondaryControls(ctx, rv, showSecondaryControls, shuffleEnabled, repeatMode)

        return rv
    }

    private fun maybeRoundBitmap(ctx: Context, source: Bitmap?): Bitmap? {
        if (source == null || source.isRecycled()) {
            return null
        }

        try {
            val width = source.getWidth()
            val height = source.getHeight()
            if (width <= 0 || height <= 0) {
                return null
            }

            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.setShader(BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))

            val radiusPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                ALBUM_ART_CORNER_RADIUS_DP,
                ctx.getResources().getDisplayMetrics()
            )
            val maxRadius = min(width, height) / 2f
            val safeRadius = min(radiusPx, maxRadius)

            canvas.drawRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                safeRadius,
                safeRadius,
                paint
            )
            return output
        } catch (e: RuntimeException) {
            Log.w("TempoWidget", "Failed to round album art", e)
            return null
        } catch (e: OutOfMemoryError) {
            Log.w("TempoWidget", "Failed to round album art", e)
            return null
        }
    }

    private fun applySecondaryControls(
        ctx: Context,
        rv: RemoteViews,
        show: Boolean,
        shuffleEnabled: Boolean,
        repeatMode: Int
    ) {
        if (!show) {
            rv.setViewVisibility(R.id.controls_secondary, View.GONE)
            rv.setViewVisibility(R.id.btn_shuffle, View.GONE)
            rv.setViewVisibility(R.id.btn_repeat, View.GONE)
            return
        }

        val inactiveColor = ContextCompat.getColor(ctx, R.color.widget_icon_tint)
        val activeColor = ContextCompat.getColor(ctx, R.color.widget_icon_tint_active)

        rv.setViewVisibility(R.id.controls_secondary, View.VISIBLE)
        rv.setViewVisibility(R.id.btn_shuffle, View.VISIBLE)
        rv.setViewVisibility(R.id.btn_repeat, View.VISIBLE)
        rv.setImageViewResource(R.id.btn_shuffle, R.drawable.ic_shuffle)
        rv.setImageViewResource(
            R.id.btn_repeat,
            if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat
        )
        rv.setInt(
            R.id.btn_shuffle,
            "setColorFilter",
            if (shuffleEnabled) activeColor else inactiveColor
        )
        rv.setInt(
            R.id.btn_repeat, "setColorFilter",
            if (repeatMode == Player.REPEAT_MODE_OFF) inactiveColor else activeColor
        )
    }
}
