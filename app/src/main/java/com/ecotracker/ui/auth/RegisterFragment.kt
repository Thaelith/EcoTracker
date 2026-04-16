package com.ecotracker.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ecotracker.R
import com.ecotracker.databinding.FragmentRegisterBinding
import com.ecotracker.utils.toast
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRegister.setOnClickListener {
            clearError()

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            when {
                email.isEmpty() || password.isEmpty() || confirm.isEmpty() -> {
                    showError(getString(R.string.auth_error_empty_register))
                    return@setOnClickListener
                }
                password != confirm -> {
                    showError(getString(R.string.auth_error_password_mismatch))
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    showError(getString(R.string.auth_error_password_short))
                    return@setOnClickListener
                }
            }

            setLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    setLoading(false)
                    if (task.isSuccessful) {
                        requireContext().toast(getString(R.string.auth_success_register))
                        findNavController().navigate(R.id.action_registerFragment_to_usernameFragment)
                    } else {
                        showError(
                            getString(
                                R.string.auth_error_register_failed,
                                task.exception?.localizedMessage ?: getString(R.string.error_unknown)
                            )
                        )
                    }
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = getString(
            if (isLoading) R.string.auth_loading_register else R.string.auth_register_button
        )
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.tvGoToLogin.isEnabled = !isLoading
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
