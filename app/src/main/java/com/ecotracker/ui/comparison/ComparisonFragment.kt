package com.ecotracker.ui.comparison

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.ecotracker.R
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.databinding.FragmentComparisonBinding
import com.ecotracker.databinding.ItemProductComparisonBinding
import com.ecotracker.utils.CarbonCalculator
import com.ecotracker.utils.toColor
import com.ecotracker.utils.toColorGradient
import com.ecotracker.utils.toDisplayLabel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class ComparisonFragment : Fragment() {

    private var _binding: FragmentComparisonBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ComparisonViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComparisonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectedProducts.observe(viewLifecycleOwner) { products ->
            if (products.size == 2) {
                showContentState()
                displayComparison(products[0], products[1])
            } else {
                showEmptyState()
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun displayComparison(p1: ScannedProduct, p2: ScannedProduct) {
        bindProductCard(binding.cardProduct1, p1)
        bindProductCard(binding.cardProduct2, p2)

        val c1 = p1.carbonFootprint
        val c2 = p2.carbonFootprint

        if (c1 != null && c2 != null) {
            val winner = when {
                c1 < c2 -> p1
                c2 < c1 -> p2
                else -> null
            }
            val diff = abs(c1 - c2)
            val maxVal = maxOf(c1, c2)
            val percent = if (maxVal > 0) (diff / maxVal) * 100 else 0.0

            if (winner != null) {
                binding.tvWinnerText.text = getString(R.string.label_winner, winner.productName)
                binding.tvWinnerText.setTextColor(resources.getColor(R.color.eco_green, null))
                binding.tvDifferenceInfo.text =
                    getString(R.string.label_impact_difference, diff) +
                        " (${getString(R.string.label_lower_impact, percent)})"
                binding.tvResultSupporting.text = getString(R.string.comparison_better_choice)
            } else {
                binding.tvWinnerText.text = getString(R.string.comparison_no_clear_winner)
                binding.tvWinnerText.setTextColor(resources.getColor(R.color.on_surface, null))
                binding.tvDifferenceInfo.text = getString(R.string.label_impact_difference, diff)
                binding.tvResultSupporting.text = getString(R.string.comparison_result_subtitle)
            }
        } else {
            binding.tvWinnerText.text = getString(R.string.msg_incomplete_comparison)
            binding.tvWinnerText.setTextColor(resources.getColor(R.color.on_surface, null))
            binding.tvDifferenceInfo.text = getString(R.string.comparison_missing_data)
            binding.tvResultSupporting.text = getString(R.string.comparison_result_subtitle)
        }
    }

    private fun bindProductCard(cardBinding: ItemProductComparisonBinding, product: ScannedProduct) {
        cardBinding.tvName.text = product.productName
        cardBinding.tvCarbon.text = CarbonCalculator.format(product.carbonFootprint)
        cardBinding.tvCarbon.setTextColor(product.carbonFootprint.toColorGradient())
        cardBinding.tvStatus.text =
            getString(R.string.comparison_status_format, product.status.toDisplayLabel())
        cardBinding.tvStatus.background.setTint(product.status.toColor())

        Glide.with(this)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_history_empty)
            .error(R.drawable.ic_history_empty)
            .into(cardBinding.ivProduct)
    }

    private fun showContentState() {
        binding.emptyState.visibility = View.GONE
        binding.contentState.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.contentState.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
