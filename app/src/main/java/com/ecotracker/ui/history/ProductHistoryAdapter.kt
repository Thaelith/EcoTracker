package com.ecotracker.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.databinding.ItemProductHistoryBinding
import com.ecotracker.utils.CarbonCalculator
import com.ecotracker.utils.ecoScoreColor
import com.ecotracker.utils.toFormattedDate

class ProductHistoryAdapter(
    private val onItemClick: (ScannedProduct) -> Unit,
    private val onDeleteClick: (ScannedProduct) -> Unit
) : ListAdapter<ScannedProduct, ProductHistoryAdapter.ViewHolder>(DiffCallback) {

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
                tvName.text = product.productName
                tvBrand.text = product.brand
                tvEcoScore.text = product.ecoScore
                tvEcoScore.setBackgroundColor(product.ecoScore.ecoScoreColor())
                tvCarbon.text = CarbonCalculator.format(product.carbonFootprint)
                tvDate.text = product.timestamp.toFormattedDate()

                root.setOnClickListener { onItemClick(product) }
                btnDelete.setOnClickListener { onDeleteClick(product) }
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
