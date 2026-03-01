package com.ecotracker.data.remote

import com.google.gson.annotations.SerializedName

data class UPCItemDbResponse(
    @SerializedName("code")   val code: String?,
    @SerializedName("total")  val total: Int?,
    @SerializedName("items")  val items: List<UPCItemDbItem>?
)

data class UPCItemDbItem(
    @SerializedName("ean")         val ean: String?,
    @SerializedName("title")       val title: String?,
    @SerializedName("brand")       val brand: String?,
    @SerializedName("category")    val category: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("images")      val images: List<String>?
)
