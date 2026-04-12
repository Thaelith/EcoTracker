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
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class ComparisonFragment : Fragment() {

    private var _binding: FragmentComparisonBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ComparisonViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComparisonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel.selectedProducts.observe(viewLifecycleOwner) { products ->
            if (products.size == 2) {
                displayComparison(products[0], products[1])
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
            val winner = if (c1 <= c2) p1 else p2
            val diff = abs(c1 - c2)
            val maxVal = maxOf(c1, c2)
            val percent = if (maxVal > 0) (diff / maxVal) * 100 else 0.0

            binding.tvWinnerText.text = getString(R.string.label_winner, winner.productName)
            binding.tvWinnerText.setTextColor(resources.getColor(R.color.eco_green, null))
            binding.tvDifferenceInfo.text = getString(R.string.label_impact_difference, diff) + 
                " (" + getString(R.string.label_lower_impact, percent) + ")"
        } else {
            binding.tvWinnerText.text = getString(R.string.msg_incomplete_comparison)
            binding.tvWinnerText.setTextColor(resources.getColor(R.color.on_surface_variant, null))
            binding.tvDifferenceInfo.text = ""
        }
    }

    private fun bindProductCard(cardBinding: ItemProductComparisonBinding, product: ScannedProduct) {
        cardBinding.tvName.text = product.productName
        cardBinding.tvCarbon.text = CarbonCalculator.format(product.carbonFootprint)
        cardBinding.tvCarbon.setTextColor(product.carbonFootprint.toColorGradient())
        
        cardBinding.tvStatus.text = product.status.name
        cardBinding.tvStatus.setBackgroundColor(product.status.toColor())

        Glide.with(this)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_history_empty)
            .error(R.drawable.ic_history_empty)
            .into(cardBinding.ivProduct)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
