package com.magpie.magpie.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.catalog.CatalogRepository
import com.magpie.magpie.data.catalog.models.TrackReadDto
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewReadDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TrackUiState {
    object Loading : TrackUiState()
    data class Success(
        val track: TrackReadDto,
        val reviews: List<ReviewReadDto>
    ) : TrackUiState()
    data class Error(val message: String) : TrackUiState()
}

class TrackViewModel(
    private val catalogRepository: CatalogRepository,
    private val reviewRepository: ReviewRepository,
    private val trackId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow<TrackUiState>(TrackUiState.Loading)
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    init {
        loadTrackDetails()
    }

    fun loadTrackDetails() {
        viewModelScope.launch {
            try {
                _uiState.value = TrackUiState.Loading
                val track = catalogRepository.getTrack(trackId)
                val reviews = try {
                    reviewRepository.getReviews(targetType = "track", targetId = trackId)
                } catch (e: Exception) {
                    emptyList()
                }
                _uiState.value = TrackUiState.Success(track, reviews)
            } catch (e: Exception) {
                _uiState.value = TrackUiState.Error(e.message ?: "Failed to load track details")
            }
        }
    }
}
