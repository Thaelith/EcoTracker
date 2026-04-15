package com.ecotracker.ui.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ecotracker.R
import com.ecotracker.databinding.FragmentProfileBinding
import com.ecotracker.utils.Badge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvEmail.text = state.email
                    binding.tvUsername.text = state.username
                    binding.tvRankName.text = getString(state.rankNameResId)
                    binding.pbRankProgress.progress = state.rankProgress
                    binding.tvScanCountProgress.text =
                        getString(R.string.label_scan_count, state.scanCount)
                    updateBadgesPreview(state.unlockedBadgesPreview)
                }
            }
        }
    }

    private fun updateBadgesPreview(unlockedBadges: List<Badge>) {
        binding.layoutBadgesPreview.removeAllViews()

        unlockedBadges.forEach { badge ->
            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                    setMargins(0, 0, 16, 0)
                }
                setImageResource(badge.iconResId ?: R.drawable.ic_quests)
                imageTintList = ColorStateList.valueOf(
                    resources.getColor(R.color.eco_green, null)
                )
            }
            binding.layoutBadgesPreview.addView(imageView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
