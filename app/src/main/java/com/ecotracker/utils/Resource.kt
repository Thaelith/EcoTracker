package com.ecotracker.utils

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val data: Any? = null) : Resource<Nothing>()
    data class NeedsInput(val barcode: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()

    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError get() = this is Error
}
