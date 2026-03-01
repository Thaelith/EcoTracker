package com.ecotracker.ui.manual

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ecotracker.databinding.FragmentManualEntryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManualEntryFragment : Fragment() {

    private var _binding: FragmentManualEntryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManualEntryViewModel by viewModels()
    private val args: ManualEntryFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.barcodeTextView.text = "Barcode: ${args.barcode}"

        binding.searchOnWebButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${args.barcode}"))
            startActivity(intent)
        }

        binding.saveButton.setOnClickListener {
            val productName = binding.productNameEditText.text.toString()
            val brand = binding.brandEditText.text.toString()
            val category = binding.categoryEditText.text.toString()
            val imageUrl = binding.imageUrlEditText.text.toString()

            if (productName.isNotBlank() && brand.isNotBlank() && category.isNotBlank()) {
                viewModel.saveProduct(args.barcode, productName, brand, category, imageUrl)
            } else {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.saveState.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(requireContext(), "Product saved!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                viewModel.onSaveConsumed()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
