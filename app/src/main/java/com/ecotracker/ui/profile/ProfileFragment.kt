package com.ecotracker.ui.profile

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.ecotracker.R
import com.ecotracker.databinding.FragmentProfileBinding
import com.ecotracker.utils.Badge
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var currentPhotoUri: String? = null

    private val pickProfilePhoto =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            val resolver = requireContext().contentResolver
            val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION

            try {
                resolver.takePersistableUriPermission(uri, readFlag)
                releasePreviousPhotoPermission(uri)
                viewModel.updateProfilePhoto(uri.toString())
            } catch (_: SecurityException) {
                Toast.makeText(
                    requireContext(),
                    R.string.profile_photo_error,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSelectProfilePhoto.setOnClickListener {
            pickProfilePhoto.launch(arrayOf("image/*"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    currentPhotoUri = state.photoUri
                    binding.tvEmail.text = state.email
                    binding.tvUsername.text = state.username
                    binding.tvRankName.text = getString(state.rankNameResId)
                    binding.pbRankProgress.progress = state.rankProgress
                    binding.tvRankProgress.text =
                        getString(R.string.profile_rank_progress, state.rankProgress)
                    binding.tvScanCountProgress.text =
                        getString(R.string.label_scan_count, state.scanCount)
                    binding.btnSelectProfilePhoto.text = getString(
                        if (state.photoUri == null) {
                            R.string.profile_photo_choose
                        } else {
                            R.string.profile_photo_change
                        }
                    )
                    bindProfilePhoto(state.photoUri)
                    updateBadgesPreview(state.unlockedBadgesPreview)
                }
            }
        }
    }

    private fun bindProfilePhoto(photoUri: String?) {
        val imageView = binding.ivProfilePhoto
        if (photoUri.isNullOrBlank()) {
            showEmptyProfilePhoto(imageView)
            return
        }

        imageView.setPadding(0, 0, 0, 0)
        imageView.imageTintList = null
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.contentDescription = getString(R.string.profile_photo_content_description)

        Glide.with(this)
            .load(Uri.parse(photoUri))
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(imageView)
    }

    private fun showEmptyProfilePhoto(imageView: ImageView) {
        Glide.with(this).clear(imageView)
        val padding = resources.getDimensionPixelSize(R.dimen.profile_photo_placeholder_padding)
        imageView.setPadding(padding, padding, padding, padding)
        imageView.setImageResource(R.drawable.ic_profile)
        imageView.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.md_theme_primary))
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.contentDescription =
            getString(R.string.profile_photo_placeholder_content_description)
    }

    private fun releasePreviousPhotoPermission(newUri: Uri) {
        val previous = currentPhotoUri?.takeIf { it != newUri.toString() } ?: return
        try {
            requireContext().contentResolver.releasePersistableUriPermission(
                Uri.parse(previous),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
    }

    private fun updateBadgesPreview(unlockedBadges: List<Badge>) {
        binding.layoutBadgesPreview.removeAllViews()
        binding.tvBadgesEmpty.visibility = if (unlockedBadges.isEmpty()) View.VISIBLE else View.GONE

        unlockedBadges.forEach { badge ->
            val context = requireContext()
            val card = MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.profile_badge_card_width),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 16, 0)
                }
                radius = resources.getDimension(R.dimen.profile_badge_card_radius)
                cardElevation = 0f
                strokeWidth = 1
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_surface))
                setStrokeColor(ContextCompat.getColor(context, R.color.md_theme_surface_variant))
            }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
            }

            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(56, 56)
                setImageResource(badge.iconResId ?: R.drawable.ic_quests)
                imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.eco_green)
                )
                contentDescription = getString(R.string.profile_badge_content_description)
            }

            val titleView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 14
                }
                text = getString(badge.nameResId)
                setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            content.addView(imageView)
            content.addView(titleView)
            card.addView(content)
            binding.layoutBadgesPreview.addView(card)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
