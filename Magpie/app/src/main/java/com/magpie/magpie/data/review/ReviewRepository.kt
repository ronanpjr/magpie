package com.magpie.magpie.data.review

import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.review.api.ReviewApiService
import com.magpie.magpie.data.review.models.ReviewReadDto
import javax.inject.Inject

class ReviewRepository @Inject constructor(
    private val reviewApiService: ReviewApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getUserReviews(
        userId: Int,
        page: Int = 1,
        limit: Int = 20
    ): List<ReviewReadDto> {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token available")
        val bearerToken = "Bearer $token"
        val pageDto = reviewApiService.getUserReviews(
            userId = userId,
            page = page,
            limit = limit,
            token = bearerToken
        )
        return pageDto.items
    }

    suspend fun getReviews(
        targetType: String? = null,
        targetId: Int? = null,
        authorId: Int? = null,
        page: Int = 1,
        limit: Int = 20,
        orderBy: String = "recent"
    ): List<ReviewReadDto> {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No access token available")
        val bearerToken = "Bearer $token"
        val pageDto = reviewApiService.getReviews(
            targetType = targetType,
            targetId = targetId,
            authorId = authorId,
            page = page,
            limit = limit,
            orderBy = orderBy,
            token = bearerToken
        )
        return pageDto.items
    }
}
