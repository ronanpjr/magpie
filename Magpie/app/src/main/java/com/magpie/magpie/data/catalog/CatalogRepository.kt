package com.magpie.magpie.data.catalog

import com.magpie.magpie.data.catalog.api.CatalogApiService
import com.magpie.magpie.data.catalog.models.ArtistReadDto
import com.magpie.magpie.data.catalog.models.AlbumReadDto
import com.magpie.magpie.data.catalog.models.TrackReadDto
import com.magpie.magpie.data.catalog.models.CatalogSearchResponseDto

class CatalogRepository(
    private val catalogApiService: CatalogApiService
) {
    suspend fun getArtist(id: Int): ArtistReadDto {
        return catalogApiService.getArtist(id)
    }

    suspend fun getArtistAlbums(artistId: Int): List<AlbumReadDto> {
        return catalogApiService.getArtistAlbums(artistId)
    }

    suspend fun getAlbum(id: Int): AlbumReadDto {
        return catalogApiService.getAlbum(id)
    }

    suspend fun getAlbumTracks(albumId: Int): List<TrackReadDto> {
        return catalogApiService.getAlbumTracks(albumId)
    }

    suspend fun search(query: String, type: String = "all", limit: Int = 10): CatalogSearchResponseDto {
        return catalogApiService.search(query, type, limit)
    }

    suspend fun getTrack(id: Int): TrackReadDto {
        return catalogApiService.getTrack(id)
    }
}
