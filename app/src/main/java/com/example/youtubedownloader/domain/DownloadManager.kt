package com.example.youtubedownloader.domain

import android.content.Context
import com.example.youtubedownloader.data.Download
import com.example.youtubedownloader.data.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownloadManager(
    private val context: Context,
    private val repository: DownloadRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tasks = mutableMapOf<String, DownloadTask>()

    private fun idOf(videoId: String, stream: Stream) = Download.idFor(videoId, stream.itag)

    fun start(videoId: String, stream: Stream, title: String, author: String, thumbnail: String?) {
        val id = idOf(videoId, stream)
        val running = tasks[id]
        if (running != null && running.isRunning()) return

        scope.launch {
            val existing = repository.getById(id)
            val download = existing ?: Download(
                id = id,
                videoId = videoId,
                title = title,
                author = author,
                thumbnail = thumbnail,
                itag = stream.itag,
                mimeType = stream.mimeType,
                qualityLabel = stream.qualityLabel,
                streamType = stream.type,
                bitrate = stream.bitrate,
                fileSize = stream.fileSize,
                status = "PENDING",
                url = stream.url ?: "",
                createdAt = System.currentTimeMillis()
            )

            repository.insert(download.copy(status = "DOWNLOADING", progress = 0, downloadedBytes = 0))

            val task = DownloadTask(
                context = context,
                download = download.copy(status = "DOWNLOADING"),
                onProgress = { progress, downloaded, total ->
                    repository.update(download.copy(
                        status = "DOWNLOADING",
                        progress = progress,
                        downloadedBytes = downloaded,
                        totalBytes = total
                    ))
                },
                onComplete = { filePath ->
                    repository.update(download.copy(
                        status = "COMPLETED",
                        progress = 100,
                        filePath = filePath
                    ))
                },
                onError = { _ ->
                    repository.update(download.copy(status = "FAILED"))
                },
                scope = scope
            )
            tasks[id] = task
            task.start()
        }
    }

    fun pause(id: String) {
        tasks[id]?.pause()
        repository.getById(id)?.let {
            repository.update(it.copy(status = "PAUSED"))
        }
    }

    fun resume(id: String) {
        val d = repository.getById(id) ?: return
        tasks[d.id]?.resume()
        repository.update(d.copy(status = "DOWNLOADING"))
    }

    fun cancel(id: String) {
        tasks[id]?.cancel()
        tasks.remove(id)
        repository.deleteById(id)
    }

    fun isRunning(id: String) = tasks[id]?.isRunning() == true

    fun statusFor(videoId: String, itag: Int): String? {
        return repository.getById(Download.idFor(videoId, itag))?.status
    }
}
