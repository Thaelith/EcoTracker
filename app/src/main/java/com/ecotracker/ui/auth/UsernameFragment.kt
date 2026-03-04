package com.ecotracker.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ecotracker.databinding.FragmentUsernameBinding
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
        inflater: LayoutInflater, container: ViewGroup?,
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
            val username = binding.etUsername.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            setLoading(true)
            
            // Create a user profile in Firestore
            val userProfile = hashMapOf(
                "username" to username,
                "co2e" to 0.0,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(userId)
                .set(userProfile)
                .addOnSuccessListener {
                    setLoading(false)
                    Toast.makeText(context, "Username saved!", Toast.LENGTH_SHORT).show()
                    (activity as? AuthActivity)?.navigateToMain()
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(context, "Failed to save username: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.etUsername.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
