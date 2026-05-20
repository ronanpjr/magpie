package com.magpie.magpie.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.magpie.magpie.data.auth.models.UserRead

@Entity(tableName = "cached_user")
data class CachedUser(
    @PrimaryKey val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val createdAt: String,
    val followerCount: Int,
    val followingCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > TTL

    fun toDto(): UserRead = UserRead(
        id = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        bio = bio,
        createdAt = createdAt,
        followerCount = followerCount,
        followingCount = followingCount,
        isFollowing = false
    )

    companion object {
        private const val TTL = 3_600_000L // 1 hour

        fun fromDto(dto: UserRead): CachedUser = CachedUser(
            id = dto.id,
            username = dto.username,
            displayName = dto.displayName,
            avatarUrl = dto.avatarUrl,
            bio = dto.bio,
            createdAt = dto.createdAt,
            followerCount = dto.followerCount,
            followingCount = dto.followingCount
        )
    }
}
