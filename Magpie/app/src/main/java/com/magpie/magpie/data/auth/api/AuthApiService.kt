package com.magpie.magpie.data.auth.api

import com.magpie.magpie.data.auth.models.AuthResponse
import com.magpie.magpie.data.auth.models.PasswordRecoveryRequest
import com.magpie.magpie.data.auth.models.PasswordRecoveryResponse
import com.magpie.magpie.data.auth.models.PageUserRead
import com.magpie.magpie.data.auth.models.TokenRefreshRequest
import com.magpie.magpie.data.auth.models.TokenResponse
import com.magpie.magpie.data.auth.models.UserCreate
import com.magpie.magpie.data.auth.models.UserLogin
import com.magpie.magpie.data.auth.models.UserRead
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Header
import retrofit2.http.Query

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

    @GET("users/{user_id}")
    suspend fun getUserById(
        @Path("user_id") userId: Int,
        @Header("Authorization") token: String
    ): UserRead

    @POST("users/{user_id}/follow")
    suspend fun followUser(
        @Path("user_id") userId: Int,
        @Header("Authorization") token: String
    ): Map<String, String>

    @DELETE("users/{user_id}/follow")
    suspend fun unfollowUser(
        @Path("user_id") userId: Int,
        @Header("Authorization") token: String
    ): Map<String, String>

    @GET("users/{user_id}/followers")
    suspend fun getFollowers(
        @Path("user_id") userId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") token: String
    ): PageUserRead

    @GET("users/{user_id}/following")
    suspend fun getFollowing(
        @Path("user_id") userId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") token: String
    ): PageUserRead

    @GET("users/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") token: String
    ): PageUserRead
}
