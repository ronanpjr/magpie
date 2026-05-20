package com.magpie.magpie.data.review.models

import com.magpie.magpie.data.auth.models.UserRead
import com.squareup.moshi.Json

// Pagination wrapper for API responses
data class PageDto<T>(
    @Json(name = "items")
    val items: List<T>,
    @Json(name = "total")
    val total: Int,
    @Json(name = "page")
    val page: Int,
    @Json(name = "limit")
    val limit: Int,
    @Json(name = "pages")
    val pages: Int
)

// Review DTO from API
data class ReviewReadDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "author")
    val author: UserRead,
    @Json(name = "target_type")
    val targetType: String, // "track" or "album"
    @Json(name = "target_id")
    val targetId: Int,
    @Json(name = "target_title")
    val targetTitle: String,
    @Json(name = "target_image_url")
    val targetImageUrl: String? = null,
    @Json(name = "artist_name")
    val artistName: String,
    @Json(name = "rating")
    val rating: Double,
    @Json(name = "body")
    val body: String? = null,
    @Json(name = "like_count")
    val likeCount: Int = 0,
    @Json(name = "liked_by_me")
    val likedByMe: Boolean = false,
    @Json(name = "is_owner")
    val isOwner: Boolean = false,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "updated_at")
    val updatedAt: String
)

data class ReviewCreateDto(
    @Json(name = "target_type")
    val targetType: String,
    @Json(name = "target_id")
    val targetId: Int,
    @Json(name = "rating")
    val rating: Double,
    @Json(name = "body")
    val body: String? = null
)

data class ReviewCommentDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "author")
    val author: UserRead,
    @Json(name = "body")
    val body: String,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "up_count")
    val upCount: Int = 0,
    @Json(name = "down_count")
    val downCount: Int = 0,
    @Json(name = "my_vote")
    val myVote: String? = null
)

data class ReviewCommentCreateDto(
    @Json(name = "body")
    val body: String
)

data class ReviewCommentVoteRequestDto(
    @Json(name = "direction")
    val direction: String
)

data class ReviewUpdateDto(
    @Json(name = "rating")
    val rating: Double? = null,
    @Json(name = "body")
    val body: String? = null
)
