package com.ecotracker.ui.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.repository.EcoTrackerRepository
import com.ecotracker.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: EcoTrackerRepository
) : ViewModel() {

    private val _scanState = MutableLiveData<Resource<ScannedProduct>>()
    val scanState: LiveData<Resource<ScannedProduct>> = _scanState

    private val _savedState = MutableLiveData<Boolean>()
    val savedState: LiveData<Boolean> = _savedState

    private val _showManualEntry = MutableLiveData<String?>()
    val showManualEntry: LiveData<String?> = _showManualEntry

    fun lookupBarcode(barcode: String) {
        _scanState.value = Resource.Loading
        viewModelScope.launch {
            val cached = repository.getProductByBarcode(barcode)
            if (cached != null) {
                _scanState.postValue(Resource.Success(cached))
                return@launch
            }

            when (val result = repository.fetchProductByBarcode(barcode)) {
                is Resource.Error -> {
                    if (result.data is String) { // Barcode is in the data field
                        _showManualEntry.postValue(result.data)
                    }
                    _scanState.postValue(result)
                }
                else -> _scanState.postValue(result)
            }
        }
    }

    fun saveProduct(product: ScannedProduct) {
        viewModelScope.launch {
            val id = repository.saveProduct(product)
            _savedState.value = id > 0
        }
    }

    fun onManualEntryNavigated() {
        _showManualEntry.value = null
    }

    fun onProductSavedToastShown() {
        _savedState.value = false
    }

    fun resetState() {
        _scanState.value = null
        _savedState.value = false
        _showManualEntry.value = null
    }
}
