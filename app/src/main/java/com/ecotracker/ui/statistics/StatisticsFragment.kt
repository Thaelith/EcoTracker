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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
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
                return@observe
            }
            
            // Group products by day of year
            val cal = Calendar.getInstance()
            val groupedByDay = products.groupBy { 
                cal.timeInMillis = it.timestamp
                cal.get(Calendar.DAY_OF_YEAR)
            }

            // Create last 7 days list
            val format = SimpleDateFormat("EEE", Locale.getDefault())
            val entries = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()

            val iterCal = Calendar.getInstance()
            iterCal.add(Calendar.DAY_OF_YEAR, -6) // Start from 6 days ago

            for (i in 0..6) {
                val dayOfYear = iterCal.get(Calendar.DAY_OF_YEAR)
                val dayList = groupedByDay[dayOfYear] ?: emptyList()
                val sumCarbon = dayList.sumOf { it.carbonFootprint }.toFloat()

                entries.add(BarEntry(i.toFloat(), sumCarbon))
                labels.add(format.format(iterCal.time))
                iterCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val dataSet = BarDataSet(entries, "Carbon Footprint").apply {
                color = requireContext().getColor(R.color.md_theme_primary)
                valueTextSize = 10f
                setDrawValues(false)
            }

            binding.barChart.apply {
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                data = BarData(dataSet)
                invalidate() // refresh
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
