package com.cappielloantonio.tempo.widget

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cappielloantonio.tempo.service.MediaService
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ExecutionException

object WidgetActions {
    fun dispatchToMediaSession(ctx: Context, action: String) {
        Log.d("TempoWidget", "dispatch action=" + action)
        val appCtx = ctx.getApplicationContext()
        val token = SessionToken(appCtx, ComponentName(appCtx, MediaService::class.java))
        val future = MediaController.Builder(appCtx, token).buildAsync()
        future.addListener({
            try {
                if (!future.isDone()) return@addListener
                val c = future.get()
                Log.d("TempoWidget", "controller connected, isPlaying=" + c.isPlaying())
                when (action) {
                    WidgetProvider.Companion.ACT_PLAY_PAUSE -> if (c.isPlaying()) c.pause()
                    else c.play()

                    WidgetProvider.Companion.ACT_NEXT -> c.seekToNext()
                    WidgetProvider.Companion.ACT_PREV -> c.seekToPrevious()
                    WidgetProvider.Companion.ACT_TOGGLE_SHUFFLE -> c.setShuffleModeEnabled(!c.getShuffleModeEnabled())
                    WidgetProvider.Companion.ACT_CYCLE_REPEAT -> {
                        val repeatMode = c.getRepeatMode()
                        val nextMode: Int
                        if (repeatMode == Player.REPEAT_MODE_OFF) {
                            nextMode = Player.REPEAT_MODE_ALL
                        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
                            nextMode = Player.REPEAT_MODE_ONE
                        } else {
                            nextMode = Player.REPEAT_MODE_OFF
                        }
                        c.setRepeatMode(nextMode)
                    }
                }
                WidgetUpdateManager.refreshFromController(ctx)
                c.release()
            } catch (e: ExecutionException) {
                Log.e("TempoWidget", "dispatch failed", e)
            } catch (e: InterruptedException) {
                Log.e("TempoWidget", "dispatch failed", e)
            }
        }, MoreExecutors.directExecutor())
    }
}
