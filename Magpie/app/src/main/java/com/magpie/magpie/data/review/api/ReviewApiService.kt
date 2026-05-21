package com.magpie.magpie.data.review.api

import com.magpie.magpie.data.review.models.PageDto
import com.magpie.magpie.data.review.models.ReviewCommentCreateDto
import com.magpie.magpie.data.review.models.ReviewCommentDto
import com.magpie.magpie.data.review.models.ReviewCommentVoteRequestDto
import com.magpie.magpie.data.review.models.ReviewCreateDto
import com.magpie.magpie.data.review.models.ReviewReadDto
import com.magpie.magpie.data.review.models.ReviewUpdateDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApiService {

    @GET("users/{userId}/reviews")
    suspend fun getUserReviews(
        @Path("userId") userId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Header("Authorization") token: String
    ): PageDto<ReviewReadDto>


    @GET("reviews")
    suspend fun getReviews(
        @Query("target_type") targetType: String? = null,
        @Query("target_id") targetId: Int? = null,
        @Query("author_id") authorId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("order_by") orderBy: String = "recent",
        @Header("Authorization") token: String
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

    @POST("reviews")
    suspend fun createReview(
        @Body payload: ReviewCreateDto
    ): ReviewReadDto

    @GET("review-comments/{reviewId}")
    suspend fun getReviewComments(
        @Path("reviewId") reviewId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): PageDto<ReviewCommentDto>

    @POST("review-comments/{reviewId}")
    suspend fun postReviewComment(
        @Path("reviewId") reviewId: Int,
        @Body payload: ReviewCommentCreateDto
    ): ReviewCommentDto

    @POST("review-comments/{reviewId}/vote/{commentId}")
    suspend fun voteReviewComment(
        @Path("reviewId") reviewId: Int,
        @Path("commentId") commentId: Int,
        @Body payload: ReviewCommentVoteRequestDto
    ): Map<String, String>

    @POST("reviews/{reviewId}/like")
    suspend fun likeReview(
        @Path("reviewId") reviewId: Int
    ): Map<String, String>

    @DELETE("reviews/{reviewId}/like")
    suspend fun unlikeReview(
        @Path("reviewId") reviewId: Int
    ): Map<String, String>

    @PUT("reviews/{reviewId}")
    suspend fun updateReview(
        @Path("reviewId") reviewId: Int,
        @Body payload: ReviewUpdateDto
    ): ReviewReadDto

    @DELETE("reviews/{reviewId}")
    suspend fun deleteReview(
        @Path("reviewId") reviewId: Int
    ): Map<String, String>
}