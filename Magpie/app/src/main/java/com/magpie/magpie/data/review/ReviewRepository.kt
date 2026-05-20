package com.magpie.magpie.data.review

import com.magpie.magpie.data.review.api.ReviewApiService
import com.magpie.magpie.data.review.models.PageDto
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

    suspend fun likeReview(reviewId: Int) {
        reviewApiService.likeReview(reviewId)
    }

    suspend fun unlikeReview(reviewId: Int) {
        reviewApiService.unlikeReview(reviewId)
    }
}
