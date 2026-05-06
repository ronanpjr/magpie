package com.magpie.magpie.data.auth.token

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_token")
data class TokenEntity(
    @PrimaryKey val id: Int = 0,
    val accessToken: String?,
    val refreshToken: String?
)
