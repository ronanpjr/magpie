package com.magpie.magpie.ui.screens.createreview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.models.ReviewCreateDto
import com.magpie.magpie.data.review.models.ReviewReadDto
import kotlinx.coroutines.launch
import retrofit2.HttpException

class CreateReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val templateReviewId: Int
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
        loadTemplate()
    }

    fun loadTemplate() {
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
