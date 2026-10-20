package com.example.youtubedownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class Download(
    @PrimaryKey val id: String, // "${videoId}_${itag}"
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnail: String?,
    val itag: Int,
    val mimeType: String,
    val qualityLabel: String?,
    val streamType: String, // "VIDEO" or "AUDIO"
    val bitrate: Long,
    val fileSize: Long?,
    val status: String, // PENDING / DOWNLOADING / PAUSED / COMPLETED / FAILED
    val progress: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val filePath: String? = null,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()

    companion object {
        fun idFor(videoId: String, itag: Int) = "$videoId_$itag"
    }
)
