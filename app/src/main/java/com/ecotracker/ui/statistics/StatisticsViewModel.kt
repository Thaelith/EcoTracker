package com.ecotracker.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.repository.ProductRepository
import com.ecotracker.data.repository.UserRepository
import com.ecotracker.utils.startOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class WeeklyChartBar(
    val label: String,
    val totalCarbonKg: Float
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val totalScannedCount: LiveData<Int> = userRepository.getTotalScannedCount().asLiveData()

    val totalCarbonToday: LiveData<Double?> =
        userRepository.getTotalCarbonSince(startOfDay(0)).asLiveData()

    val totalCarbonThisWeek: LiveData<Double?> =
        userRepository.getTotalCarbonSince(startOfDay(6)).asLiveData()

    val weeklyChart: LiveData<List<WeeklyChartBar>> =
        productRepository.getProductsSince(startOfDay(6))
            .map(::buildWeeklyChart)
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .asLiveData()

    private fun buildWeeklyChart(products: List<ScannedProduct>): List<WeeklyChartBar> {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val groupCalendar = Calendar.getInstance()
        val groupedTotals = products.groupBy {
            groupCalendar.timeInMillis = it.timestamp
            dayKey(groupCalendar)
        }.mapValues { (_, items) ->
            items.sumOf { it.carbonFootprint ?: 0.0 }.toFloat()
        }

        val dayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
        }

        return buildList(7) {
            repeat(7) {
                add(
                    WeeklyChartBar(
                        label = dayFormat.format(dayCalendar.time),
                        totalCarbonKg = groupedTotals[dayKey(dayCalendar)] ?: 0f
                    )
                )
                dayCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private fun dayKey(calendar: Calendar): Int {
        return (calendar.get(Calendar.YEAR) * 1000) + calendar.get(Calendar.DAY_OF_YEAR)
    }
}
