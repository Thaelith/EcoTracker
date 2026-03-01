package com.ecotracker.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.repository.EcoTrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: EcoTrackerRepository
) : ViewModel() {

    val allProducts: LiveData<List<ScannedProduct>> = repository.getAllProducts().asLiveData()

    fun deleteProduct(product: ScannedProduct) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }
}
