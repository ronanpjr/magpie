package com.magpie.magpie.data.auth

import com.magpie.magpie.data.auth.api.AuthApiService
import com.magpie.magpie.data.auth.models.PasswordRecoveryRequest
import com.magpie.magpie.data.auth.models.TokenRefreshRequest
import com.magpie.magpie.data.auth.models.UserCreate
import com.magpie.magpie.data.auth.models.UserLogin
import com.magpie.magpie.data.auth.token.TokenManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class  RemoteAuthRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val mutex = Mutex()

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String
    ): AuthResult {
        return try {
            val response = authApiService.register(
                UserCreate(
                    username = username,
                    email = email,
                    password = password,
                    displayName = displayName
                )
            )

            // Save tokens
            tokenManager.saveTokens(response.accessToken, response.refreshToken, response.user.username, response.user.id)

            AuthResult(
                success = true,
                message = "REGISTER_SUCCESS",
                username = response.user.username,
                userId = response.user.id,
                displayName = response.user.displayName,
                email = email,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken
            )
        } catch (e: Exception) {
            AuthResult(
                success = false,
                message = e.message ?: "REGISTER_FAILED"
            )
        }
    }

    override suspend fun login(username: String, password: String): AuthResult {
        return try {
            val response = authApiService.login(
                UserLogin(
                    username = username,
                    password = password
                )
            )

            // Save tokens
            tokenManager.saveTokens(response.accessToken, response.refreshToken, response.user.username, response.user.id)

            AuthResult(
                success = true,
                message = "LOGIN_SUCCESS",
                username = response.user.username,
                userId = response.user.id,
                displayName = response.user.displayName,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken
            )
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "INVALID_CREDENTIALS"  // Unauthorized - wrong password or invalid credentials
                404 -> "USER_NOT_FOUND"        // Not Found - user doesn't exist
                else -> "LOGIN_FAILED"
            }
            AuthResult(
                success = false,
                message = errorMessage
            )
        } catch (e: Exception) {
            AuthResult(
                success = false,
                message = "LOGIN_FAILED"
            )
        }
    }

    override suspend fun refreshToken(refreshToken: String): AuthResult {
        return mutex.withLock {
            try {
                val response = authApiService.refreshToken(
                    TokenRefreshRequest(refreshToken = refreshToken)
                )

                // Save new tokens
                tokenManager.saveTokens(response.accessToken, response.refreshToken)

                AuthResult(
                    success = true,
                    message = "TOKEN_REFRESH_SUCCESS",
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken
                )
            } catch (e: Exception) {
                // If refresh fails, clear tokens
                tokenManager.clearTokens()
                AuthResult(
                    success = false,
                    message = e.message ?: "TOKEN_REFRESH_FAILED"
                )
            }
        }
    }

    override suspend fun passwordRecovery(emailOrUsername: String): AuthResult {
        return try {
            val response = authApiService.passwordRecovery(
                PasswordRecoveryRequest(emailOrUsername = emailOrUsername)
            )

            AuthResult(
                success = true,
                message = response["message"] ?: "PASSWORD_RECOVERY_SENT"
            )
        } catch (e: Exception) {
            AuthResult(
                success = false,
                message = e.message ?: "PASSWORD_RECOVERY_FAILED"
            )
        }
    }

    override suspend fun logout() {
        tokenManager.clearTokens()
    }
}
