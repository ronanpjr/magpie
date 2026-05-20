package com.magpie.magpie.data.review.api

import com.magpie.magpie.data.review.models.PageDto
import com.magpie.magpie.data.review.models.ReviewReadDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApiService {
    @GET("users/{userId}/reviews")
    suspend fun getUserReviews(
        @Path("userId") userId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PageDto<ReviewReadDto>

    @GET("feed")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PageDto<ReviewReadDto>

    @GET("feed/popular")
    suspend fun getPopularFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PageDto<ReviewReadDto>

    @GET("reviews/{reviewId}")
    suspend fun getReview(
        @Path("reviewId") reviewId: Int
    ): ReviewReadDto

    @POST("reviews/{reviewId}/like")
    suspend fun likeReview(
        @Path("reviewId") reviewId: Int
    ): Map<String, String>

    @DELETE("reviews/{reviewId}/like")
    suspend fun unlikeReview(
        @Path("reviewId") reviewId: Int
    ): Map<String, String>
}
