package com.magpie.magpie.data.catalog.models

import com.squareup.moshi.Json

data class ArtistReadDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "spotify_id")
    val spotifyId: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "image_url")
    val imageUrl: String? = null,
    @Json(name = "genres")
    val genres: List<String> = emptyList(),
    @Json(name = "avg_rating")
    val avgRating: Double = 0.0
)

data class AlbumReadDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "spotify_id")
    val spotifyId: String,
    @Json(name = "title")
    val title: String,
    @Json(name = "artist_name")
    val artistName: String,
    @Json(name = "artist_id")
    val artistId: Int,
    @Json(name = "image_url")
    val imageUrl: String? = null,
    @Json(name = "release_date")
    val releaseDate: String? = null,
    @Json(name = "album_type")
    val albumType: String? = null,
    @Json(name = "avg_rating")
    val avgRating: Double = 0.0,
    @Json(name = "review_count")
    val reviewCount: Int = 0
)

data class TrackReadDto(
    @Json(name = "id")
    val id: Int,
    @Json(name = "spotify_id")
    val spotifyId: String,
    @Json(name = "title")
    val title: String,
    @Json(name = "artist_name")
    val artistName: String,
    @Json(name = "album_title")
    val albumTitle: String,
    @Json(name = "album_id")
    val albumId: Int,
    @Json(name = "album_image_url")
    val albumImageUrl: String? = null,
    @Json(name = "duration_ms")
    val durationMs: Int? = null,
    @Json(name = "preview_url")
    val previewUrl: String? = null,
    @Json(name = "avg_rating")
    val avgRating: Double = 0.0,
    @Json(name = "review_count")
    val reviewCount: Int = 0
)

data class CatalogSearchResponseDto(
    @Json(name = "artists")
    val artists: List<ArtistReadDto> = emptyList(),
    @Json(name = "albums")
    val albums: List<AlbumReadDto> = emptyList(),
    @Json(name = "tracks")
    val tracks: List<TrackReadDto> = emptyList()
)
