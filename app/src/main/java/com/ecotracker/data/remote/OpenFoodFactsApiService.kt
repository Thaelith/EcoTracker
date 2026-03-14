package com.ecotracker.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApiService {

    /**
     * Fetch product data by barcode from Open Food Facts.
     * Example: GET /api/v2/product/3017620422003.json
     */
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse>

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
    }
}
