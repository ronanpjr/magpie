package com.magpie.magpie.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.magpie.magpie.data.catalog.models.ArtistReadDto

@Entity(tableName = "cached_artist")
data class CachedArtist(
    @PrimaryKey val id: Int,
    val spotifyId: String,
    val name: String,
    val imageUrl: String?,
    val genres: String,
    val avgRating: Double,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > TTL

    fun toDto(): ArtistReadDto = ArtistReadDto(
        id = id,
        spotifyId = spotifyId,
        name = name,
        imageUrl = imageUrl,
        genres = if (genres.isEmpty()) emptyList() else genres.split(","),
        avgRating = avgRating
    )

    companion object {
        private const val TTL = 86_400_000L // 24 hours

        fun fromDto(dto: ArtistReadDto): CachedArtist = CachedArtist(
            id = dto.id,
            spotifyId = dto.spotifyId,
            name = dto.name,
            imageUrl = dto.imageUrl,
            genres = dto.genres.joinToString(","),
            avgRating = dto.avgRating
        )
    }
}
