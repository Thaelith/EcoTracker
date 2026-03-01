package com.ecotracker.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface UPCItemDbApiService {

    @GET("trial/lookup")
    suspend fun lookupBarcode(
        @Query("upc") barcode: String
    ): Response<UPCItemDbResponse>

    companion object {
        const val BASE_URL = "https://api.upcitemdb.com/"
    }
}
