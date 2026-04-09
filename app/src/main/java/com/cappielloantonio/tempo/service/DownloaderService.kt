package com.cappielloantonio.tempo.service

import android.app.Notification
import android.content.Context
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Requirements.RequirementFlags
import androidx.media3.exoplayer.scheduler.Scheduler
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.util.DownloadUtil

@UnstableApi
class DownloaderService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.exo_download_notification_channel_name,
    0
) {
    override fun getDownloadManager(): DownloadManager {
        val downloadManager = DownloadUtil.getDownloadManager(this)!!
        val downloadNotificationHelper = DownloadUtil.getDownloadNotificationHelper(this)!!
        downloadManager.addListener(
            TerminalStateNotificationHelper(
                this,
                downloadNotificationHelper,
                FOREGROUND_NOTIFICATION_ID + 1
            )
        )
        return downloadManager
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, JOB_ID)
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        @RequirementFlags notMetRequirements: @RequirementFlags Int
    ): Notification {
        return DownloadUtil.getDownloadNotificationHelper(this).buildProgressNotification(
            this,
            R.drawable.ic_download,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }

    private class TerminalStateNotificationHelper(
        context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        private var nextNotificationId: Int
    ) : DownloadManager.Listener {
        private val context: Context

        private val successfulDownloadGroupNotification: Notification
        private val failedDownloadGroupNotification: Notification

        private val successfulDownloadGroupNotificationId: Int
        private val failedDownloadGroupNotificationId: Int

        init {
            this.context = context.getApplicationContext()

            successfulDownloadGroupNotification = DownloadUtil.buildGroupSummaryNotification(
                this.context,
                DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
                DownloadUtil.DOWNLOAD_NOTIFICATION_SUCCESSFUL_GROUP,
                R.drawable.ic_check_circle,
                "Downloads completed"
            )

            failedDownloadGroupNotification = DownloadUtil.buildGroupSummaryNotification(
                this.context,
                DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
                DownloadUtil.DOWNLOAD_NOTIFICATION_FAILED_GROUP,
                R.drawable.ic_error,
                "Downloads failed"
            )

            successfulDownloadGroupNotificationId = nextNotificationId++
            failedDownloadGroupNotificationId = nextNotificationId++
        }

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            var notification: Notification?

            if (download.state == Download.STATE_COMPLETED) {
                notification = notificationHelper.buildDownloadCompletedNotification(
                    context,
                    R.drawable.ic_check_circle,
                    null,
                    DownloaderManager.Companion.getDownloadNotificationMessage(download.request.id)
                )
                notification = Notification.Builder.recoverBuilder(context, notification)
                    .setGroup(DownloadUtil.DOWNLOAD_NOTIFICATION_SUCCESSFUL_GROUP).build()
                NotificationUtil.setNotification(
                    this.context,
                    successfulDownloadGroupNotificationId,
                    successfulDownloadGroupNotification
                )
                DownloaderManager.Companion.updateRequestDownload(download)
            } else if (download.state == Download.STATE_FAILED) {
                notification = notificationHelper.buildDownloadFailedNotification(
                    context,
                    R.drawable.ic_error,
                    null,
                    DownloaderManager.Companion.getDownloadNotificationMessage(download.request.id)
                )
                notification = Notification.Builder.recoverBuilder(context, notification)
                    .setGroup(DownloadUtil.DOWNLOAD_NOTIFICATION_FAILED_GROUP).build()
                NotificationUtil.setNotification(
                    this.context,
                    failedDownloadGroupNotificationId,
                    failedDownloadGroupNotification
                )
            } else {
                return
            }

            NotificationUtil.setNotification(context, nextNotificationId++, notification)
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            DownloaderManager.Companion.removeRequestDownload(download)
        }
    }

    companion object {
        private const val JOB_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }
}
