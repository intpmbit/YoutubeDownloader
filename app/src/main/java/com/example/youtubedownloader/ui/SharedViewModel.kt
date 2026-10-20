package com.example.youtubedownloader.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.youtubedownloader.data.VideoInfo

class SharedViewModel : ViewModel() {
    private val _video = MutableLiveData<VideoInfo?>()
    val video: LiveData<VideoInfo?> = _video

    fun setVideo(video: VideoInfo?) = _video.value = video
    fun clear() = _video.value = null
}
