package com.ecotracker.ui.comparison

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ecotracker.data.local.ScannedProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ComparisonViewModel @Inject constructor() : ViewModel() {

    private val _selectedProducts = MutableLiveData<List<ScannedProduct>>(emptyList())
    val selectedProducts: LiveData<List<ScannedProduct>> = _selectedProducts

    fun toggleProductSelection(product: ScannedProduct) {
        val current = _selectedProducts.value.orEmpty().toMutableList()
        val existing = current.find { it.id == product.id }

        if (existing != null) {
            current.remove(existing)
        } else {
            if (current.size < 2) {
                current.add(product)
            } else {
                current[1] = product
            }
        }
        _selectedProducts.value = current
    }

    fun clearSelection() {
        _selectedProducts.value = emptyList()
    }
}
