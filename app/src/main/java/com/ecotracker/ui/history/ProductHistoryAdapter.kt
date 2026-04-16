package com.ecotracker.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecotracker.R
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.databinding.ItemProductHistoryBinding
import com.ecotracker.utils.CarbonCalculator
import com.ecotracker.utils.ecoScoreColor
import com.ecotracker.utils.toColorGradient
import com.ecotracker.utils.toFormattedDate

class ProductHistoryAdapter(
    private val onItemClick: (ScannedProduct) -> Unit,
    private val onDeleteClick: (ScannedProduct) -> Unit,
    private val onCompareClick: (ScannedProduct) -> Unit
) : ListAdapter<ScannedProduct, ProductHistoryAdapter.ViewHolder>(DiffCallback) {

    private var selectedBarcodes: Set<String> = emptySet()

    fun updateSelection(selected: List<ScannedProduct>) {
        selectedBarcodes = selected.map { it.barcode }.toSet()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProductHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    inner class ViewHolder(private val binding: ItemProductHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ScannedProduct) {
            binding.apply {
                val context = root.context
                val isSelected = selectedBarcodes.contains(product.barcode)
                tvName.text = product.productName
                tvBrand.text = product.brand
                tvEcoScore.text = product.ecoScore
                tvEcoScore.setBackgroundColor(product.ecoScore.ecoScoreColor())
                tvCarbon.text = CarbonCalculator.format(product.carbonFootprint)
                tvCarbon.setTextColor(product.carbonFootprint.toColorGradient())
                tvDate.text = product.timestamp.toFormattedDate()
                root.cardElevation = if (isSelected) 4f else 2f
                root.strokeWidth = if (isSelected) {
                    context.resources.getDimensionPixelSize(R.dimen.active_user_stroke_width)
                } else {
                    1
                }
                root.setStrokeColor(
                    ContextCompat.getColor(
                        context,
                        if (isSelected) R.color.md_theme_primary else R.color.md_theme_surface_variant
                    )
                )
                btnCompare.imageTintList = ContextCompat.getColorStateList(
                    context,
                    if (isSelected) R.color.md_theme_primary else R.color.on_surface_variant
                )

                root.setOnClickListener { onItemClick(product) }
                btnDelete.setOnClickListener { onDeleteClick(product) }
                btnCompare.setOnClickListener { onCompareClick(product) }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScannedProduct>() {
        override fun areItemsTheSame(oldItem: ScannedProduct, newItem: ScannedProduct): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ScannedProduct, newItem: ScannedProduct): Boolean =
            oldItem == newItem
    }
}
