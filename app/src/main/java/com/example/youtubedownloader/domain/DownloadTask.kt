package com.example.youtubedownloader.domain

import android.content.Context
import android.os.Environment
import com.example.youtubedownloader.data.Download
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

typealias ProgressCallback = (progress: Int, downloaded: Long, total: Long) -> Unit

class DownloadTask(
    private val context: Context,
    private val download: Download,
    private val onProgress: ProgressCallback,
    private val onComplete: (filePath: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val scope: CoroutineScope
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private var job: Job? = null
    private var cancelled = false
    private var paused = false

    fun isRunning() = job?.isActive == true

    fun start() {
        cancelled = false
        paused = false
        job = scope.launch { downloadFile() }
    }

    fun pause() = paused = true

    fun resume() = paused = false

    fun cancel() {
        cancelled = true
        paused = false
        job?.cancel()
    }

    private suspend fun downloadFile() {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: run {
            onError("لا يمكن الوصول لمجلد التحميل")
            return
        }
        val ext = extensionFor(download.mimeType)
        val file = File(dir, "${download.videoId}_${download.itag}.$ext")

        val request = Request.Builder().url(download.url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            onError("فشل الاتصال بالخادم (${response.code})")
            return
        }
        val body = response.body ?: run {
            onError("استجابة فارغة من الخادم")
            return
        }

        val contentLength = body.contentLength()
        val totalBytes = if (contentLength != -1L) contentLength else (download.totalBytes ?: 0L)
        var already = download.downloadedBytes

        val source = body.source()
        if (already > 0) {
            source.skip(already.toLong())
        }

        val out = FileOutputStream(file, already > 0)
        val buffered = BufferedOutputStream(out)
        val buffer = ByteArray(8192)
        var read = already
        var lastNotify = System.currentTimeMillis()

        try {
            while (true) {
                if (cancelled) return
                while (paused) {
                    delay(200)
                    if (cancelled) return
                }
                val count = source.read(buffer)
                if (count == -1) break
                buffered.write(buffer, 0, count)
                read += count

                val progress = if (totalBytes > 0) ((read * 100) / totalBytes).toInt().coerceAtMost(100) else 0
                val now = System.currentTimeMillis()
                if (now - lastNotify > 300) {
                    lastNotify = now
                    onProgress(progress, read, totalBytes)
                }
            }
            buffered.flush()
            if (!cancelled) {
                onComplete(file.absolutePath)
            }
        } catch (e: Exception) {
            if (!cancelled) onError(e.message ?: "فشل التحميل")
        } finally {
            buffered.close()
            body.close()
        }
    }

    private fun extensionFor(mimeType: String): String {
        return when {
            mimeType.contains("mp4", ignoreCase = true) -> "mp4"
            mimeType.contains("webm", ignoreCase = true) -> "webm"
            mimeType.contains("audio/mp4", ignoreCase = true) || mimeType.contains("m4a", ignoreCase = true) -> "m4a"
            mimeType.contains("audio/webm", ignoreCase = true) -> "webm"
            mimeType.contains("ogg", ignoreCase = true) -> "ogg"
            else -> "dat"
        }
    }
}
