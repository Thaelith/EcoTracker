package com.ecotracker.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecotracker.R
import com.ecotracker.databinding.FragmentUsernameBinding
import com.ecotracker.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsernameFragment : Fragment() {

    private var _binding: FragmentUsernameBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsernameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnSave.setOnClickListener {
            clearError()

            val rawUsername = binding.etUsername.text.toString().trim()
            val username = rawUsername.replace(Regex("[^a-zA-Z0-9_ \\-]"), "")

            if (username.isEmpty()) {
                showError(getString(R.string.auth_error_empty_username))
                return@setOnClickListener
            }

            if (username.length > com.ecotracker.utils.AppConfig.USERNAME_MAX_LENGTH) {
                showError(
                    getString(
                        R.string.auth_error_username_length,
                        com.ecotracker.utils.AppConfig.USERNAME_MAX_LENGTH
                    )
                )
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            setLoading(true)

            val userProfile = hashMapOf(
                "username" to username,
                "co2e" to 0.0,
                "scanCount" to 0,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(userId)
                .set(userProfile)
                .addOnSuccessListener {
                    setLoading(false)
                    requireContext().toast(getString(R.string.auth_success_username))
                    (activity as? AuthActivity)?.navigateToMain()
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    showError(
                        getString(
                            R.string.auth_error_username_failed,
                            e.localizedMessage ?: getString(R.string.error_unknown)
                        )
                    )
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnSave.text = getString(
            if (isLoading) R.string.auth_loading_username
            else R.string.auth_username_button
        )
        binding.etUsername.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        binding.errorPanel.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    private fun clearError() {
        binding.errorPanel.visibility = View.GONE
        binding.tvErrorMessage.text = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
