package com.magpie.magpie.ui.screens.reviewdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewReadDto
import com.magpie.magpie.data.review.models.ReviewUpdateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReviewDetailUiState {
    data object Loading : ReviewDetailUiState()
    data class Success(val review: ReviewReadDto, val isOwner: Boolean) : ReviewDetailUiState()
    data class Error(val message: String) : ReviewDetailUiState()
}

class ReviewDetailViewModel(
    private val reviewRepository: ReviewRepository,
    private val reviewId: Int,
    private val tokenManager: TokenManager?
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewDetailUiState>(ReviewDetailUiState.Loading)
    val uiState: StateFlow<ReviewDetailUiState> = _uiState.asStateFlow()

    var showEditDialog by mutableStateOf(false)
        private set
    var editRating by mutableFloatStateOf(4f)
        private set
    var editBodyText by mutableStateOf("")
        private set
    var showDeleteConfirm by mutableStateOf(false)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ReviewDetailUiState.Loading
            _uiState.value = try {
                val review = reviewRepository.getReview(reviewId)
                val currentUserId = tokenManager?.getUserId()
                val isOwner = review.isOwner || (currentUserId != null && review.author.id == currentUserId)
                ReviewDetailUiState.Success(review, isOwner)
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
                val updatedReview = reviewRepository.getReview(review.id)
                val currentUserId = tokenManager?.getUserId()
                val isOwner = updatedReview.isOwner || (currentUserId != null && updatedReview.author.id == currentUserId)
                _uiState.value = ReviewDetailUiState.Success(updatedReview, isOwner)
            } catch (_: Exception) {
                load()
            }
        }
    }

    fun openEditDialog() {
        val current = _uiState.value
        if (current !is ReviewDetailUiState.Success) return
        editRating = current.review.rating.toFloat()
        editBodyText = current.review.body ?: ""
        actionError = null
        showEditDialog = true
    }

    fun closeEditDialog() {
        showEditDialog = false
        actionError = null
    }

    fun updateEditRating(value: Float) {
        editRating = value
    }

    fun updateEditBody(value: String) {
        editBodyText = value
    }

    fun saveEdit() {
        val current = _uiState.value
        if (current !is ReviewDetailUiState.Success) return
        viewModelScope.launch {
            try {
                reviewRepository.updateReview(
                    reviewId,
                    ReviewUpdateDto(
                        rating = editRating.toDouble(),
                        body = editBodyText.trim().ifEmpty { null }
                    )
                )
                showEditDialog = false
                actionError = null
                load()
            } catch (e: Exception) {
                actionError = e.message ?: "edit_error"
            }
        }
    }

    fun openDeleteConfirm() {
        actionError = null
        showDeleteConfirm = true
    }

    fun closeDeleteConfirm() {
        showDeleteConfirm = false
        actionError = null
    }

    fun deleteReview(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                reviewRepository.deleteReview(reviewId)
                showDeleteConfirm = false
                onDeleted()
            } catch (e: Exception) {
                actionError = e.message ?: "delete_error"
            }
        }
    }
}
