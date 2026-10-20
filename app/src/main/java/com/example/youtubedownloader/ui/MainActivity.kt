package com.example.youtubedownloader.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.youtubedownloader.R
import com.example.youtubedownloader.data.VideoInfo
import com.example.youtubedownloader.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val shared: SharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment.newInstance())
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> swap(HomeFragment.newInstance())
                R.id.nav_downloads -> swap(DownloadsFragment.newInstance())
                R.id.nav_settings -> swap(SettingsFragment.newInstance())
                else -> false
            }
        }
    }

    fun showDetail(video: VideoInfo) {
        shared.setVideo(video)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, VideoDetailFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    fun showHome() {
        shared.clear()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment.newInstance())
        .commit()
    }

    private fun swap(fragment: androidx.fragment.app.Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
