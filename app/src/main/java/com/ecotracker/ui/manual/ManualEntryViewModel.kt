package com.ecotracker.ui.manual

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.repository.EcoTrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val repository: EcoTrackerRepository
) : ViewModel() {

    private val _saveState = MutableLiveData<Boolean>()
    val saveState: LiveData<Boolean> = _saveState

    fun saveProduct(
        barcode: String,
        productName: String,
        brand: String,
        category: String,
        imageUrl: String
    ) {
        val product = ScannedProduct(
            barcode = barcode,
            productName = productName,
            brand = brand,
            categories = category,
            imageUrl = imageUrl,
            ecoScore = "N/A",
            ecoScoreValue = 0,
            carbonFootprint = 0.0
        )

        viewModelScope.launch {
            val id = repository.saveProduct(product)
            _saveState.postValue(id > 0)
        }
    }

    fun onSaveConsumed() {
        _saveState.value = false
    }
}
