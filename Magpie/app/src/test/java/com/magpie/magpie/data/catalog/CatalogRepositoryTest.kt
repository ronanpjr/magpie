package com.magpie.magpie.data.catalog

import com.magpie.magpie.data.catalog.api.CatalogApiService
import com.magpie.magpie.data.catalog.models.AlbumReadDto
import com.magpie.magpie.data.catalog.models.ArtistReadDto
import com.magpie.magpie.data.catalog.models.CatalogSearchResponseDto
import com.magpie.magpie.data.catalog.models.TrackReadDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CatalogRepositoryTest {

    private val catalogApiService = mockk<CatalogApiService>()
    private lateinit var repository: CatalogRepository

    @Before
    fun setUp() {
        repository = CatalogRepository(catalogApiService)
    }

    @Test
    fun `getArtist delegates to api service and returns artist`() {
        runTest {
            val expected = ArtistReadDto(
                id = 1, spotifyId = "s1", name = "Artist",
                imageUrl = null, genres = emptyList(), avgRating = 4.5
            )
            coEvery { catalogApiService.getArtist(1) } returns expected

            val result = repository.getArtist(1)

            assertEquals(expected, result)
            coVerify { catalogApiService.getArtist(1) }
        }
    }

    @Test
    fun `getArtistAlbums delegates and returns album list`() {
        runTest {
            val expected = listOf(
                AlbumReadDto(id = 1, spotifyId = "s1", title = "Album", artistName = "A", artistId = 1)
            )
            coEvery { catalogApiService.getArtistAlbums(1) } returns expected

            val result = repository.getArtistAlbums(1)

            assertEquals(expected, result)
            coVerify { catalogApiService.getArtistAlbums(1) }
        }
    }

    @Test
    fun `getAlbum delegates and returns album`() {
        runTest {
            val expected = AlbumReadDto(id = 1, spotifyId = "s1", title = "Album", artistName = "A", artistId = 1)
            coEvery { catalogApiService.getAlbum(1) } returns expected

            val result = repository.getAlbum(1)

            assertEquals(expected, result)
            coVerify { catalogApiService.getAlbum(1) }
        }
    }

    @Test
    fun `getAlbumTracks delegates and returns track list`() {
        runTest {
            val expected = listOf(
                TrackReadDto(id = 1, spotifyId = "s1", title = "Track", artistName = "A",
                    albumTitle = "Album", albumId = 1)
            )
            coEvery { catalogApiService.getAlbumTracks(1) } returns expected

            val result = repository.getAlbumTracks(1)

            assertEquals(expected, result)
            coVerify { catalogApiService.getAlbumTracks(1) }
        }
    }

    @Test
    fun `search delegates with correct query params`() {
        runTest {
            val expected = CatalogSearchResponseDto()
            coEvery { catalogApiService.search("test", "album", 5) } returns expected

            val result = repository.search("test", "album", 5)

            assertEquals(expected, result)
            coVerify { catalogApiService.search("test", "album", 5) }
        }
    }

    @Test
    fun `getTrack delegates and returns track`() {
        runTest {
            val expected = TrackReadDto(id = 1, spotifyId = "s1", title = "Track", artistName = "A",
                albumTitle = "Album", albumId = 1)
            coEvery { catalogApiService.getTrack(1) } returns expected

            val result = repository.getTrack(1)

            assertEquals(expected, result)
            coVerify { catalogApiService.getTrack(1) }
        }
    }
}
