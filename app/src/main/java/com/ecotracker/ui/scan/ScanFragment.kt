package com.ecotracker.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ecotracker.R
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.databinding.FragmentScanBinding
import com.ecotracker.utils.CarbonCalculator
import com.ecotracker.utils.Resource
import com.ecotracker.utils.ecoScoreColor
import com.ecotracker.utils.gone
import com.ecotracker.utils.toColor
import com.ecotracker.utils.toColorGradient
import com.ecotracker.utils.toDisplayLabel
import com.ecotracker.utils.toast
import com.ecotracker.utils.visible
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScanViewModel by viewModels()
    private var inputPromptDialog: AlertDialog? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchScanner()
            } else {
                showErrorState(
                    title = getString(R.string.scan_permission_required),
                    message = getString(R.string.scan_error_retry)
                )
            }
        }

    private val scannerLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            result.contents?.let { barcode ->
                binding.tvScannedBarcode.text = barcode
                binding.productResultCard.gone()
                binding.errorState.gone()
                viewModel.lookupBarcode(barcode)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
        resetToIdleState()
    }

    override fun onDestroyView() {
        inputPromptDialog?.dismiss()
        inputPromptDialog = null
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnScanBarcode.setOnClickListener { checkCameraAndScan() }

        binding.btnSaveProduct.setOnClickListener {
            val product = (viewModel.scanState.value as? Resource.Success)?.data
            product?.let(viewModel::saveProduct)
        }

        binding.btnScanAgain.setOnClickListener {
            viewModel.resetState()
            resetToIdleState()
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
                    showErrorState(
                        title = getString(R.string.scan_error_title),
                        message = state.message
                    )
                }
                is Resource.NeedsInput -> {
                    showLoading(false)
                }
                null -> resetToIdleState()
            }
        }

        viewModel.savedState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.loadingState.visible()
                    binding.btnSaveProduct.isEnabled = false
                    binding.btnScanAgain.isEnabled = false
                }
                is Resource.Success -> {
                    binding.loadingState.gone()
                    binding.tvResultState.text = getString(R.string.scan_state_saved)
                    binding.btnScanAgain.isEnabled = true
                    requireContext().toast(getString(R.string.msg_save_success))
                    viewModel.onProductSavedToastShown()
                }
                is Resource.Error -> {
                    binding.loadingState.gone()
                    binding.btnSaveProduct.isEnabled = true
                    binding.btnScanAgain.isEnabled = true
                    requireContext().toast(resource.message)
                }
                else -> {
                    binding.loadingState.gone()
                    binding.btnSaveProduct.isEnabled = true
                    binding.btnScanAgain.isEnabled = true
                }
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
        inputPromptDialog?.dismiss()

        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_product_hint, null)
        val etHint =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etProductHint)
        val btnSearch =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSearchWithAi)
        val btnCancel =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelAiSearch)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_ai_title))
            .setIcon(R.drawable.ic_ai_stars)
            .setView(dialogView)
            .create()

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            if (inputPromptDialog === dialog) {
                inputPromptDialog = null
            }
        }

        btnSearch.setOnClickListener {
            val hintText = etHint.text.toString().trim()
            if (hintText.isNotEmpty()) {
                dialog.dismiss()
                viewModel.estimateWithUserInput(barcode, hintText)
            } else {
                requireContext().toast(getString(R.string.dialog_ai_empty_hint))
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            viewModel.cancelInputPrompt()
        }

        dialog.show()

        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog_surface)
        )

        etHint.requestFocus()
        inputPromptDialog = dialog
    }

    private fun checkCameraAndScan() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> launchScanner()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt(getString(R.string.scan_prompt_align))
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
        }
        scannerLauncher.launch(options)
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingState.visibility = if (loading) View.VISIBLE else View.GONE
        binding.errorState.gone()
        binding.btnScanBarcode.isEnabled = !loading
        if (loading) {
            binding.productResultCard.gone()
        }
    }

    private fun displayProduct(product: ScannedProduct) {
        binding.apply {
            loadingState.gone()
            errorState.gone()
            productResultCard.visible()
            btnSaveProduct.isEnabled = true
            btnScanAgain.isEnabled = true
            tvResultState.text = getString(R.string.scan_state_found)
            tvProductName.text = product.productName
            tvBrand.text = product.brand
            tvEcoScore.text = product.ecoScore
            tvEcoScore.setBackgroundColor(product.ecoScore.ecoScoreColor())
            tvCarbon.text = CarbonCalculator.format(product.carbonFootprint)
            tvCarbon.setTextColor(product.carbonFootprint.toColorGradient())
            tvStatus.text =
                getString(R.string.scan_result_status_format, product.status.toDisplayLabel())
            tvStatus.background.setTint(product.status.toColor())
            tvCategories.text = if (product.categories.isNotBlank()) {
                product.categories.take(80)
            } else {
                getString(R.string.scan_result_missing_value)
            }

            if (!product.aiReasoning.isNullOrBlank()) {
                analysisSection.visible()
                tvReasoning.text = product.aiReasoning
                tvConfidence.text = getString(
                    R.string.scan_ai_confidence,
                    product.aiConfidence ?: getString(R.string.scan_result_missing_value)
                )
            } else {
                analysisSection.gone()
            }
        }
    }

    private fun showErrorState(title: String, message: String) {
        binding.loadingState.gone()
        binding.productResultCard.gone()
        binding.errorState.visible()
        binding.tvErrorTitle.text = title
        binding.tvErrorMessage.text = message
        binding.btnScanBarcode.isEnabled = true
        binding.btnSaveProduct.isEnabled = true
        binding.btnScanAgain.isEnabled = true
    }

    private fun resetToIdleState() {
        binding.loadingState.gone()
        binding.errorState.gone()
        binding.productResultCard.gone()
        binding.tvScannedBarcode.text = getString(R.string.scan_hint)
        binding.tvResultState.text = getString(R.string.scan_state_found)
        binding.btnScanBarcode.isEnabled = true
        binding.btnSaveProduct.isEnabled = true
        binding.btnScanAgain.isEnabled = true
    }
}
