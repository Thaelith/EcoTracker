package com.ecotracker.utils

/**
 * Validates barcode formats before making API calls.
 * Supports common barcode types: EAN-8, EAN-13, UPC-A, UPC-E, ISBN-10, ISBN-13.
 */
object BarcodeValidator {

    /**
     * Checks if a barcode string is valid and non-empty.
     * Only allows numeric barcodes with reasonable lengths.
     */
    fun isValid(barcode: String): Boolean {
        if (barcode.isBlank()) return false
        if (!barcode.all { it.isDigit() }) return false
        if (barcode.length < 8) return false
        return true
    }

    /**
     * Validates EAN-13 checksum (modulo 10).
     */
    fun isValidEan13(barcode: String): Boolean {
        if (barcode.length != 13) return false
        if (!barcode.all { it.isDigit() }) return false

        var sum = 0
        for (i in 0 until 12) {
            val digit = barcode[i].digitToInt()
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return barcode[12].digitToInt() == checkDigit
    }

    /**
     * Returns a user-friendly error message for invalid barcodes.
     */
    fun getValidationError(barcode: String): String? {
        return when {
            barcode.isBlank() -> "Barcode cannot be empty"
            !barcode.all { it.isDigit() } -> "Barcode must contain only numbers"
            barcode.length < 8 -> "Barcode is too short"
            else -> null
        }
    }
}
