package com.ecotracker.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenBeautyFactsApiService {

    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse> // same response structure

    companion object {
        const val BASE_URL = "https://world.openbeautyfacts.org/"
    }
}
