package com.magpie.magpie.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.catalog.CatalogRepository
import com.magpie.magpie.data.catalog.models.AlbumReadDto
import com.magpie.magpie.data.catalog.models.TrackReadDto
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewReadDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AlbumUiState {
    object Loading : AlbumUiState()
    data class Success(
        val album: AlbumReadDto,
        val tracks: List<TrackReadDto>,
        val reviews: List<ReviewReadDto>
    ) : AlbumUiState()
    data class Error(val message: String) : AlbumUiState()
}

class AlbumViewModel(
    private val catalogRepository: CatalogRepository,
    private val reviewRepository: ReviewRepository,
    private val albumId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        loadAlbumDetails()
    }

    fun loadAlbumDetails() {
        viewModelScope.launch {
            try {
                _uiState.value = AlbumUiState.Loading
                val album = catalogRepository.getAlbum(albumId)
                val tracks = try {
                    catalogRepository.getAlbumTracks(albumId)
                } catch (e: Exception) {
                    emptyList()
                }
                val reviews = try {
                    reviewRepository.getReviews(targetType = "album", targetId = albumId)
                } catch (e: Exception) {
                    emptyList()
                }
                _uiState.value = AlbumUiState.Success(album, tracks, reviews)
            } catch (e: Exception) {
                _uiState.value = AlbumUiState.Error(e.message ?: "Failed to load album details")
            }
        }
    }
}
