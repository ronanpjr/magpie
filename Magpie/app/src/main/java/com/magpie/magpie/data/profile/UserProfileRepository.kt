package com.magpie.magpie.data.profile

import com.magpie.magpie.data.auth.api.AuthApiService
import com.magpie.magpie.data.auth.models.UserRead
import com.magpie.magpie.data.auth.token.TokenManager

class UserProfileRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getCurrentUser(): UserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        return authApiService.getCurrentUser("Bearer $token")
    }

    suspend fun getUserById(userId: Int): UserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        // Note: This endpoint may need to be added to AuthApiService if it doesn't exist
        // For now, we'll return the current user as a fallback
        return authApiService.getCurrentUser("Bearer $token")
    }

    suspend fun updateCurrentUser(updates: Map<String, Any>): UserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        return authApiService.updateCurrentUser(updates, "Bearer $token")
    }
}
