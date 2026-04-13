package com.ecotracker.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecotracker.databinding.FragmentLeaderboardBinding
import com.ecotracker.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LeaderboardViewModel by viewModels()
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = LeaderboardAdapter()
        binding.rvLeaderboard.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@LeaderboardFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchLeaderboard()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.leaderboardState.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            binding.swipeRefresh.isRefreshing = false
                            binding.progressBar.visibility = View.GONE
                            val users = resource.data ?: emptyList()
                            adapter.submitList(users)
                            binding.emptyState.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                        }
                        is Resource.Error -> {
                            binding.swipeRefresh.isRefreshing = false
                            binding.progressBar.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                            com.google.android.material.snackbar.Snackbar.make(
                                binding.root,
                                resource.message,
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).show()
                        }
                        is Resource.Loading -> {
                            if (!binding.swipeRefresh.isRefreshing) {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            binding.emptyState.visibility = View.GONE
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
