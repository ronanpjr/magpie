package com.magpie.magpie.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magpie.magpie.data.auth.models.UserRead
import com.magpie.magpie.data.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UserProfileUiState {
    object Loading : UserProfileUiState()
    data class Success(val profile: UserProfileUiModel) : UserProfileUiState()
    data class Error(val message: String) : UserProfileUiState()
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
                _uiState.value = UserProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun followToggle() {
        // TODO: Implement follow/unfollow API call
        val currentState = _uiState.value
        if (currentState is UserProfileUiState.Success) {
            val updatedProfile = currentState.profile.copy(
                isFollowing = !currentState.profile.isFollowing
            )
            _uiState.value = UserProfileUiState.Success(updatedProfile)
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

    private fun UserRead.toUiModel(reviews: List<com.magpie.magpie.data.review.models.ReviewReadDto> = emptyList()): UserProfileUiModel = UserProfileUiModel(
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
