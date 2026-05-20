package com.magpie.magpie.ui.screens.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.magpie.magpie.R
import com.magpie.magpie.data.review.models.ReviewReadDto
import com.magpie.magpie.ui.components.StarRatingRow
import kotlinx.coroutines.launch

private sealed interface FeedGridEntry {
    data class SourceBanner(val personalized: Boolean) : FeedGridEntry
    data class ReviewItem(val review: ReviewReadDto) : FeedGridEntry
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    viewModel: FeedViewModel,
    onReviewClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            scope.launch {
                viewModel.refresh()
                refreshing = false
            }
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .pullRefresh(pullState)
    ) {
        when (val state = uiState) {
            is FeedUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is FeedUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.feed_error_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = stringResource(R.string.feed_retry))
                    }
                }
            }
            is FeedUiState.Success -> {
                if (state.items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.feed_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                } else {
                    val gridEntries = remember(state.items, state.usedPersonalizedFeed) {
                        buildList {
                            add(FeedGridEntry.SourceBanner(state.usedPersonalizedFeed))
                            state.items.forEach { add(FeedGridEntry.ReviewItem(it)) }
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = gridEntries,
                            key = { entry ->
                                when (entry) {
                                    is FeedGridEntry.SourceBanner -> "feed_source_banner"
                                    is FeedGridEntry.ReviewItem -> entry.review.id
                                }
                            },
                            span = { entry ->
                                when (entry) {
                                    is FeedGridEntry.SourceBanner -> GridItemSpan(maxLineSpan)
                                    is FeedGridEntry.ReviewItem -> GridItemSpan(1)
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is FeedGridEntry.SourceBanner ->
                                    FeedSourceBanner(personalized = entry.personalized)
                                is FeedGridEntry.ReviewItem ->
                                    FeedReviewGridCard(
                                        review = entry.review,
                                        onClick = { onReviewClick(entry.review.id) }
                                    )
                            }
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FeedSourceBanner(
    personalized: Boolean,
    modifier: Modifier = Modifier
) {
    val text = if (personalized) {
        stringResource(R.string.feed_banner_following)
    } else {
        stringResource(R.string.feed_banner_trending)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun FeedReviewGridCard(
    review: ReviewReadDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RowAuthor(review = review)
            val imageUrl = review.targetImageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.feed_cover_content_description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
            }
            Text(
                text = review.targetTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = review.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StarRatingRow(rating = review.rating, starSize = 16.dp)
        }
    }
}

@Composable
private fun RowAuthor(review: ReviewReadDto) {
    val author = review.author
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val avatar = author.avatarUrl
        if (!avatar.isNullOrBlank()) {
            AsyncImage(
                model = avatar,
                contentDescription = stringResource(R.string.feed_avatar_content_description),
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = author.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.profile_username_format, author.username),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
