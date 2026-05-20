package com.magpie.magpie.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.catalog.CatalogRepository
import com.magpie.magpie.data.catalog.models.ArtistReadDto
import com.magpie.magpie.data.catalog.models.AlbumReadDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ArtistUiState {
    object Loading : ArtistUiState()
    data class Success(val artist: ArtistReadDto, val albums: List<AlbumReadDto>) : ArtistUiState()
    data class Error(val message: String) : ArtistUiState()
}

class ArtistViewModel(
    private val catalogRepository: CatalogRepository,
    private val artistId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        loadArtistDetails()
    }

    fun loadArtistDetails() {
        viewModelScope.launch {
            try {
                _uiState.value = ArtistUiState.Loading
                val artist = catalogRepository.getArtist(artistId)
                val albums = try {
                    catalogRepository.getArtistAlbums(artistId)
                } catch (e: Exception) {
                    emptyList()
                }
                _uiState.value = ArtistUiState.Success(artist, albums)
            } catch (e: Exception) {
                _uiState.value = ArtistUiState.Error(e.message ?: "Failed to load artist details")
            }
        }
    }
}
