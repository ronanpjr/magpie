package com.magpie.magpie.di

import android.content.Context
import com.magpie.magpie.data.auth.AuthRepository
import com.magpie.magpie.data.auth.RemoteAuthRepository
import com.magpie.magpie.data.auth.api.AuthApiService
import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.network.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Singleton
    @Provides
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager {
        return TokenManager(context)
    }

    @Singleton
    @Provides
    fun provideAuthApiService(
        @ApplicationContext context: Context,
        tokenManager: TokenManager
    ): AuthApiService {
        // Initialize RetrofitClient with context and tokenManager
        RetrofitClient.initialize(context, tokenManager)
        return RetrofitClient.createService(AuthApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideAuthRepository(
        authApiService: AuthApiService,
        tokenManager: TokenManager
    ): AuthRepository {
        return RemoteAuthRepository(authApiService, tokenManager)
    }
}
