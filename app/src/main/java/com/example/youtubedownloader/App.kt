package com.example.youtubedownloader

import android.app.Application
import androidx.room.Room
import com.example.youtubedownloader.domain.DownloadManager
import com.example.youtubedownloader.domain.DownloadRepository

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    lateinit var database: AppDatabase
        private set
    val repository by lazy { DownloadRepository(database.downloadDao()) }
    val manager by lazy { DownloadManager(this, repository) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(
            this, AppDatabase::class.java, "ytd_database"
        ).fallbackToDestructiveMigration().build()
    }
}
