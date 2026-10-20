package com.example.youtubedownloader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.youtubedownloader.R
import com.example.youtubedownloader.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvVersion.text = "الإصدار 1.0.0"
        binding.btnClear.setOnClickListener {
            // مسح السجلات المكتملة (يُنفَّذ عبر DownloadViewModel في مستقبل التحديث)
            android.widget.Toast.makeText(requireContext(), "سيتم دعم مسح السجلات في التحديث القادم", android.widget.Toast.LENGTH_SHORT).show()
        }
        binding.btnAbout.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "تطبيق تنزيل فيديوهات يوتيوب - نموذج تعليمي", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
