package com.ecotracker.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ecotracker.R
import com.ecotracker.databinding.FragmentStatisticsBinding
import com.ecotracker.utils.CarbonCalculator
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private enum class ImpactLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        observeViewModel()
    }

    private fun setupChart() {
        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setFitBars(true)
            setNoDataText("")
            setPinchZoom(false)
            setScaleEnabled(false)
            extraBottomOffset = 8f

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 7
                textColor = requireContext().getColor(R.color.on_surface_variant)
                textSize = 11f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                textColor = requireContext().getColor(R.color.on_surface_variant)
                textSize = 11f
                gridColor = requireContext().getColor(R.color.md_theme_surface_variant)
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return if (value == 0f) "0" else "${value.toInt()} kg"
                    }
                }
            }

            axisRight.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.totalScannedCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalScans.text = count.toString()
        }

        viewModel.totalCarbonToday.observe(viewLifecycleOwner) { carbon ->
            binding.tvDailyCarbon.text = CarbonCalculator.format(carbon ?: 0.0)
        }

        viewModel.totalCarbonThisWeek.observe(viewLifecycleOwner) { carbon ->
            binding.tvWeeklyCarbon.text = CarbonCalculator.format(carbon ?: 0.0)
        }

        viewModel.weeklyProducts.observe(viewLifecycleOwner) { products ->
            if (products.isEmpty()) {
                binding.barChart.clear()
                binding.chartEmptyState.visibility = View.VISIBLE
                return@observe
            }

            binding.chartEmptyState.visibility = View.GONE

            val cal = Calendar.getInstance()
            val groupedByDay = products.groupBy {
                cal.timeInMillis = it.timestamp
                cal.get(Calendar.DAY_OF_YEAR)
            }

            val format = SimpleDateFormat("EEE", Locale.getDefault())
            val entries = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()

            val iterCal = Calendar.getInstance()
            iterCal.add(Calendar.DAY_OF_YEAR, -6)

            for (i in 0..6) {
                val dayOfYear = iterCal.get(Calendar.DAY_OF_YEAR)
                val dayList = groupedByDay[dayOfYear] ?: emptyList()
                val sumCarbon = dayList
                    .filter { it.carbonFootprint != null }
                    .sumOf { it.carbonFootprint!! }
                    .toFloat()

                entries.add(BarEntry(i.toFloat(), sumCarbon))
                labels.add(format.format(iterCal.time))
                iterCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val dataSet = BarDataSet(entries, getString(R.string.chart_title)).apply {
                colors = entries.map { entry ->
                    when (resolveImpactLevel(entry.y)) {
                        ImpactLevel.LOW -> requireContext().getColor(R.color.impact_low)
                        ImpactLevel.MEDIUM -> requireContext().getColor(R.color.impact_medium)
                        ImpactLevel.HIGH -> requireContext().getColor(R.color.impact_high)
                    }
                }
                valueTextSize = 10f
                setDrawValues(false)
                highLightAlpha = 0
            }

            binding.barChart.apply {
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                data = BarData(dataSet).apply {
                    barWidth = 0.56f
                }
                animateY(300)
                invalidate()
            }
        }
    }

    private fun resolveImpactLevel(totalCarbonKg: Float): ImpactLevel {
        return when {
            totalCarbonKg <= LOW_IMPACT_MAX_KG -> ImpactLevel.LOW
            totalCarbonKg <= MEDIUM_IMPACT_MAX_KG -> ImpactLevel.MEDIUM
            else -> ImpactLevel.HIGH
        }
    }

    companion object {
        private const val LOW_IMPACT_MAX_KG = 2f
        private const val MEDIUM_IMPACT_MAX_KG = 5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
