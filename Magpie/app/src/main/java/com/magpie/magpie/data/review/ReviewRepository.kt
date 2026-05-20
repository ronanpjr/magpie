package com.magpie.magpie.data.review

import com.magpie.magpie.data.review.api.ReviewApiService
import com.magpie.magpie.data.review.models.PageDto
import com.magpie.magpie.data.review.models.ReviewCommentCreateDto
import com.magpie.magpie.data.review.models.ReviewCommentDto
import com.magpie.magpie.data.review.models.ReviewCommentVoteRequestDto
import com.magpie.magpie.data.review.models.ReviewCreateDto
import com.magpie.magpie.data.review.models.ReviewReadDto
import javax.inject.Inject

class ReviewRepository @Inject constructor(
    private val reviewApiService: ReviewApiService
) {
    suspend fun getUserReviews(
        userId: Int,
        page: Int = 1,
        limit: Int = 20
    ): List<ReviewReadDto> {
        return reviewApiService.getUserReviews(userId, page, limit).items
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

    suspend fun unlikeReview(reviewId: Int) {
        reviewApiService.unlikeReview(reviewId)
    }
}
