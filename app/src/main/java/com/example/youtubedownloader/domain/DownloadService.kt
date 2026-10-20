package com.example.youtubedownloader.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.youtubedownloader.App
import com.example.youtubedownloader.R
import com.example.youtubedownloader.ui.MainActivity

class DownloadService : Service() {

    private lateinit var repository: DownloadRepository
    private lateinit var manager: DownloadManager
    private lateinit var nm: NotificationManager

    companion object {
        const val ACTION_PAUSE = "com.example.youtubedownloader.ACTION_PAUSE_SVC"
        const val ACTION_CANCEL = "com.example.youtubedownloader.ACTION_CANCEL_SVC"
        const val EXTRA_VIDEO_ID = "videoId"
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "ytd_downloads"
        var instance: DownloadService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val app = application as App
        repository = DownloadRepository(app.database.downloadDao())
        manager = DownloadManager(this, repository)
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.channel_downloads),
                    NotificationManager.IMPORTANCE_LOW)
            )
        }
        refreshNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val videoId = intent?.getStringExtra(EXTRA_VIDEO_ID)
        when (action) {
            ACTION_PAUSE -> videoId?.let { manager.pause(it) }
            ACTION_CANCEL -> videoId?.let { manager.cancel(it) }
        }
        refreshNotification()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun refreshNotification() {
        val active = repository.getActive() // Flow, but we need a snapshot; use a simple count below
        // build notification from last known state via repository is async; keep it simple
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_ready))
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        nm.notify(NOTIF_ID, builder.build())
    }
}
