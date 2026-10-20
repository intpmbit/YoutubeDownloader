package com.example.youtubedownloader.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // يمكن توسيعه لاحقاً لإشعار المستخدم بإكمال التحميل
    }
}
