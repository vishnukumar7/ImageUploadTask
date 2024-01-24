package com.app.imageupload.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiInterface {

    @Multipart
    @POST("/uploadImage")
    fun uploadImage(@Part image : MultipartBody.Part): Call<ResponseBody>
}