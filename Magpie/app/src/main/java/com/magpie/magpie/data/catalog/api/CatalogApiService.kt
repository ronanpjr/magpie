package com.magpie.magpie.data.catalog.api

import com.magpie.magpie.data.catalog.models.ArtistReadDto
import com.magpie.magpie.data.catalog.models.AlbumReadDto
import com.magpie.magpie.data.catalog.models.TrackReadDto
import com.magpie.magpie.data.catalog.models.CatalogSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApiService {
    @GET("catalog/artists/{id}")
    suspend fun getArtist(
        @Path("id") id: Int
    ): ArtistReadDto

    @GET("catalog/artists/{id}/albums")
    suspend fun getArtistAlbums(
        @Path("id") id: Int
    ): List<AlbumReadDto>

    @GET("catalog/albums/{id}")
    suspend fun getAlbum(
        @Path("id") id: Int
    ): AlbumReadDto

    @GET("catalog/albums/{id}/tracks")
    suspend fun getAlbumTracks(
        @Path("id") id: Int
    ): List<TrackReadDto>

    @GET("catalog/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "all",
        @Query("limit") limit: Int = 10
    ): CatalogSearchResponseDto
}
