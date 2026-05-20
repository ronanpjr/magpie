package com.magpie.magpie.ui.screens.createreview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.auth.models.UserRead
import com.magpie.magpie.data.catalog.CatalogRepository
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewCreateDto
import com.magpie.magpie.data.review.models.ReviewReadDto
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class CreateReviewSource {
    data class FromTemplate(val templateReviewId: Int) : CreateReviewSource()
    data class Direct(val targetType: String, val targetId: Int) : CreateReviewSource()
}

class CreateReviewViewModel(
    private val catalogRepository: CatalogRepository,
    private val reviewRepository: ReviewRepository,
    private val source: CreateReviewSource
) : ViewModel() {

    var templateReview by mutableStateOf<ReviewReadDto?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var loadFinished by mutableStateOf(false)
        private set

    var rating by mutableFloatStateOf(4f)
    var bodyText by mutableStateOf("")
    var publishLoading by mutableStateOf(false)
    var publishError by mutableStateOf<String?>(null)

    init {
        when (source) {
            is CreateReviewSource.FromTemplate -> loadTemplate(source.templateReviewId)
            is CreateReviewSource.Direct -> loadTargetInfo(source.targetType, source.targetId)
        }
    }

    fun retry() {
        when (source) {
            is CreateReviewSource.FromTemplate -> loadTemplate(source.templateReviewId)
            is CreateReviewSource.Direct -> loadTargetInfo(source.targetType, source.targetId)
        }
    }

    private fun loadTemplate(templateReviewId: Int) {
        loadFinished = false
        loadError = null
        templateReview = null
        viewModelScope.launch {
            try {
                templateReview = reviewRepository.getReview(templateReviewId)
            } catch (e: Exception) {
                loadError = e.message ?: "error"
            } finally {
                loadFinished = true
            }
        }
    }

    private fun loadTargetInfo(targetType: String, targetId: Int) {
        loadFinished = false
        loadError = null
        templateReview = null
        viewModelScope.launch {
            try {
                val (title, imageUrl, artistName) = when (targetType) {
                    "album" -> {
                        val album = catalogRepository.getAlbum(targetId)
                        Triple(album.title, album.imageUrl, album.artistName)
                    }
                    "track" -> {
                        val track = catalogRepository.getTrack(targetId)
                        Triple(track.title, track.albumImageUrl, track.artistName)
                    }
                    else -> throw IllegalArgumentException("Unknown target type: $targetType")
                }
                templateReview = ReviewReadDto(
                    id = 0,
                    author = UserRead(0, "", "", null, null, "", 0, 0, false),
                    targetType = targetType,
                    targetId = targetId,
                    targetTitle = title,
                    targetImageUrl = imageUrl,
                    artistName = artistName,
                    rating = 0.0,
                    body = null,
                    likeCount = 0,
                    likedByMe = false,
                    createdAt = "",
                    updatedAt = ""
                )
            } catch (e: Exception) {
                loadError = e.message ?: "error"
            } finally {
                loadFinished = true
            }
        }
    }

    fun publish(onSuccess: () -> Unit) {
        val t = templateReview ?: return
        val body = bodyText.trim()
        if (body.isEmpty()) {
            publishError = "empty_body"
            return
        }
        publishLoading = true
        publishError = null
        viewModelScope.launch {
            try {
                reviewRepository.createReview(
                    ReviewCreateDto(
                        targetType = t.targetType,
                        targetId = t.targetId,
                        rating = rating.toDouble(),
                        body = body
                    )
                )
                onSuccess()
            } catch (e: HttpException) {
                publishError = when (e.code()) {
                    409 -> "already_exists"
                    else -> e.message ?: "http_error"
                }
            } catch (e: Exception) {
                publishError = e.message
            } finally {
                publishLoading = false
            }
        }
    }
}
