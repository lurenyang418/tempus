package com.cappielloantonio.tempo.broadcast.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.cappielloantonio.tempo.ui.activity.MainActivity

@OptIn(markerClass = [UnstableApi::class])
class ConnectivityStatusBroadcastReceiver(private val activity: MainActivity) :
    BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.getAction()) {
            val noConnectivity =
                intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)

            if (noConnectivity) {
                activity.binding?.offlineModeTextView?.setVisibility(View.VISIBLE)
            } else {
                activity.binding?.offlineModeTextView?.setVisibility(View.GONE)
            }
        }
    }
}