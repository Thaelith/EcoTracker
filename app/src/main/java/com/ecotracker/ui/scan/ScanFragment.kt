package com.ecotracker.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ecotracker.R
import com.ecotracker.databinding.FragmentScanBinding
import com.ecotracker.utils.Resource
import com.ecotracker.utils.gone
import com.ecotracker.utils.toast
import com.ecotracker.utils.visible
import com.ecotracker.utils.ecoScoreColor
import com.ecotracker.utils.toColorGradient
import com.ecotracker.utils.CarbonCalculator
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScanViewModel by viewModels()

    // ── Camera permission launcher ────────────────────────────────────────────

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchScanner() else requireContext().toast("Camera permission required")
        }

    // ── ZXing scanner launcher ────────────────────────────────────────────────

    private val scannerLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents?.let { barcode ->
                binding.tvScannedBarcode.text = barcode
                binding.productResultCard.gone()
                viewModel.lookupBarcode(barcode)
            }
        }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnScanBarcode.setOnClickListener { checkCameraAndScan() }

        binding.btnSaveProduct.setOnClickListener {
            val product = (viewModel.scanState.value as? Resource.Success)?.data
            product?.let { viewModel.saveProduct(it) }
        }

        binding.btnScanAgain.setOnClickListener {
            binding.productResultCard.gone()
            binding.tvScannedBarcode.text = getString(R.string.scan_hint)
            viewModel.resetState()
        }
    }

    private fun observeViewModel() {
        viewModel.scanState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    displayProduct(state.data)
                }
                is Resource.Error -> {
                    showLoading(false)
                    requireContext().toast(state.message)
                }
                is Resource.NeedsInput -> {
                    showLoading(false)
                }
                null -> { /* idle */ }
            }
        }

        viewModel.savedState.observe(viewLifecycleOwner) { saved ->
            if (saved == true) {
                requireContext().toast("Product saved to history!")
                viewModel.onProductSavedToastShown()
            }
        }

        viewModel.showManualEntry.observe(viewLifecycleOwner) { barcode ->
            if (barcode != null) {
                val action = ScanFragmentDirections.actionScanFragmentToManualEntryFragment(barcode)
                findNavController().navigate(action)
                viewModel.onManualEntryNavigated()
            }
        }

        viewModel.showInputPrompt.observe(viewLifecycleOwner) { barcode ->
            if (barcode != null) {
                showProductHintDialog(barcode)
                viewModel.onInputPromptShown()
            }
        }
    }

    private fun showProductHintDialog(barcode: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_product_hint, null)
        val etHint = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etProductHint)
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Product Not Found")
            .setIcon(R.drawable.ic_ai_stars)
            .setView(dialogView)
            .setPositiveButton("Search with AI") { _, _ ->
                val hintText = etHint.text.toString().trim()
                if (hintText.isNotEmpty()) {
                    viewModel.estimateWithUserInput(barcode, hintText)
                } else {
                    requireContext().toast("Hint cannot be empty.")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        // Auto-show keyboard
        etHint.requestFocus()
    }

    // ── Scanning ──────────────────────────────────────────────────────────────

    private fun checkCameraAndScan() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchScanner()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt("Align barcode within the frame")
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
        }
        scannerLauncher.launch(options)
    }

    // ── UI updates ────────────────────────────────────────────────────────────

    private fun showLoading(loading: Boolean) {
        if (loading) binding.progressBar.visible() else binding.progressBar.gone()
    }

    private fun displayProduct(product: com.ecotracker.data.local.ScannedProduct) {
        binding.apply {
            productResultCard.visible()
            tvProductName.text  = product.productName
            tvBrand.text        = product.brand
            tvEcoScore.text     = product.ecoScore
            tvEcoScore.setBackgroundColor(product.ecoScore.ecoScoreColor())
            tvCarbon.text       = CarbonCalculator.format(product.carbonFootprint)
            tvCarbon.setTextColor(product.carbonFootprint.toColorGradient())
            tvCategories.text   = if (product.categories.isNotBlank())
                product.categories.take(80) else "—"

            // Show Analysis if available
            if (!product.aiReasoning.isNullOrBlank()) {
                analysisSection.visible()
                tvReasoning.text = product.aiReasoning
                tvConfidence.text = "Confidence: ${product.aiConfidence ?: "Unknown"}"
            } else {
                analysisSection.gone()
            }
        }
    }
}
