package com.magpie.magpie.data.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryAuthRepository : AuthRepository {

    private data class UserRecord(
        val password: String,
        val email: String,
        val displayName: String
    )

    private val users = mutableMapOf<String, UserRecord>()
    private val mutex = Mutex()

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String
    ): AuthResult {
        return mutex.withLock {
            if (users.containsKey(username)) {
                return@withLock AuthResult(
                    success = false,
                    message = "USER_EXISTS"
                )
            }

            users[username] = UserRecord(
                password = password,
                email = email,
                displayName = displayName
            )
            AuthResult(
                success = true,
                message = "REGISTER_SUCCESS",
                username = username,
                displayName = displayName,
                email = email
            )
        }
    }

    override suspend fun login(username: String, password: String): AuthResult {
        return mutex.withLock {
            val storedUser = users[username]
            if (storedUser == null) {
                return@withLock AuthResult(
                    success = false,
                    message = "USER_NOT_FOUND"
                )
            }

            if (storedUser.password != password) {
                return@withLock AuthResult(
                    success = false,
                    message = "INVALID_CREDENTIALS"
                )
            }

            AuthResult(
                success = true,
                message = "LOGIN_SUCCESS",
                username = username,
                displayName = storedUser.displayName,
                email = storedUser.email
            )
        }
    }

    override suspend fun refreshToken(refreshToken: String): AuthResult {
        return AuthResult(
            success = false,
            message = "TOKEN_REFRESH_NOT_SUPPORTED"
        )
    }

    override suspend fun passwordRecovery(emailOrUsername: String): AuthResult {
        return AuthResult(
            success = true,
            message = "PASSWORD_RECOVERY_SENT"
        )
    }

    override suspend fun logout() {
        // no-op for in-memory fallback
    }
}
