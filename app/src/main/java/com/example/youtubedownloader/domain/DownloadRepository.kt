package com.example.youtubedownloader.domain

import com.example.youtubedownloader.data.AppDatabase
import com.example.youtubedownloader.data.Download
import com.example.youtubedownloader.data.DownloadDao
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val dao: DownloadDao) {

    suspend fun insert(download: Download) = dao.insert(download)
    suspend fun update(download: Download) = dao.update(download)
    suspend fun deleteById(id: String) = dao.deleteById(id)
    suspend fun getById(id: String): Download? = dao.getById(id)

    fun getAll(): Flow<List<Download>> = dao.getAll()
    fun getActive(): Flow<List<Download>> = dao.getActive()
    fun getCompleted(): Flow<List<Download>> = dao.getCompleted()
    fun getByVideo(videoId: String): Flow<List<Download>> = dao.getByVideo(videoId)
}
