package com.magpie.magpie.data.auth.api

import com.magpie.magpie.data.auth.models.AuthResponse
import com.magpie.magpie.data.auth.models.PasswordRecoveryRequest
import com.magpie.magpie.data.auth.models.PasswordRecoveryResponse
import com.magpie.magpie.data.auth.models.TokenRefreshRequest
import com.magpie.magpie.data.auth.models.TokenResponse
import com.magpie.magpie.data.auth.models.UserCreate
import com.magpie.magpie.data.auth.models.UserLogin
import com.magpie.magpie.data.auth.models.UserRead
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Header

interface AuthApiService {
    @POST("auth/register")
    suspend fun register(
        @Body request: UserCreate
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: UserLogin
    ): AuthResponse

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: TokenRefreshRequest
    ): TokenResponse

    @POST("auth/password-recovery")
    suspend fun passwordRecovery(
        @Body request: PasswordRecoveryRequest
    ): Map<String, String>

    @GET("users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): UserRead

    @PUT("users/me")
    suspend fun updateCurrentUser(
        @Body updates: Map<String, Any>,
        @Header("Authorization") token: String
    ): UserRead
}
