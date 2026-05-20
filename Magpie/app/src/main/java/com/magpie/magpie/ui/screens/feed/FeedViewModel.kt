package com.magpie.magpie.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewReadDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedUiState {
    data object Loading : FeedUiState()
    data class Success(
        val items: List<ReviewReadDto>,
        val usedPersonalizedFeed: Boolean
    ) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class FeedViewModel(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading
            _uiState.value = loadFeedPage()
        }
    }

    private suspend fun loadFeedPage(): FeedUiState {
        return try {
            val page = reviewRepository.getFeed(page = 1, limit = 40)
            FeedUiState.Success(
                items = page.items,
                usedPersonalizedFeed = true
            )
        } catch (e: Exception) {
            try {
                val page = reviewRepository.getPopularFeed(page = 1, limit = 40)
                FeedUiState.Success(
                    items = page.items,
                    usedPersonalizedFeed = false
                )
            } catch (inner: Exception) {
                FeedUiState.Error(inner.message ?: e.message ?: "unknown")
            }
        }
    }
}
