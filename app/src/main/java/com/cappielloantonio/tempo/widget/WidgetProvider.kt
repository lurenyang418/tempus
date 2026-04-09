package com.cappielloantonio.tempo.widget

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.RemoteViews
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.activity.MainActivity

open class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(ctx: Context?, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            val rv = WidgetUpdateManager.chooseBuild(ctx!!, id)
            attachIntents(ctx, rv, id, null, null, null)
            mgr.updateAppWidget(id, rv)
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val a = intent.getAction()
        Log.d(TAG, "onReceive action=" + a)
        if (ACT_PLAY_PAUSE == a || ACT_NEXT == a || ACT_PREV == a
            || ACT_TOGGLE_SHUFFLE == a || ACT_CYCLE_REPEAT == a
        ) {
            WidgetActions.dispatchToMediaSession(ctx, a)
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == a) {
            WidgetUpdateManager.refreshFromController(ctx)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val rv = WidgetUpdateManager.chooseBuild(context, appWidgetId)
        attachIntents(context, rv, appWidgetId, null, null, null)
        appWidgetManager.updateAppWidget(appWidgetId, rv)
        WidgetUpdateManager.refreshFromController(context)
    }

    companion object {
        private const val TAG = "TempoWidget"
        const val ACT_PLAY_PAUSE: String = "tempo.widget.PLAY_PAUSE"
        const val ACT_NEXT: String = "tempo.widget.NEXT"
        const val ACT_PREV: String = "tempo.widget.PREV"
        const val ACT_TOGGLE_SHUFFLE: String = "tempo.widget.SHUFFLE"
        const val ACT_CYCLE_REPEAT: String = "tempo.widget.REPEAT"

        @JvmOverloads
        fun attachIntents(
            ctx: Context?, rv: RemoteViews, requestCodeBase: Int = 0,
            songLink: String? = null,
            albumLink: String? = null,
            artistLink: String? = null
        ) {
            val playPause = PendingIntent.getBroadcast(
                ctx,
                requestCodeBase + 0,
                Intent(ctx, WidgetProvider4x1::class.java).setAction(ACT_PLAY_PAUSE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val next = PendingIntent.getBroadcast(
                ctx,
                requestCodeBase + 1,
                Intent(ctx, WidgetProvider4x1::class.java).setAction(ACT_NEXT),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val prev = PendingIntent.getBroadcast(
                ctx,
                requestCodeBase + 2,
                Intent(ctx, WidgetProvider4x1::class.java).setAction(ACT_PREV),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val shuffle = PendingIntent.getBroadcast(
                ctx,
                requestCodeBase + 3,
                Intent(ctx, WidgetProvider4x1::class.java).setAction(ACT_TOGGLE_SHUFFLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val repeat = PendingIntent.getBroadcast(
                ctx,
                requestCodeBase + 4,
                Intent(ctx, WidgetProvider4x1::class.java).setAction(ACT_CYCLE_REPEAT),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            rv.setOnClickPendingIntent(R.id.btn_play_pause, playPause)
            rv.setOnClickPendingIntent(R.id.btn_next, next)
            rv.setOnClickPendingIntent(R.id.btn_prev, prev)
            rv.setOnClickPendingIntent(R.id.btn_shuffle, shuffle)
            rv.setOnClickPendingIntent(R.id.btn_repeat, repeat)

            val launch: PendingIntent? =
                buildMainActivityPendingIntent(ctx, requestCodeBase + 10, null)
            rv.setOnClickPendingIntent(R.id.root, launch)

            val songPending: PendingIntent? =
                buildMainActivityPendingIntent(ctx, requestCodeBase + 20, songLink)
            val artistPending: PendingIntent? =
                buildMainActivityPendingIntent(ctx, requestCodeBase + 21, artistLink)
            val albumPending: PendingIntent? =
                buildMainActivityPendingIntent(ctx, requestCodeBase + 22, albumLink)

            val fallback = launch
            rv.setOnClickPendingIntent(
                R.id.album_art,
                if (songPending != null) songPending else fallback
            )
            rv.setOnClickPendingIntent(
                R.id.title,
                if (songPending != null) songPending else fallback
            )
            rv.setOnClickPendingIntent(
                R.id.subtitle,
                if (artistPending != null) artistPending else (if (songPending != null) songPending else fallback)
            )
            rv.setOnClickPendingIntent(
                R.id.album,
                if (albumPending != null) albumPending else fallback
            )
        }

        private fun buildMainActivityPendingIntent(
            ctx: Context?,
            requestCode: Int,
            link: String?
        ): PendingIntent? {
            val intent: Intent?
            if (!TextUtils.isEmpty(link)) {
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(link), ctx, MainActivity::class.java)
            } else {
                intent = Intent(ctx, MainActivity::class.java)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val stackBuilder = TaskStackBuilder.create(ctx)
            stackBuilder.addNextIntentWithParentStack(intent)
            return stackBuilder.getPendingIntent(
                requestCode,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
