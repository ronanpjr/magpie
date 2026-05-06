package com.magpie.magpie.data.auth.token

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TokenManager(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        TokenDatabase::class.java,
        DATABASE_NAME
    ).build()

    private val tokenDao = database.tokenDao()

    fun saveTokens(accessToken: String, refreshToken: String? = null) {
        runBlocking {
            withContext(Dispatchers.IO) {
                tokenDao.upsertToken(
                    TokenEntity(
                        id = 0,
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    )
                )
            }
        }
    }

    fun getAccessToken(): String? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                tokenDao.getToken()?.accessToken
            }
        }
    }

    fun getRefreshToken(): String? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                tokenDao.getToken()?.refreshToken
            }
        }
    }

    fun clearTokens() {
        runBlocking {
            withContext(Dispatchers.IO) {
                tokenDao.clearToken()
            }
        }
    }

    fun hasValidToken(): Boolean {
        return !getAccessToken().isNullOrEmpty()
    }

    companion object {
        private const val DATABASE_NAME = "magpie_auth_tokens.db"
    }
}
