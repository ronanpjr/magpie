package com.magpie.magpie.data.network

import android.content.Context
import com.magpie.magpie.BuildConfig
import com.magpie.magpie.data.auth.api.AuthApiService
import com.magpie.magpie.data.auth.token.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var retrofit: Retrofit? = null
    private lateinit var tokenManager: TokenManager
    private lateinit var context: Context

    fun initialize(context: Context, tokenManager: TokenManager) {
        this.context = context.applicationContext
        this.tokenManager = tokenManager
    }

    fun getRetrofit(): Retrofit {
        return retrofit ?: buildRetrofit()
    }

    private fun buildRetrofit(): Retrofit {
        if (!::context.isInitialized || !::tokenManager.isInitialized) {
            throw IllegalStateException("RetrofitClient must be initialized with context and tokenManager")
        }

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = buildOkHttpClient()

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit!!
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add token interceptor
        httpClient.addInterceptor { chain ->
            val originalRequest = chain.request()
            
            // Add Authorization header if we have a token
            val token = tokenManager.getAccessToken()
            val requestBuilder = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }

            chain.proceed(requestBuilder)
        }

        // Add logging interceptor in debug builds
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            httpClient.addInterceptor(loggingInterceptor)
        }

        return httpClient.build()
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return getRetrofit().create(serviceClass)
    }
}
