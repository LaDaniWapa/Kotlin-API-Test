package com.daniela.apitestv1.api

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MyApi {
    @Headers("Accept: application/json")
    @GET("/")
    fun getIndex(): Call<String?>?

    @Multipart
    @POST("/predict/")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<ApiResponse>

}

data class ApiResponse(
    @SerializedName("SFW probability") val sfwProbability: String,
    @SerializedName("NSFW probability") val nsfwProbability: String
)
