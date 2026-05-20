package com.magpie.magpie.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.magpie.magpie.data.catalog.models.TrackReadDto

@Entity(tableName = "cached_track")
data class CachedTrack(
    @PrimaryKey val id: Int,
    val spotifyId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val albumId: Int,
    val albumImageUrl: String?,
    val durationMs: Int?,
    val previewUrl: String?,
    val avgRating: Double,
    val reviewCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > TTL

    fun toDto(): TrackReadDto = TrackReadDto(
        id = id,
        spotifyId = spotifyId,
        title = title,
        artistName = artistName,
        albumTitle = albumTitle,
        albumId = albumId,
        albumImageUrl = albumImageUrl,
        durationMs = durationMs,
        previewUrl = previewUrl,
        avgRating = avgRating,
        reviewCount = reviewCount
    )

    companion object {
        private const val TTL = 86_400_000L // 24 hours

        fun fromDto(dto: TrackReadDto): CachedTrack = CachedTrack(
            id = dto.id,
            spotifyId = dto.spotifyId,
            title = dto.title,
            artistName = dto.artistName,
            albumTitle = dto.albumTitle,
            albumId = dto.albumId,
            albumImageUrl = dto.albumImageUrl,
            durationMs = dto.durationMs,
            previewUrl = dto.previewUrl,
            avgRating = dto.avgRating,
            reviewCount = dto.reviewCount
        )
    }
}
