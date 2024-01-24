package com.app.imageupload.network

import com.app.imageupload.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(getOkHttpClient())
        .build()

    private fun getOkHttpClient(): OkHttpClient {
        val client = OkHttpClient.Builder()
        client.apply {
            readTimeout(1, TimeUnit.MINUTES)
            writeTimeout(1, TimeUnit.MINUTES)
            connectTimeout(1, TimeUnit.MINUTES)
            addInterceptor(getLogging())
        }
        return client.build()
    }

    private fun getLogging(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        if(BuildConfig.DEBUG){
            logging.level = HttpLoggingInterceptor.Level.BASIC
        }
        else{
            logging.level = HttpLoggingInterceptor.Level.NONE
        }
        return logging
    }

    fun <T> buildService(service: Class<T>): T {
        return retrofit.create(service)
    }

}