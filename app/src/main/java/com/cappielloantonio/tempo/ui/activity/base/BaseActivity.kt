package com.cappielloantonio.tempo.ui.activity.base

import android.Manifest
import android.content.res.Configuration
import android.graphics.Color
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import java.util.concurrent.Future
import com.cappielloantonio.tempo.service.DownloaderService
import com.cappielloantonio.tempo.service.MediaService
import com.cappielloantonio.tempo.ui.dialog.BatteryOptimizationDialog
import com.cappielloantonio.tempo.util.Flavors.initializeCastContext
import com.cappielloantonio.tempo.util.Preferences.askForOptimization
import com.cappielloantonio.tempo.util.Preferences.isDisplayAlwaysOn
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
open class BaseActivity : AppCompatActivity() {
    private var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdge()
        initializeCastContext(this)
        initializeDownloader()
        checkBatteryOptimization()
        checkPermission()
        checkAlwaysOnDisplay()
    }

    override fun onStart() {
        super.onStart()
        updateSystemBarAppearance()
        initializeBrowser()
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    private fun checkBatteryOptimization() {
        if (detectBatteryOptimization() && askForOptimization()) {
            showBatteryOptimizationDialog()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun checkAlwaysOnDisplay() {
        if (isDisplayAlwaysOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun detectBatteryOptimization(): Boolean {
        val packageName = getPackageName()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return !powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun showBatteryOptimizationDialog() {
        val dialog = BatteryOptimizationDialog()
        dialog.show(getSupportFragmentManager(), null)
    }

    private fun initializeBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            this,
            SessionToken(this, ComponentName(this, MediaService::class.java))
        ).buildAsync()
    }

    private fun releaseBrowser() {
        @Suppress("UNCHECKED_CAST")
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture!! as Future<out MediaController>)
    }

    fun getMediaBrowserListenableFuture(): ListenableFuture<MediaBrowser>? {
        return mediaBrowserListenableFuture
    }

    private fun initializeDownloader() {
        try {
            DownloadService.start(this, DownloaderService::class.java)
        } catch (e: IllegalStateException) {
            DownloadService.startForeground(this, DownloaderService::class.java)
        }
    }

    @Suppress("DEPRECATION")
    private fun configureEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = true
        }

        updateSystemBarAppearance()
    }

    private fun updateSystemBarAppearance() {
        val isLightTheme =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_YES

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = isLightTheme

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = isLightTheme
        }
    }

    companion object {
        private const val TAG = "BaseActivity"
    }
}
