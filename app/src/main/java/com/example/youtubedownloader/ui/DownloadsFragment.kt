package com.example.youtubedownloader.ui

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
import com.example.youtubedownloader.R
import com.example.youtubedownloader.data.Download
import com.example.youtubedownloader.databinding.FragmentDownloadsBinding
import com.example.youtubedownloader.databinding.ItemDownloadBinding
import java.io.File
import kotlinx.coroutines.first
import kotlin.math.round

class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    private val dvm: DownloadViewModel by viewModels()

    private inner class DownloadAdapter(private val items: MutableList<Download> = mutableListOf()) :
        androidx.recyclerview.widget.RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

        fun setItems(list: List<Download>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        inner class ViewHolder(val binding: ItemDownloadBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val d = items[position]
            holder.binding.tvTitle.text = d.title
            holder.binding.tvQuality.text = "${d.qualityLabel ?: "غير محدد"} • ${d.streamType}"
            val size = if (d.fileSize != null) (d.fileSize!! / 1024.0 / 1024.0).round().toString() + " م.ب" else "—"
            holder.binding.tvSize.text = size
            holder.binding.pbProgress.progress = d.progress.coerceIn(0, 100)
            holder.binding.tvProgress.text = "${d.progress}%"

            val active = d.status == "DOWNLOADING" || d.status == "PAUSED" || d.status == "PENDING"
            holder.binding.btnPrimary.isEnabled = active
            holder.binding.btnSecondary.isEnabled = active

            when (d.status) {
                "DOWNLOADING" -> {
                    holder.binding.btnPrimary.text = "إيقاف"
                    holder.binding.btnPrimary.setOnClickListener { dvm.pause(d.id) }
                    holder.binding.btnSecondary.text = "إلغاء"
                    holder.binding.btnSecondary.setOnClickListener { dvm.cancel(d.id) }
                    holder.binding.pbProgress.visibility = View.VISIBLE
                }
                "PAUSED" -> {
                    holder.binding.btnPrimary.text = "استئناف"
                    holder.binding.btnPrimary.setOnClickListener { dvm.resume(d.id) }
                    holder.binding.btnSecondary.text = "إلغاء"
                    holder.binding.btnSecondary.setOnClickListener { dvm.cancel(d.id) }
                    holder.binding.pbProgress.visibility = View.VISIBLE
                }
                "COMPLETED" -> {
                    holder.binding.btnPrimary.text = "تشغيل"
                    holder.binding.btnPrimary.setOnClickListener {
                        if (d.filePath != null) openFile(d.filePath!!, d.mimeType)
                    }
                    holder.binding.btnSecondary.text = "حذف"
                    holder.binding.btnSecondary.setOnClickListener { dvm.cancel(d.id) }
                    holder.binding.pbProgress.visibility = View.GONE
                }
                "FAILED" -> {
                    holder.binding.btnPrimary.text = "إعادة المحاولة"
                    holder.binding.btnPrimary.setOnClickListener {
                        // استئناف من حيث توقف
                        dvm.resume(d.id)
                    }
                    holder.binding.btnSecondary.text = "حذف"
                    holder.binding.btnSecondary.setOnClickListener { dvm.cancel(d.id) }
                    holder.binding.pbProgress.visibility = View.GONE
                }
                else -> {
                    holder.binding.btnPrimary.text = "انتظار"
                    holder.binding.btnPrimary.isEnabled = false
                    holder.binding.btnSecondary.text = "إلغاء"
                    holder.binding.btnSecondary.setOnClickListener { dvm.cancel(d.id) }
                    holder.binding.pbProgress.visibility = View.VISIBLE
                }
            }
        }

        override fun getItemCount() = items.size
    }

    private val activeAdapter = DownloadAdapter()
    private val completedAdapter = DownloadAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvActive.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActive.adapter = activeAdapter
        binding.rvCompleted.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCompleted.adapter = completedAdapter

        binding.btnClearCompleted.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                dvm.completed.first().forEach { dvm.cancel(it.id) }
            }
        }
        // جمع دفقَي النشطة والمكتملة في كوروتينين منفصلين
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dvm.active.collect { list ->
                    activeAdapter.setItems(list)
                    binding.tvActiveCount.text = getString(R.string.label_active_count, list.size)
                    binding.llActive.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dvm.completed.collect { list ->
                    completedAdapter.setItems(list)
                    binding.tvCompletedCount.text = getString(R.string.label_completed_count, list.size)
                    binding.llCompleted.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
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

    companion object {
        fun newInstance() = DownloadsFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
