package com.magpie.magpie.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.magpie.magpie.data.catalog.models.AlbumReadDto

@Entity(tableName = "cached_album")
data class CachedAlbum(
    @PrimaryKey val id: Int,
    val spotifyId: String,
    val title: String,
    val artistName: String,
    val artistId: Int,
    val imageUrl: String?,
    val releaseDate: String?,
    val albumType: String?,
    val avgRating: Double,
    val reviewCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > TTL

    fun toDto(): AlbumReadDto = AlbumReadDto(
        id = id,
        spotifyId = spotifyId,
        title = title,
        artistName = artistName,
        artistId = artistId,
        imageUrl = imageUrl,
        releaseDate = releaseDate,
        albumType = albumType,
        avgRating = avgRating,
        reviewCount = reviewCount
    )

    companion object {
        private const val TTL = 86_400_000L // 24 hours

        fun fromDto(dto: AlbumReadDto): CachedAlbum = CachedAlbum(
            id = dto.id,
            spotifyId = dto.spotifyId,
            title = dto.title,
            artistName = dto.artistName,
            artistId = dto.artistId,
            imageUrl = dto.imageUrl,
            releaseDate = dto.releaseDate,
            albumType = dto.albumType,
            avgRating = dto.avgRating,
            reviewCount = dto.reviewCount
        )
    }
}
