package com.magpie.magpie.data.review.api

import com.magpie.magpie.data.review.models.PageDto
import com.magpie.magpie.data.review.models.ReviewReadDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ReviewApiService {
    @GET("users/{userId}/reviews")
    suspend fun getUserReviews(
        @Query("userId")
        userId: Int,
        @Query("page")
        page: Int = 1,
        @Query("limit")
        limit: Int = 20,
        @Header("Authorization")
        token: String
    ): PageDto<ReviewReadDto>
}
