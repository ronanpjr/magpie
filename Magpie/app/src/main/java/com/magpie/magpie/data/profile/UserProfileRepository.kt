package com.magpie.magpie.data.profile

import com.magpie.magpie.data.auth.api.AuthApiService
import com.magpie.magpie.data.auth.models.PageUserRead
import com.magpie.magpie.data.auth.models.UserRead
import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewReadDto

class UserProfileRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val reviewRepository: ReviewRepository
) {
    suspend fun getCurrentUser(): UserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        return authApiService.getCurrentUser("Bearer $token")
    }

    suspend fun getUserById(userId: Int): UserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        return authApiService.getUserById(userId, "Bearer $token")
    }

    suspend fun updateCurrentUser(updates: Map<String, Any>): UserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        return authApiService.updateCurrentUser(updates, "Bearer $token")
    }

    suspend fun followUser(userId: Int) {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        authApiService.followUser(userId, "Bearer $token")
    }

    suspend fun unfollowUser(userId: Int) {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        authApiService.unfollowUser(userId, "Bearer $token")
    }

    suspend fun getFollowers(userId: Int, page: Int = 1, limit: Int = 20): PageUserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        return authApiService.getFollowers(userId, page, limit, "Bearer $token")
    }

    suspend fun getFollowing(userId: Int, page: Int = 1, limit: Int = 20): PageUserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        return authApiService.getFollowing(userId, page, limit, "Bearer $token")
    }

    suspend fun searchUsers(query: String, page: Int = 1, limit: Int = 20): PageUserRead {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token found")
        return authApiService.searchUsers(query, page, limit, "Bearer $token")
    }

    suspend fun getUserReviews(userId: Int, page: Int = 1, limit: Int = 20): List<ReviewReadDto> {
        return reviewRepository.getUserReviews(userId, page, limit)
    }
}
