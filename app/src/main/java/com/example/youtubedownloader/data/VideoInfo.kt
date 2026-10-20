package com.example.youtubedownloader.data

data class VideoInfo(
    val videoId: String,
    val title: String,
    val author: String,
    val lengthSeconds: Int,
    val thumbnail: String?,
    val streams: List<Stream>
)

data class Stream(
    val itag: Int,
    val mimeType: String,
    val type: String, // "VIDEO" or "AUDIO"
    val qualityLabel: String?,
    val bitrate: Long,
    val width: Int,
    val height: Int,
    val fileSize: Long?,
    val url: String?, // الرابط المباشر إن توفّر
    val cipher: String? // signatureCipher إن كان الرابط مشفراً
)
