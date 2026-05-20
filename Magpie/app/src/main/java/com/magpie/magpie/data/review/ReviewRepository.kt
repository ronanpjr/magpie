package com.magpie.magpie.data.review

import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.review.api.ReviewApiService
import com.magpie.magpie.data.review.models.PageDto
import com.magpie.magpie.data.review.models.ReviewCommentCreateDto
import com.magpie.magpie.data.review.models.ReviewCommentDto
import com.magpie.magpie.data.review.models.ReviewCommentVoteRequestDto
import com.magpie.magpie.data.review.models.ReviewCreateDto
import com.magpie.magpie.data.review.models.ReviewReadDto
import com.magpie.magpie.data.review.models.ReviewUpdateDto
import javax.inject.Inject

class ReviewRepository @Inject constructor(
    private val reviewApiService: ReviewApiService,
    private val tokenManager: TokenManager? = null
) {
    suspend fun getUserReviews(
        userId: Int,
        page: Int = 1,
        limit: Int = 20
    ): List<ReviewReadDto> {
        val token = tokenManager?.getAccessToken() ?: throw IllegalStateException("No access token available")
        val bearerToken = "Bearer $token"
        return reviewApiService.getUserReviews(userId, page, limit, bearerToken).items
    }

    suspend fun getFeed(page: Int = 1, limit: Int = 20): PageDto<ReviewReadDto> {
        return reviewApiService.getFeed(page, limit)
    }

    suspend fun getPopularFeed(page: Int = 1, limit: Int = 20): PageDto<ReviewReadDto> {
        return reviewApiService.getPopularFeed(page, limit)
    }

    suspend fun getReview(reviewId: Int): ReviewReadDto {
        return reviewApiService.getReview(reviewId)
    }

    suspend fun createReview(payload: ReviewCreateDto): ReviewReadDto {
        return reviewApiService.createReview(payload)
    }

    suspend fun getReviewComments(
        reviewId: Int,
        page: Int = 1,
        limit: Int = 50
    ): PageDto<ReviewCommentDto> {
        return reviewApiService.getReviewComments(reviewId, page, limit)
    }

    suspend fun postReviewComment(reviewId: Int, body: String): ReviewCommentDto {
        return reviewApiService.postReviewComment(reviewId, ReviewCommentCreateDto(body = body))
    }

    suspend fun voteReviewComment(reviewId: Int, commentId: Int, direction: String) {
        reviewApiService.voteReviewComment(
            reviewId,
            commentId,
            ReviewCommentVoteRequestDto(direction = direction)
        )
    }

    suspend fun likeReview(reviewId: Int) {
        reviewApiService.likeReview(reviewId)
    }

    suspend fun updateReview(reviewId: Int, payload: ReviewUpdateDto): ReviewReadDto {
        return reviewApiService.updateReview(reviewId, payload)
    }

    suspend fun deleteReview(reviewId: Int) {
        reviewApiService.deleteReview(reviewId)
    }

    suspend fun unlikeReview(reviewId: Int) {
        reviewApiService.unlikeReview(reviewId)
    }

    suspend fun getReviews(
        targetType: String? = null,
        targetId: Int? = null,
        authorId: Int? = null,
        page: Int = 1,
        limit: Int = 20,
        orderBy: String = "recent"
    ): List<ReviewReadDto> {
        val token = tokenManager?.getAccessToken() ?: throw IllegalStateException("No access token available")
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

    suspend fun createPlaceholderReview(
        targetType: String,  // "album" or "track"
        targetId: Int
    ): ReviewReadDto {
        return reviewApiService.createReview(
            ReviewCreateDto(
                targetType = targetType,
                targetId = targetId,
                rating = 3.0,
                body = null
            )
        )
    }
}
