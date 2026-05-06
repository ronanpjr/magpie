package com.magpie.magpie.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.magpie.magpie.R

data class UserProfileUiModel(
    val displayName: String,
    val username: String,
    val bio: String?,
    val avatarUrl: String?,
    val followerCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean
)

enum class UserProfileViewType {
    ME,
    OTHER
}

@Composable
fun UserProfileScreen(
    paddingValues: PaddingValues,
    viewModel: UserProfileViewModel,
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState = viewModel.uiState.collectAsState()
    val reviews = ProfileReviewPlaceholder.sample()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        when (val state = uiState.value) {
            is UserProfileUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UserProfileUiState.Success -> {
                val profile = state.profile
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ProfileHeader(
                            viewType = viewModel.viewType,
                            profile = profile,
                            onFollowToggle = { viewModel.followToggle() },
                            onEditProfile = onEditProfile
                        )
                    }
                    item {
                        ProfileStats(
                            followerCount = profile.followerCount,
                            followingCount = profile.followingCount,
                            onFollowersClick = onFollowersClick,
                            onFollowingClick = onFollowingClick
                        )
                    }
                    item {
                        ProfileBio(bio = profile.bio)
                    }
                    item {
                        Text(
                            text = stringResource(R.string.profile_label_reviews),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    items(reviews) { review ->
                        ReviewPlaceholderCard(review = review)
                    }
                    item {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 40.dp)
                        ) {
                            Text(text = stringResource(R.string.profile_action_logout))
                        }
                    }
                }
            }
            is UserProfileUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error loading profile",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Overloaded version for backward compatibility with hardcoded data
 */
@Composable
fun UserProfileScreen(
    paddingValues: PaddingValues,
    viewType: UserProfileViewType,
    profile: UserProfileUiModel,
    onFollowToggle: () -> Unit,
    onEditProfile: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onLogout: () -> Unit
) {
    val reviews = ProfileReviewPlaceholder.sample()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeader(
                viewType = viewType,
                profile = profile,
                onFollowToggle = onFollowToggle,
                onEditProfile = onEditProfile
            )
        }
        item {
            ProfileStats(
                followerCount = profile.followerCount,
                followingCount = profile.followingCount,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick
            )
        }
        item {
            ProfileBio(bio = profile.bio)
        }
        item {
            Text(
                text = stringResource(R.string.profile_label_reviews),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(reviews) { review ->
            ReviewPlaceholderCard(review = review)
        }
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 40.dp)
            ) {
                Text(text = stringResource(R.string.profile_action_logout))
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    viewType: UserProfileViewType,
    profile: UserProfileUiModel,
    onFollowToggle: () -> Unit,
    onEditProfile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarBlock(
                    avatarUrl = profile.avatarUrl,
                    displayName = profile.displayName
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.profile_username_format, profile.username),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (viewType == UserProfileViewType.ME) {
                OutlinedButton(
                    onClick = onEditProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.profile_action_edit))
                }
            } else {
                Button(
                    onClick = onFollowToggle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (profile.isFollowing) {
                            stringResource(R.string.profile_action_unfollow)
                        } else {
                            stringResource(R.string.profile_action_follow)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarBlock(avatarUrl: String?, displayName: String) {
    if (avatarUrl.isNullOrBlank()) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    } else {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ProfileStats(
    followerCount: Int,
    followingCount: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StatCard(
            value = followerCount,
            label = stringResource(R.string.profile_label_followers),
            onClick = onFollowersClick,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = followingCount,
            label = stringResource(R.string.profile_label_following),
            onClick = onFollowingClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileBio(bio: String?) {
    if (bio.isNullOrBlank()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.profile_bio_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.profile_label_bio),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class ProfileReviewPlaceholder(
    val title: String,
    val subtitle: String,
    val body: String,
    val rating: String
) {
    companion object {
        @Composable
        fun sample(): List<ProfileReviewPlaceholder> = listOf(
            ProfileReviewPlaceholder(
                title = stringResource(R.string.profile_review_placeholder_title_one),
                subtitle = stringResource(R.string.profile_review_placeholder_subtitle_one),
                body = stringResource(R.string.profile_review_placeholder_body_one),
                rating = stringResource(R.string.profile_review_placeholder_rating_one)
            ),
            ProfileReviewPlaceholder(
                title = stringResource(R.string.profile_review_placeholder_title_two),
                subtitle = stringResource(R.string.profile_review_placeholder_subtitle_two),
                body = stringResource(R.string.profile_review_placeholder_body_two),
                rating = stringResource(R.string.profile_review_placeholder_rating_two)
            ),
            ProfileReviewPlaceholder(
                title = stringResource(R.string.profile_review_placeholder_title_three),
                subtitle = stringResource(R.string.profile_review_placeholder_subtitle_three),
                body = stringResource(R.string.profile_review_placeholder_body_three),
                rating = stringResource(R.string.profile_review_placeholder_rating_three)
            )
        )
    }
}

@Composable
private fun ReviewPlaceholderCard(review: ProfileReviewPlaceholder) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = review.rating,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = review.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = review.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
