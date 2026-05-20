package com.magpie.magpie.ui.screens.reviewcomments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewCommentDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class ReviewCommentsUiState {
    object Loading : ReviewCommentsUiState()
    data class Ready(val comments: List<ReviewCommentDto>) : ReviewCommentsUiState()
    data class Error(val message: String) : ReviewCommentsUiState()
}

class ReviewCommentsViewModel(
    private val reviewRepository: ReviewRepository,
    private val reviewId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewCommentsUiState>(ReviewCommentsUiState.Loading)
    val uiState: StateFlow<ReviewCommentsUiState> = _uiState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refresh()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ReviewCommentsUiState.Loading
            try {
                val page = reviewRepository.getReviewComments(reviewId)
                _uiState.value = ReviewCommentsUiState.Ready(page.items)
            } catch (e: Exception) {
                _uiState.value = ReviewCommentsUiState.Error(e.message ?: "error")
            }
        }
    }

    fun postComment(body: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                reviewRepository.postReviewComment(reviewId, body)
                refresh()
                onDone()
            } catch (e: Exception) {
                _uiState.value = ReviewCommentsUiState.Error(e.message ?: "error")
            }
        }
    }

    fun vote(commentId: Int, direction: String) {
        viewModelScope.launch {
            if (_uiState.value !is ReviewCommentsUiState.Ready) return@launch
            try {
                reviewRepository.voteReviewComment(reviewId, commentId, direction)
                val page = reviewRepository.getReviewComments(reviewId)
                _uiState.value = ReviewCommentsUiState.Ready(page.items)
            } catch (e: HttpException) {
                _userMessage.value = "HTTP_${e.code()}"
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "vote_error"
            }
        }
    }
}
