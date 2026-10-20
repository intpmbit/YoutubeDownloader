package com.example.youtubedownloader.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.youtubedownloader.R
import com.example.youtubedownloader.data.Download
import com.example.youtubedownloader.data.Stream
import com.example.youtubedownloader.databinding.ItemStreamBinding

class StreamAdapter(
    private val context: android.content.Context,
    private var videoId: String = "",
    private val onAction: (Stream) -> Unit,
    private val getStatus: (Int) -> String?
) : ListAdapter<Stream, StreamAdapter.ViewHolder>(StreamDiffCallback()) {

    private val activeById = mutableMapOf<String, Download>()

    fun submitList(streams: List<Stream>, vid: String) {
        videoId = vid
        super.submitList(streams)
    }

    fun setActive(list: List<Download>) {
        activeById.clear()
        list.forEach { activeById[it.id] = it }
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemStreamBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemStreamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stream = getItem(position)
        val id = com.example.youtubedownloader.data.Download.idFor(videoId, stream.itag)
        val status = getStatus(stream.itag) ?: activeById[id]?.status

        holder.binding.tvQuality.text = stream.qualityLabel ?: "جودة غير محددة"
        val size = if (stream.fileSize != null) {
            "%.1f م.ب".format(stream.fileSize!! / 1024.0 / 1024.0)
        } else {
            "حجم غير محدد"
        }
        holder.binding.tvSize.text = "$size • ${stream.type}"
        holder.binding.tvType.text = if (stream.type == "VIDEO") "فيديو" else "صوت"

        val pb: ProgressBar = holder.binding.pbStream
        when (status) {
            "DOWNLOADING" -> {
                holder.binding.btnAction.text = "إيقاف"
                holder.binding.btnAction.setOnClickListener {
                    val d = activeById[id]
                    if (d != null) onAction(stream) // يُستدعى الإيقاف من fragment حسب الحالة
                }
                pb.visibility = View.VISIBLE
                holder.binding.tvProgress.visibility = View.VISIBLE
                holder.binding.tvProgress.text = "جاري التحميل…"
            }
            "PAUSED" -> {
                holder.binding.btnAction.text = "استئناف"
                holder.binding.btnAction.setOnClickListener { onAction(stream) }
                pb.visibility = View.GONE
                holder.binding.tvProgress.visibility = View.GONE
            }
            "COMPLETED" -> {
                holder.binding.btnAction.text = "تم ✓"
                holder.binding.btnAction.isEnabled = false
                pb.visibility = View.GONE
                holder.binding.tvProgress.visibility = View.GONE
            }
            "FAILED" -> {
                holder.binding.btnAction.text = "إعادة المحاولة"
                holder.binding.btnAction.setOnClickListener { onAction(stream) }
                pb.visibility = View.GONE
                holder.binding.tvProgress.visibility = View.GONE
            }
            else -> {
                holder.binding.btnAction.text = "تحميل"
                holder.binding.btnAction.setOnClickListener { onAction(stream) }
                pb.visibility = View.GONE
                holder.binding.tvProgress.visibility = View.GONE
            }
        }
    }

    class StreamDiffCallback : DiffUtil.ItemCallback<Stream>() {
        override fun areItemsTheSame(oldItem: Stream, newItem: Stream) = oldItem.itag == newItem.itag
        override fun areContentsTheSame(oldItem: Stream, newItem: Stream) = oldItem == newItem
    }
}
