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

    private val _savedState = MutableLiveData<Resource<Unit>?>()
    val savedState: LiveData<Resource<Unit>?> = _savedState

    private val _showManualEntry = MutableLiveData<String?>()
    val showManualEntry: LiveData<String?> = _showManualEntry

    private val _showInputPrompt = MutableLiveData<String?>()
    val showInputPrompt: LiveData<String?> = _showInputPrompt

    fun lookupBarcode(barcode: String) {
        _scanState.value = Resource.Loading
        viewModelScope.launch {
            val cached = repository.getProductByBarcode(barcode)
            if (cached != null && !cached.isWeak()) {
                _scanState.postValue(Resource.Success(cached))
                return@launch
            }

            when (val result = repository.fetchProductByBarcode(barcode)) {
                is Resource.NeedsInput -> {
                    _showInputPrompt.postValue(result.barcode)
                }
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

    fun estimateWithUserInput(barcode: String, userHint: String) {
        _scanState.value = Resource.Loading
        viewModelScope.launch {
            val result = repository.estimateWithUserPrompt(barcode, userHint)
            _scanState.postValue(result)
        }
    }

    fun saveProduct(product: ScannedProduct) {
        _savedState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val remoteId = "${product.barcode}_${product.timestamp}"
                repository.saveProduct(product, remoteId)
                _savedState.postValue(Resource.Success(Unit))
            } catch (e: Exception) {
                _savedState.postValue(Resource.Error(e.message ?: "Failed to save product"))
            }
        }
    }

    fun onManualEntryNavigated() {
        _showManualEntry.value = null
    }

    fun onInputPromptShown() {
        _showInputPrompt.value = null
    }

    fun onProductSavedToastShown() {
        _savedState.value = null
    }

    fun resetState() {
        _scanState.value = null
        _savedState.value = null
        _showManualEntry.value = null
        _showInputPrompt.value = null
    }
}
