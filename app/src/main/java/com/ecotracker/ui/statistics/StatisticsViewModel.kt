package com.ecotracker.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.ecotracker.data.repository.EcoTrackerRepository
import com.ecotracker.utils.startOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: EcoTrackerRepository
) : ViewModel() {

    val totalScannedCount: LiveData<Int> = repository.getTotalScannedCount().asLiveData()

    val totalCarbonToday: LiveData<Double?> =
        repository.getTotalCarbonSince(startOfDay(0)).asLiveData()

    val totalCarbonThisWeek: LiveData<Double?> =
        repository.getTotalCarbonSince(startOfDay(7)).asLiveData()

    val weeklyProducts = repository.getProductsSince(startOfDay(7)).asLiveData()
}
