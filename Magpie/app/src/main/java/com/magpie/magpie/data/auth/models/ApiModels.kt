package com.magpie.magpie.data.auth.models

import com.squareup.moshi.Json
import java.time.Instant

// Request/Response DTOs
data class UserCreate(
    @Json(name = "username")
    val username: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "password")
    val password: String,
    @Json(name = "display_name")
    val displayName: String
)

data class UserLogin(
    @Json(name = "username")
    val username: String,
    @Json(name = "password")
    val password: String
)

data class TokenRefreshRequest(
    @Json(name = "refresh_token")
    val refreshToken: String
)

data class PasswordRecoveryRequest(
    @Json(name = "email_or_username")
    val emailOrUsername: String
)

// Response DTOs
data class UserRead(
    @Json(name = "id")
    val id: Int,
    @Json(name = "username")
    val username: String,
    @Json(name = "display_name")
    val displayName: String,
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    @Json(name = "bio")
    val bio: String? = null,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "follower_count")
    val followerCount: Int = 0,
    @Json(name = "following_count")
    val followingCount: Int = 0,
    @Json(name = "is_following")
    val isFollowing: Boolean = false
)

data class AuthResponse(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "token_type")
    val tokenType: String = "bearer",
    @Json(name = "refresh_token")
    val refreshToken: String? = null,
    @Json(name = "user")
    val user: UserRead
)

data class TokenResponse(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "token_type")
    val tokenType: String = "bearer",
    @Json(name = "refresh_token")
    val refreshToken: String? = null
)

data class PasswordRecoveryResponse(
    val message: String? = null
)
