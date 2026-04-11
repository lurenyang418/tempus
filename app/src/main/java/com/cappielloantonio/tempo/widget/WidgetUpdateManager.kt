package com.cappielloantonio.tempo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.glide.CustomGlideRequest
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.util.AssetLinkUtil
import com.cappielloantonio.tempo.util.MusicUtil
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ExecutionException
import kotlin.math.max

object WidgetUpdateManager {
    private const val WIDGET_SAFE_ART_SIZE = 512

    fun updateFromState(
        ctx: Context,
        title: String?,
        artist: String?,
        album: String?,
        art: Bitmap?,
        playing: Boolean,
        shuffleEnabled: Boolean,
        repeatMode: Int,
        positionMs: Long,
        durationMs: Long,
        songLink: String?,
        albumLink: String?,
        artistLink: String?
    ) {
        var title = title
        var artist = artist
        var album = album
        if (TextUtils.isEmpty(title)) title = ctx.getString(R.string.widget_not_playing)
        if (TextUtils.isEmpty(artist)) artist = ctx.getString(R.string.widget_placeholder_subtitle)
        if (TextUtils.isEmpty(album)) album = ""

        val timing = createTimingInfo(positionMs, durationMs)

        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, WidgetProvider4x1::class.java))
        for (id in ids) {
            val rv = choosePopulate(
                ctx,
                title,
                artist,
                album,
                art,
                playing,
                timing.elapsedText,
                timing.totalText,
                timing.progress,
                shuffleEnabled,
                repeatMode,
                id
            )
            WidgetProvider.Companion.attachIntents(ctx, rv, id, songLink, albumLink, artistLink)
            mgr.updateAppWidget(id, rv)
        }
    }

    fun pushNow(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, WidgetProvider4x1::class.java))
        for (id in ids) {
            val rv = chooseBuild(ctx, id)
            WidgetProvider.Companion.attachIntents(ctx, rv, id, null, null, null)
            mgr.updateAppWidget(id, rv)
        }
    }

    fun updateFromState(
        ctx: Context,
        title: String?,
        artist: String?,
        album: String,
        coverArtId: String?,
        playing: Boolean,
        shuffleEnabled: Boolean,
        repeatMode: Int,
        positionMs: Long,
        durationMs: Long,
        songLink: String?,
        albumLink: String?,
        artistLink: String?
    ) {
        val appCtx = ctx.getApplicationContext()
        val t: String =
            (if (TextUtils.isEmpty(title)) appCtx.getString(R.string.widget_not_playing) else title)!!
        val a: String =
            (if (TextUtils.isEmpty(artist)) appCtx.getString(R.string.widget_placeholder_subtitle) else artist)!!
        val alb = if (!TextUtils.isEmpty(album)) album else ""
        val p = playing
        val sh = shuffleEnabled
        val rep = repeatMode
        val timing = createTimingInfo(positionMs, durationMs)
        val songLinkFinal = songLink
        val albumLinkFinal = albumLink
        val artistLinkFinal = artistLink

        if (!TextUtils.isEmpty(coverArtId)) {
            CustomGlideRequest.loadAlbumArtBitmap(
                appCtx,
                coverArtId,
                WIDGET_SAFE_ART_SIZE,
                object : CustomTarget<Bitmap?>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        val mgr = AppWidgetManager.getInstance(appCtx)
                        val ids = mgr.getAppWidgetIds(
                            ComponentName(
                                appCtx,
                                WidgetProvider4x1::class.java
                            )
                        )
                        for (id in ids) {
                            val rv = choosePopulate(
                                appCtx, t, a, alb, resource, p,
                                timing.elapsedText, timing.totalText, timing.progress, sh, rep, id
                            )
                            WidgetProvider.Companion.attachIntents(
                                appCtx,
                                rv,
                                id,
                                songLinkFinal,
                                albumLinkFinal,
                                artistLinkFinal
                            )
                            mgr.updateAppWidget(id, rv)
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        val mgr = AppWidgetManager.getInstance(appCtx)
                        val ids = mgr.getAppWidgetIds(
                            ComponentName(
                                appCtx,
                                WidgetProvider4x1::class.java
                            )
                        )
                        for (id in ids) {
                            val rv = choosePopulate(
                                appCtx, t, a, alb, null, p,
                                timing.elapsedText, timing.totalText, timing.progress, sh, rep, id
                            )
                            WidgetProvider.Companion.attachIntents(
                                appCtx,
                                rv,
                                id,
                                songLinkFinal,
                                albumLinkFinal,
                                artistLinkFinal
                            )
                            mgr.updateAppWidget(id, rv)
                        }
                    }
                }
            )
        } else {
            val mgr = AppWidgetManager.getInstance(appCtx)
            val ids = mgr.getAppWidgetIds(ComponentName(appCtx, WidgetProvider4x1::class.java))
            for (id in ids) {
                val rv = choosePopulate(
                    appCtx, t, a, alb, null, p,
                    timing.elapsedText, timing.totalText, timing.progress, sh, rep, id
                )
                WidgetProvider.Companion.attachIntents(
                    appCtx,
                    rv,
                    id,
                    songLinkFinal,
                    albumLinkFinal,
                    artistLinkFinal
                )
                mgr.updateAppWidget(id, rv)
            }
        }
    }

    fun refreshFromController(ctx: Context) {
        val appCtx = ctx.getApplicationContext()
        val token = SessionToken(appCtx, ComponentName(appCtx, MediaService::class.java))
        val future = MediaController.Builder(appCtx, token).buildAsync()
        future.addListener({
            try {
                if (!future.isDone()) return@addListener
                val c = future.get()
                val mi = c.getCurrentMediaItem()
                var title: String? = null
                var artist: String? = null
                var album: String? = null
                var coverId: String? = null
                var songLink: String? = null
                var albumLink: String? = null
                var artistLink: String? = null
                if (mi != null) {
                    if (mi.mediaMetadata.title != null) title = mi.mediaMetadata.title.toString()
                    if (mi.mediaMetadata.artist != null) artist = mi.mediaMetadata.artist.toString()
                    if (mi.mediaMetadata.albumTitle != null) album =
                        mi.mediaMetadata.albumTitle.toString()
                    if (mi.mediaMetadata.extras != null) {
                        val extras: Bundle = mi.mediaMetadata.extras!!
                        if (title == null) title = mi.mediaMetadata.extras!!.getString("title")
                        if (artist == null) artist = mi.mediaMetadata.extras!!.getString("artist")
                        if (album == null) album = mi.mediaMetadata.extras!!.getString("album")
                        coverId = extras.getString("coverArtId")

                        songLink = extras.getString("assetLinkSong")
                        if (songLink == null) {
                            songLink = AssetLinkUtil.buildLink(
                                AssetLinkUtil.TYPE_SONG,
                                extras.getString("id")
                            )
                        }

                        albumLink = extras.getString("assetLinkAlbum")
                        if (albumLink == null) {
                            albumLink = AssetLinkUtil.buildLink(
                                AssetLinkUtil.TYPE_ALBUM,
                                extras.getString("albumId")
                            )
                        }

                        artistLink = extras.getString("assetLinkArtist")
                        if (artistLink == null) {
                            artistLink = AssetLinkUtil.buildLink(
                                AssetLinkUtil.TYPE_ARTIST,
                                extras.getString("artistId")
                            )
                        }
                    }
                }
                var position = c.getCurrentPosition()
                var duration = c.getDuration()
                if (position == C.TIME_UNSET) position = 0
                if (duration == C.TIME_UNSET) duration = 0
                WidgetUpdateManager.updateFromState(
                    appCtx,
                    if (title != null) title else appCtx.getString(R.string.widget_not_playing),
                    if (artist != null) artist else appCtx.getString(R.string.widget_placeholder_subtitle),
                    album!!,
                    coverId,
                    c.isPlaying(),
                    c.getShuffleModeEnabled(),
                    c.getRepeatMode(),
                    position,
                    duration,
                    songLink,
                    albumLink,
                    artistLink
                )
                c.release()
            } catch (ignored: ExecutionException) {
            } catch (ignored: InterruptedException) {
            }
        }, MoreExecutors.directExecutor())
    }

    private fun createTimingInfo(positionMs: Long, durationMs: Long): TimingInfo {
        var safePosition = max(0L, positionMs)
        val safeDuration = if (durationMs > 0) durationMs else 0L
        if (safeDuration > 0 && safePosition > safeDuration) {
            safePosition = safeDuration
        }

        val elapsed = if (safeDuration > 0 || safePosition > 0)
            MusicUtil.getReadableDurationString(safePosition, true)
        else
            null
        val total = if (safeDuration > 0)
            MusicUtil.getReadableDurationString(safeDuration, true)
        else
            null

        var progress = 0
        if (safeDuration > 0) {
            val scaled = safePosition * WidgetViewsFactory.PROGRESS_MAX
            val progressLong = scaled / safeDuration
            if (progressLong < 0) {
                progress = 0
            } else if (progressLong > WidgetViewsFactory.PROGRESS_MAX) {
                progress = WidgetViewsFactory.PROGRESS_MAX
            } else {
                progress = progressLong.toInt()
            }
        }

        return TimingInfo(elapsed, total, progress)
    }

    fun chooseBuild(ctx: Context, appWidgetId: Int): RemoteViews {
        val size = resolveLayoutSize(ctx, appWidgetId)
        when (size) {
            LayoutSize.MEDIUM -> return WidgetViewsFactory.buildMedium(ctx)
            LayoutSize.LARGE -> return WidgetViewsFactory.buildLarge(ctx)
            LayoutSize.EXPANDED -> return WidgetViewsFactory.buildExpanded(ctx)
            LayoutSize.COMPACT -> return WidgetViewsFactory.buildCompact(ctx)
        }
    }

    private fun choosePopulate(
        ctx: Context,
        title: String?,
        artist: String?,
        album: String?,
        art: Bitmap?,
        playing: Boolean,
        elapsedText: String?,
        totalText: String?,
        progress: Int,
        shuffleEnabled: Boolean,
        repeatMode: Int,
        appWidgetId: Int
    ): RemoteViews {
        val size = resolveLayoutSize(ctx, appWidgetId)
        when (size) {
            LayoutSize.MEDIUM -> return WidgetViewsFactory.populateMedium(
                ctx, title, artist, album, art, playing,
                elapsedText!!, totalText!!, progress, shuffleEnabled, repeatMode
            )

            LayoutSize.LARGE -> return WidgetViewsFactory.populateLarge(
                ctx, title, artist, album, art, playing,
                elapsedText!!, totalText!!, progress, shuffleEnabled, repeatMode
            )

            LayoutSize.EXPANDED -> return WidgetViewsFactory.populateExpanded(
                ctx, title, artist, album, art, playing,
                elapsedText!!, totalText!!, progress, shuffleEnabled, repeatMode
            )

            LayoutSize.COMPACT -> return WidgetViewsFactory.populateCompact(
                ctx, title, artist, album, art, playing,
                elapsedText!!, totalText!!, progress, shuffleEnabled, repeatMode
            )
        }
    }

    private fun resolveLayoutSize(ctx: Context, appWidgetId: Int): LayoutSize {
        val mgr = AppWidgetManager.getInstance(ctx)
        val opts = mgr.getAppWidgetOptions(appWidgetId)
        val minH =
            if (opts != null) opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) else 0
        val expandedThreshold =
            ctx.getResources().getInteger(R.integer.widget_expanded_min_height_dp)
        val largeThreshold = ctx.getResources().getInteger(R.integer.widget_large_min_height_dp)
        val mediumThreshold = ctx.getResources().getInteger(R.integer.widget_medium_min_height_dp)
        if (minH >= expandedThreshold) return LayoutSize.EXPANDED
        if (minH >= largeThreshold) return LayoutSize.LARGE
        if (minH >= mediumThreshold) return LayoutSize.MEDIUM
        return LayoutSize.COMPACT
    }

    private enum class LayoutSize {
        COMPACT,
        MEDIUM,
        LARGE,
        EXPANDED
    }

    private class TimingInfo(val elapsedText: String?, val totalText: String?, val progress: Int)
}