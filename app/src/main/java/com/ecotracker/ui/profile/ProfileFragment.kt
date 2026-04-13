package com.ecotracker.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ecotracker.R
import com.ecotracker.data.repository.EcoTrackerRepository
import com.ecotracker.databinding.FragmentProfileBinding
import com.ecotracker.utils.GamificationEngine
import com.ecotracker.utils.gone
import com.ecotracker.utils.visible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var repository: EcoTrackerRepository

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupUserData()
        observeGamification()
    }

    private fun setupUserData() {
        val user = auth.currentUser
        if (user == null) {
            binding.tvEmail.text = "Not signed in"
            binding.tvUsername.text = "—"
            return
        }

        binding.tvEmail.text = user.email ?: "—"

        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val b = _binding ?: return@addOnSuccessListener
                if (document != null && document.exists()) {
                    val rawUsername = document.getString("username")
                    if (!rawUsername.isNullOrBlank()) {
                        b.tvUsername.text = rawUsername
                            .replace(Regex("[^a-zA-Z0-9_ \\-]"), "")
                            .take(com.ecotracker.utils.AppConfig.USERNAME_MAX_LENGTH)
                    }
                }
            }
            .addOnFailureListener {
                val b = _binding ?: return@addOnFailureListener
                b.tvUsername.text = "—"
            }
    }

    private fun observeGamification() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllProducts().collectLatest { products ->
                val scanCount = products.size
                val rank = GamificationEngine.calculateRank(scanCount)
                val badges = GamificationEngine.getBadges(products)

                binding.apply {
                    tvRankName.text = getString(rank.nameResId)
                    pbRankProgress.progress = rank.percentage
                    tvScanCountProgress.text = getString(R.string.label_scan_count, scanCount)
                }

                updateBadgesPreview(badges.filter { it.isUnlocked })
            }
        }
    }

    private fun updateBadgesPreview(unlockedBadges: List<com.ecotracker.utils.Badge>) {
        binding.layoutBadgesPreview.removeAllViews()
        
        if (unlockedBadges.isEmpty()) {
            // Optional: show a "No badges yet" message
            return
        }

        unlockedBadges.take(5).forEach { badge ->
            val imageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                    setMargins(0, 0, 16, 0)
                }
                setImageResource(R.drawable.ic_quests)
                imageTintList = android.content.res.ColorStateList.valueOf(
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
