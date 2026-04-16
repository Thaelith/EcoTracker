package com.ecotracker.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecotracker.R
import com.ecotracker.databinding.FragmentHistoryBinding
import com.ecotracker.ui.comparison.ComparisonViewModel
import com.ecotracker.utils.gone
import com.ecotracker.utils.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private val comparisonViewModel: ComparisonViewModel by activityViewModels()
    private lateinit var adapter: ProductHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        observeComparison()
    }

    private fun setupRecyclerView() {
        adapter = ProductHistoryAdapter(
            onItemClick = { /* Navigate to detail if needed */ },
            onDeleteClick = { viewModel.deleteProduct(it) },
            onCompareClick = { product -> 
                comparisonViewModel.toggleProductSelection(product)
            }
        )
        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.allProducts.observe(viewLifecycleOwner) { products ->
            if (products.isEmpty()) {
                binding.emptyState.visible()
                binding.recyclerHistory.gone()
            } else {
                binding.emptyState.gone()
                binding.recyclerHistory.visible()
                adapter.submitList(products)
            }
        }
    }

    private fun observeComparison() {
        comparisonViewModel.selectedProducts.observe(viewLifecycleOwner) { selected ->
            adapter.updateSelection(selected)
            when (selected.size) {
                1 -> {
                    binding.tvComparisonStateTitle.text =
                        getString(R.string.history_compare_selected_one)
                    binding.tvComparisonStateSubtitle.text =
                        getString(R.string.history_compare_cta)
                }
                2 -> {
                    binding.tvComparisonStateTitle.text =
                        getString(R.string.history_compare_selected_two)
                    binding.tvComparisonStateSubtitle.text =
                        getString(R.string.history_compare_ready)
                    findNavController().navigate(R.id.action_historyFragment_to_comparisonFragment)
                }
                else -> {
                    binding.tvComparisonStateTitle.text =
                        getString(R.string.history_compare_idle)
                    binding.tvComparisonStateSubtitle.text =
                        getString(R.string.history_compare_cta)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset selection when returning to history
        comparisonViewModel.clearSelection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
