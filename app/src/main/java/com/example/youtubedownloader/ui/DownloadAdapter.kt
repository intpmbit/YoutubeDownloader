package com.example.youtubedownloader.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.youtubedownloader.R
import com.example.youtubedownloader.data.Download
import com.example.youtubedownloader.databinding.ItemDownloadBinding

class DownloadAdapter : ListAdapter<Download, DownloadAdapter.ViewHolder>(DownloadDiffCallback()) {

    inner class ViewHolder(val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val d = getItem(position)
        val b = holder.binding
        b.tvTitle.text = d.title
        b.tvQuality.text = "${d.qualityLabel ?: "غير محدد"} • ${d.streamType}"
        val size = if (d.fileSize != null) "%.1f م.ب".format(d.fileSize!! / 1024.0 / 1024.0) else "—"
        b.tvSize.text = size
        b.pbProgress.progress = d.progress.coerceIn(0, 100)
        b.tvProgress.text = "${d.progress}%"

        val active = d.status == "DOWNLOADING" || d.status == "PAUSED" || d.status == "PENDING"
        b.btnPrimary.isEnabled = active
        b.btnSecondary.isEnabled = active

        when (d.status) {
            "DOWNLOADING" -> {
                b.btnPrimary.text = "إيقاف"; b.btnPrimary.setOnClickListener { /* يُربط من fragment */ }
                b.btnSecondary.text = "إلغاء"; b.btnSecondary.setOnClickListener { /* يُربط */ }
                b.pbProgress.visibility = View.VISIBLE
            }
            "PAUSED" -> {
                b.btnPrimary.text = "استئناف"; b.btnPrimary.setOnClickListener { /* يُربط */ }
                b.btnSecondary.text = "إلغاء"; b.btnSecondary.setOnClickListener { /* يُربط */ }
                b.pbProgress.visibility = View.VISIBLE
            }
            "COMPLETED" -> {
                b.btnPrimary.text = "تشغيل"; b.btnPrimary.setOnClickListener { /* يُربط */ }
                b.btnSecondary.text = "حذف"; b.btnSecondary.setOnClickListener { /* يُربط */ }
                b.pbProgress.visibility = View.GONE
            }
            "FAILED" -> {
                b.btnPrimary.text = "إعادة المحاولة"; b.btnPrimary.setOnClickListener { /* يُربط */ }
                b.btnSecondary.text = "حذف"; b.btnSecondary.setOnClickListener { /* يُربط */ }
                b.pbProgress.visibility = View.GONE
            }
            else -> {
                b.btnPrimary.text = "انتظار"; b.btnPrimary.isEnabled = false
                b.btnSecondary.text = "إلغاء"; b.btnSecondary.setOnClickListener { /* يُربط */ }
                b.pbProgress.visibility = View.VISIBLE
            }
        }
    }

    class DownloadDiffCallback : DiffUtil.ItemCallback<Download>() {
        override fun areItemsTheSame(oldItem: Download, newItem: Download) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Download, newItem: Download) = oldItem == newItem
    }
}
