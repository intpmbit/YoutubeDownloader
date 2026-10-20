package com.example.youtubedownloader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.youtubedownloader.R
import com.example.youtubedownloader.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val shared: SharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchBtn.setOnClickListener {
            val input = binding.urlInput.text.toString()
            viewModel.search(input)
        }

        binding.resultCard.setOnClickListener {
            shared.video.value?.let { (activity as? MainActivity)?.showDetail(it) }
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is HomeResult.Loading -> {
                    binding.progress.visibility = View.VISIBLE
                    binding.resultCard.visibility = View.GONE
                }
                is HomeResult.Success -> {
                    binding.progress.visibility = View.GONE
                    binding.resultCard.visibility = View.VISIBLE
                    binding.tvResultTitle.text = result.data.title
                    binding.tvResultAuthor.text = result.data.author
                    binding.tvResultStreams.text =
                        "${result.data.streams.size} خيار جودة متاح"
                    shared.setVideo(result.data)
                }
                is HomeResult.Error -> {
                    binding.progress.visibility = View.GONE
                    binding.resultCard.visibility = View.GONE
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
