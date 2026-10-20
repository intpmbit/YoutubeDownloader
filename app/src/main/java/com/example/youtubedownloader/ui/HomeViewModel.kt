package com.example.youtubedownloader.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.youtubedownloader.data.VideoInfo
import com.example.youtubedownloader.data.YouTubeApi
import kotlinx.coroutines.launch

sealed class HomeResult {
    object Loading : HomeResult()
    data class Success(val data: VideoInfo) : HomeResult()
    data class Error(val message: String) : HomeResult()
}

class HomeViewModel : ViewModel() {

    private val _result = MutableLiveData<HomeResult>(HomeResult.Loading)
    val result: LiveData<HomeResult> = _result

    fun search(videoIdOrUrl: String) = viewModelScope.launch {
        _result.value = HomeResult.Loading
        val id = extractId(videoIdOrUrl)
        if (id.isEmpty()) {
            _result.value = HomeResult.Error("أدخل رابطاً صحيحاً لمقطع يوتيوب أو معرف الفيديو")
            return@launch
        }
        try {
            val info = YouTubeApi.fetchVideoInfo(id)
            _result.value = HomeResult.Success(info)
        } catch (e: Exception) {
            _result.value = HomeResult.Error(e.message ?: "فشل جلب معلومات الفيديو")
        }
    }

    private fun extractId(input: String): String {
        val trimmed = input.trim()
        return if (trimmed.contains("youtu")) {
            trimmed
                .replace("https://www.youtube.com/watch?v=", "")
                .replace("http://www.youtube.com/watch?v=", "")
                .replace("https://youtu.be/", "")
                .replace("http://youtu.be/", "")
                .split("&").first()
                .trim()
        } else trimmed
    }
}
