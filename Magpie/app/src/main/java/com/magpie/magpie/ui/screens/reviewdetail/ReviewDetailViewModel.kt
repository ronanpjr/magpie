package com.magpie.magpie.ui.screens.reviewdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewReadDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReviewDetailUiState {
    data object Loading : ReviewDetailUiState()
    data class Success(val review: ReviewReadDto) : ReviewDetailUiState()
    data class Error(val message: String) : ReviewDetailUiState()
}

class ReviewDetailViewModel(
    private val reviewRepository: ReviewRepository,
    private val reviewId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewDetailUiState>(ReviewDetailUiState.Loading)
    val uiState: StateFlow<ReviewDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ReviewDetailUiState.Loading
            _uiState.value = try {
                ReviewDetailUiState.Success(reviewRepository.getReview(reviewId))
            } catch (e: Exception) {
                ReviewDetailUiState.Error(e.message ?: "unknown")
            }
        }
    }

    fun toggleLike() {
        val current = _uiState.value
        if (current !is ReviewDetailUiState.Success) return
        val review = current.review
        viewModelScope.launch {
            try {
                if (review.likedByMe) {
                    reviewRepository.unlikeReview(review.id)
                } else {
                    reviewRepository.likeReview(review.id)
                }
                _uiState.value = ReviewDetailUiState.Success(
                    reviewRepository.getReview(review.id)
                )
            } catch (_: Exception) {
                load()
            }
        }
    }
}
