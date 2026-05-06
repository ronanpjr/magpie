package com.magpie.magpie.di

import android.content.Context
import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.network.RetrofitClient
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.api.ReviewApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReviewModule {

    @Singleton
    @Provides
    fun provideReviewApiService(
        @ApplicationContext context: Context,
        tokenManager: TokenManager
    ): ReviewApiService {
        RetrofitClient.initialize(context, tokenManager)
        return RetrofitClient.createService(ReviewApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideReviewRepository(
        reviewApiService: ReviewApiService,
        tokenManager: TokenManager
    ): ReviewRepository {
        return ReviewRepository(reviewApiService, tokenManager)
    }
}
