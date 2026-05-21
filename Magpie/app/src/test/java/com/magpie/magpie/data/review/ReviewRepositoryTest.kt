package com.magpie.magpie.data.review

import com.magpie.magpie.data.auth.models.UserRead
import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.review.api.ReviewApiService
import com.magpie.magpie.data.review.models.PageDto
import com.magpie.magpie.data.review.models.ReviewCommentCreateDto
import com.magpie.magpie.data.review.models.ReviewCommentDto
import com.magpie.magpie.data.review.models.ReviewCommentVoteRequestDto
import com.magpie.magpie.data.review.models.ReviewCreateDto
import com.magpie.magpie.data.review.models.ReviewReadDto
import com.magpie.magpie.data.review.models.ReviewUpdateDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReviewRepositoryTest {

    private val reviewApiService = mockk<ReviewApiService>()
    private val tokenManager = mockk<TokenManager>()
    private lateinit var repository: ReviewRepository

    private val sampleAuthor = UserRead(
        id = 1, username = "user1", displayName = "User One", createdAt = "2024-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        repository = ReviewRepository(reviewApiService, tokenManager)
    }

    @Test
    fun `getFeed delegates and returns page`() {
        runTest {
            val expected: PageDto<ReviewReadDto> = PageDto(
                items = emptyList(), total = 0, page = 1, limit = 20, pages = 0
            )
            coEvery { reviewApiService.getFeed(1, 20) } returns expected

            val result = repository.getFeed()

            assertEquals(expected, result)
            coVerify { reviewApiService.getFeed(1, 20) }
        }
    }

    @Test
    fun `getFeed uses custom pagination`() {
        runTest {
            val expected: PageDto<ReviewReadDto> = PageDto(
                items = emptyList(), total = 0, page = 2, limit = 10, pages = 0
            )
            coEvery { reviewApiService.getFeed(2, 10) } returns expected

            val result = repository.getFeed(page = 2, limit = 10)

            assertEquals(expected, result)
            coVerify { reviewApiService.getFeed(2, 10) }
        }
    }

    @Test
    fun `getPopularFeed delegates and returns page`() {
        runTest {
            val expected: PageDto<ReviewReadDto> = PageDto(
                items = emptyList(), total = 0, page = 1, limit = 20, pages = 0
            )
            coEvery { reviewApiService.getPopularFeed(1, 20) } returns expected

            val result = repository.getPopularFeed()

            assertEquals(expected, result)
            coVerify { reviewApiService.getPopularFeed(1, 20) }
        }
    }

    @Test
    fun `getReview delegates and returns review`() {
        runTest {
            val expected = ReviewReadDto(
                id = 1, author = sampleAuthor, targetType = "album", targetId = 1,
                targetTitle = "Album", artistName = "Artist", rating = 4.0,
                createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
            )
            coEvery { reviewApiService.getReview(1) } returns expected

            val result = repository.getReview(1)

            assertEquals(expected, result)
            coVerify { reviewApiService.getReview(1) }
        }
    }

    @Test
    fun `createReview delegates with correct payload`() {
        runTest {
            val payload = ReviewCreateDto(targetType = "album", targetId = 1, rating = 4.5, body = "Great!")
            val expected = ReviewReadDto(
                id = 1, author = sampleAuthor, targetType = "album", targetId = 1,
                targetTitle = "Album", artistName = "Artist", rating = 4.5, body = "Great!",
                createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
            )
            coEvery { reviewApiService.createReview(payload) } returns expected

            val result = repository.createReview(payload)

            assertEquals(expected, result)
            coVerify { reviewApiService.createReview(payload) }
        }
    }

    @Test
    fun `getUserReviews injects bearer token`() {
        runTest {
            every { tokenManager.getAccessToken() } returns "test-access-token"
            val page: PageDto<ReviewReadDto> = PageDto(
                items = listOf(
                    ReviewReadDto(id = 1, author = sampleAuthor, targetType = "album", targetId = 1,
                        targetTitle = "Album", artistName = "Artist", rating = 3.0,
                        createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z")
                ), total = 1, page = 1, limit = 20, pages = 1
            )
            coEvery { reviewApiService.getUserReviews(1, 1, 20, "Bearer test-access-token") } returns page

            val result = repository.getUserReviews(1)

            assertEquals(1, result.size)
            coVerify { reviewApiService.getUserReviews(1, 1, 20, "Bearer test-access-token") }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `getUserReviews throws when no token`() {
        runTest {
            every { tokenManager.getAccessToken() } returns null
            repository.getUserReviews(1)
        }
    }

    @Test
    fun `getReviews injects bearer token with query params`() {
        runTest {
            every { tokenManager.getAccessToken() } returns "test-token"
            val page: PageDto<ReviewReadDto> = PageDto(
                items = emptyList(), total = 0, page = 1, limit = 20, pages = 0
            )
            coEvery {
                reviewApiService.getReviews("album", 1, null, 1, 20, "recent", "Bearer test-token")
            } returns (page)

            val result = repository.getReviews(targetType = "album", targetId = 1)

            assertEquals(emptyList<ReviewReadDto>(), result)
            coVerify {
                reviewApiService.getReviews("album", 1, null, 1, 20, "recent", "Bearer test-token")
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `getReviews throws when no token`() {
        runTest {
            every { tokenManager.getAccessToken() } returns null
            repository.getReviews()
        }
    }

    @Test
    fun `getReviewComments delegates with pagination`() {
        runTest {
            val expected: PageDto<ReviewCommentDto> = PageDto(
                items = emptyList(), total = 0, page = 1, limit = 50, pages = 0
            )
            coEvery { reviewApiService.getReviewComments(1, 1, 50) } returns expected

            val result = repository.getReviewComments(1)

            assertEquals(expected, result)
            coVerify { reviewApiService.getReviewComments(1, 1, 50) }
        }
    }

    @Test
    fun `postReviewComment delegates with correct payload`() {
        runTest {
            val expected = ReviewCommentDto(
                id = 1, author = sampleAuthor, body = "Nice!", createdAt = "2024-01-01T00:00:00Z"
            )
            coEvery {
                reviewApiService.postReviewComment(1, ReviewCommentCreateDto(body = "Nice!"))
            } returns expected

            val result = repository.postReviewComment(1, "Nice!")

            assertEquals(expected, result)
            coVerify {
                reviewApiService.postReviewComment(1, ReviewCommentCreateDto(body = "Nice!"))
            }
        }
    }

    @Test
    fun `voteReviewComment delegates with direction`() {
        runTest {
            coEvery {
                reviewApiService.voteReviewComment(1, 2, ReviewCommentVoteRequestDto(direction = "up"))
            } returns mapOf<String, String>()

            repository.voteReviewComment(1, 2, "up")

            coVerify {
                reviewApiService.voteReviewComment(1, 2, ReviewCommentVoteRequestDto(direction = "up"))
            }
        }
    }

    @Test
    fun `likeReview delegates`() {
        runTest {
            coEvery { reviewApiService.likeReview(1) } returns mapOf<String, String>()

            repository.likeReview(1)

            coVerify { reviewApiService.likeReview(1) }
        }
    }

    @Test
    fun `unlikeReview delegates`() {
        runTest {
            coEvery { reviewApiService.unlikeReview(1) } returns mapOf<String, String>()

            repository.unlikeReview(1)

            coVerify { reviewApiService.unlikeReview(1) }
        }
    }

    @Test
    fun `updateReview delegates with correct payload`() {
        runTest {
            val payload = ReviewUpdateDto(rating = 5.0, body = "Updated review")
            val expected = ReviewReadDto(
                id = 1, author = sampleAuthor, targetType = "album", targetId = 1,
                targetTitle = "Album", artistName = "Artist", rating = 5.0, body = "Updated review",
                createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
            )
            coEvery { reviewApiService.updateReview(1, payload) } returns expected

            val result = repository.updateReview(1, payload)

            assertEquals(expected, result)
            coVerify { reviewApiService.updateReview(1, payload) }
        }
    }

    @Test
    fun `deleteReview delegates`() {
        runTest {
            coEvery { reviewApiService.deleteReview(1) } returns mapOf<String, String>()

            repository.deleteReview(1)

            coVerify { reviewApiService.deleteReview(1) }
        }
    }

    @Test
    fun `createPlaceholderReview creates review with default rating 3 and null body`() {
        runTest {
            val expectedPayload = ReviewCreateDto(targetType = "album", targetId = 1, rating = 3.0, body = null)
            val returned = ReviewReadDto(
                id = 1, author = sampleAuthor, targetType = "album", targetId = 1,
                targetTitle = "Album", artistName = "Artist", rating = 3.0,
                createdAt = "2024-01-01T00:00:00Z", updatedAt = "2024-01-01T00:00:00Z"
            )
            coEvery { reviewApiService.createReview(expectedPayload) } returns returned

            val result = repository.createPlaceholderReview("album", 1)

            assertEquals(returned, result)
            coVerify { reviewApiService.createReview(expectedPayload) }
        }
    }
}
