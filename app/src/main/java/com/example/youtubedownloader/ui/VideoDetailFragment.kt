package com.example.youtubedownloader.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.youtubedownloader.App
import com.example.youtubedownloader.R
import com.example.youtubedownloader.data.Download
import com.example.youtubedownloader.data.Stream
import com.example.youtubedownloader.databinding.FragmentVideoDetailBinding
import com.example.youtubedownloader.domain.DownloadTask
import com.example.youtubedownloader.data.YouTubeApi
import java.io.File
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoDetailFragment : Fragment() {

    private var _binding: FragmentVideoDetailBinding? = null
    private val binding get() = _binding!!

    private val shared: SharedViewModel by activityViewModels()
    private val dvm: DownloadViewModel by viewModels()

    private val adapter = StreamAdapter(
        context = requireContext(),
        streams = emptyList(),
        videoId = "",
        onAction = { stream: Stream -> onStreamAction(stream) },
        getStatus = { itag: Int -> statusFor(itag) }
    )

    private val activeById = mutableMapOf<String, Download>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentVideoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvStreams.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStreams.adapter = adapter

        binding.btnBack.setOnClickListener {
            (activity as? MainActivity)?.showHome()
            shared.clear()
        }

        // تحديث أشرطة التقدم للتحميلات النشطة
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dvm.active.collect { list ->
                    activeById.clear()
                    list.forEach { activeById[it.id] = it }
                    adapter.notifyDataSetChanged()
                }
            }
        }

        shared.video.observe(viewLifecycleOwner) { video ->
            if (video == null) return@observe
            binding.tvTitle.text = video.title
            binding.tvAuthor.text = video.author
            binding.tvDuration.text = formatDuration(video.lengthSeconds)
            binding.tvQualityCount.text = "${video.streams.size} خيار"
            adapter.submitList(video.streams, video.videoId)
            loadThumbnail(video.thumbnail)
        }
    }

    private fun statusFor(itag: Int): String? {
        val vid = shared.video.value?.videoId ?: return null
        return activeById[Download.idFor(vid, itag)]?.status
    }

    private fun onStreamAction(stream: Stream) {
        val video = shared.video.value ?: return
        val id = Download.idFor(video.videoId, stream.itag)
        val status = statusFor(stream.itag)
        when (status) {
            "DOWNLOADING" -> dvm.pause(id)
            "PAUSED" -> dvm.resume(id)
            "COMPLETED" -> {
                val d = activeById[id]
                if (d != null && d.filePath != null) openFile(d.filePath!!, stream.mimeType)
            }
            else -> {
                if (stream.url == null) {
                    Toast.makeText(requireContext(), "الرابط المباشر غير متوفر لهذه الجودة حالياً", Toast.LENGTH_LONG).show()
                    return
                }
                dvm.start(video.videoId, stream, video.title, video.author, video.thumbnail)
            }
        }
    }

    private fun openFile(filePath: String, mimeType: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "الملف غير موجود", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "لا يوجد تطبيق لتشغيل هذا النوع من الملفات", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadThumbnail(url: String?) {
        if (url == null) return
        lifecycleScope.launch {
            try {
                val response = YouTubeApi.client.newCall(
                    okhttp3.Request.Builder().url(url).build()
                ).execute()
                val bytes = response.body?.bytes() ?: return@launch
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                withContext(Dispatchers.Main) {
                    binding.ivThumbnail.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // تُعرض صورة placeholder افتراضية
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    companion object {
        fun newInstance() = VideoDetailFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
