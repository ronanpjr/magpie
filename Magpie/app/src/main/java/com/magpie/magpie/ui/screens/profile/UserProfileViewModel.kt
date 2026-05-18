package com.magpie.magpie.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.auth.models.UserRead
import com.magpie.magpie.data.profile.UserProfileRepository
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UserProfileUiState {
    object Loading : UserProfileUiState()
    data class Success(val profile: UserProfileUiModel) : UserProfileUiState()
    data class Error(val message: String, val isUnauthorized: Boolean = false) : UserProfileUiState()
}

class UserProfileViewModel(
    private val repository: UserProfileRepository,
    private val userId: Int? = null,
    val viewType: UserProfileViewType = UserProfileViewType.ME
) : ViewModel() {
    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = UserProfileUiState.Loading
                val userRead = if (viewType == UserProfileViewType.ME) {
                    repository.getCurrentUser()
                } else {
                    userId?.let { repository.getUserById(it) }
                        ?: throw IllegalArgumentException("User ID required for OTHER profile view")
                }
                
                // Fetch reviews for the user
                val reviews = try {
                    val effectiveUserId = userId ?: userRead.id
                    repository.getUserReviews(effectiveUserId)
                } catch (e: Exception) {
                    // If reviews fail to load, continue with empty list
                    emptyList()
                }
                
                _uiState.value = UserProfileUiState.Success(userRead.toUiModel(reviews))
            } catch (e: Exception) {
                _uiState.value = UserProfileUiState.Error(
                    message = e.message ?: "Unknown error",
                    isUnauthorized = e is HttpException && e.code() == 401
                )
            }
        }
    }

    fun followToggle() {
        val currentState = _uiState.value
        if (currentState is UserProfileUiState.Success) {
            viewModelScope.launch {
                try {
                    val targetUserId = userId ?: currentState.profile.id
                    if (currentState.profile.isFollowing) {
                        repository.unfollowUser(targetUserId)
                    } else {
                        repository.followUser(targetUserId)
                    }
                    loadProfile()
                } catch (e: Exception) {
                    _uiState.value = UserProfileUiState.Error(e.message ?: "Failed to update follow state")
                }
            }
        }
    }

    fun editProfile(displayName: String, bio: String?) {
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>()
                updates["display_name"] = displayName
                if (bio != null) {
                    updates["bio"] = bio
                }
                val updatedUserRead = repository.updateCurrentUser(updates)
                
                // Preserve existing reviews from current state
                val currentState = _uiState.value
                val existingReviews = if (currentState is UserProfileUiState.Success) {
                    currentState.profile.reviews
                } else {
                    emptyList()
                }
                
                _uiState.value = UserProfileUiState.Success(updatedUserRead.toUiModel(existingReviews))
            } catch (e: Exception) {
                _uiState.value = UserProfileUiState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    fun refresh() {
        loadProfile()
    }

    private fun UserRead.toUiModel(reviews: List<com.magpie.magpie.data.review.models.ReviewReadDto> = emptyList()): UserProfileUiModel = UserProfileUiModel(
        id = this.id,
        displayName = this.displayName,
        username = this.username,
        bio = this.bio,
        avatarUrl = this.avatarUrl,
        followerCount = this.followerCount,
        followingCount = this.followingCount,
        isFollowing = this.isFollowing,
        reviews = reviews
    )
}
