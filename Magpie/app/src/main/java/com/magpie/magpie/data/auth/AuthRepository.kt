package com.magpie.magpie.data.auth

interface AuthRepository {
    suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String
    ): AuthResult

    suspend fun login(username: String, password: String): AuthResult

    suspend fun refreshToken(refreshToken: String): AuthResult

    suspend fun passwordRecovery(emailOrUsername: String): AuthResult

    suspend fun logout()
}

data class AuthResult(
    val success: Boolean,
    val message: String,
    val username: String? = null,
    val userId: Int? = null,
    val displayName: String? = null,
    val email: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)
