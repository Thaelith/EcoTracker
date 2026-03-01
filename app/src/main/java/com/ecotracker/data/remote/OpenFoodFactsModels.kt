package com.ecotracker.data.remote

import com.google.gson.annotations.SerializedName

// ── Top-level response ────────────────────────────────────────────────────────

data class OpenFoodFactsResponse(
    @SerializedName("status")        val status: Int,
    @SerializedName("status_verbose") val statusVerbose: String?,
    @SerializedName("product")       val product: ProductDto?
)

// ── Product DTO ───────────────────────────────────────────────────────────────

data class ProductDto(
    @SerializedName("code")                        val barcode: String?,
    @SerializedName("product_name")                val productName: String?,
    @SerializedName("product_name_en")             val productNameEn: String?,
    @SerializedName("brands")                      val brands: String?,
    @SerializedName("categories")                  val categories: String?,
    @SerializedName("image_url")                   val imageUrl: String?,
    @SerializedName("image_front_url")             val imageFrontUrl: String?,
    @SerializedName("ecoscore_grade")              val ecoScoreGrade: String?,
    @SerializedName("ecoscore_score")              val ecoScoreScore: Int?,
    @SerializedName("ecoscore_data")               val ecoScoreData: EcoScoreDataDto?,
    @SerializedName("nutriments")                  val nutriments: NutrimentsDto?,
    @SerializedName("packaging")                   val packaging: String?,
    @SerializedName("origins")                     val origins: String?,
    @SerializedName("manufacturing_places")        val manufacturingPlaces: String?
)

// ── EcoScore data ─────────────────────────────────────────────────────────────

data class EcoScoreDataDto(
    @SerializedName("score")         val score: Int?,
    @SerializedName("grade")         val grade: String?,
    @SerializedName("agribalyse")    val agribalyse: AgribalyseDto?
)

data class AgribalyseDto(
    @SerializedName("co2_total")     val co2Total: Double?,
    @SerializedName("co2_agriculture") val co2Agriculture: Double?,
    @SerializedName("co2_packaging") val co2Packaging: Double?
)

// ── Nutriments (optional extra info) ─────────────────────────────────────────

data class NutrimentsDto(
    @SerializedName("carbon-footprint-from-known-ingredients_product") val carbonFootprint: Double?,
    @SerializedName("carbon-footprint-from-known-ingredients_100g")    val carbonFootprintPer100g: Double?
)
