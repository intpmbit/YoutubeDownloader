package com.example.youtubedownloader.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.youtubedownloader.App
import com.example.youtubedownloader.data.Download
import com.example.youtubedownloader.data.Stream
import kotlinx.coroutines.flow.Flow

class DownloadViewModel : ViewModel() {

    private val app = App.instance
    private val repository = app.repository
    private val manager = app.manager

    val active: Flow<List<Download>> = repository.getActive()
    val completed: Flow<List<Download>> = repository.getCompleted()

    fun start(videoId: String, stream: Stream, title: String, author: String, thumbnail: String?) {
        manager.start(videoId, stream, title, author, thumbnail)
    }

    fun pause(id: String) = manager.pause(id)
    fun resume(id: String) = manager.resume(id)
    fun cancel(id: String) = manager.cancel(id)
    fun statusFor(videoId: String, itag: Int): String? = manager.statusFor(videoId, itag)
}
