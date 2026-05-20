package com.magpie.magpie.ui.screens.search

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.magpie.magpie.data.auth.models.UserRead
import com.magpie.magpie.data.catalog.models.ArtistReadDto
import com.magpie.magpie.data.catalog.models.AlbumReadDto
import com.magpie.magpie.data.catalog.models.CatalogSearchResponseDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SearchFilter { All, Users, Artists, Albums }

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchFilter = SearchFilter.All,
    val users: List<UserRead> = emptyList(),
    val artists: List<ArtistReadDto> = emptyList(),
    val albums: List<AlbumReadDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    paddingValues: PaddingValues,
    onUserClick: (Int) -> Unit,
    onArtistClick: (Int) -> Unit,
    onAlbumClick: (Int) -> Unit,
    searchUsers: suspend (String) -> List<UserRead>,
    searchCatalog: suspend (String, String) -> CatalogSearchResponseDto
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.All) }
    var state by remember { mutableStateOf(SearchUiState()) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(query, filter) {
        job?.cancel()
        if (query.isBlank() || query.trim().length < 2) {
            state = state.copy(
                query = query,
                selectedFilter = filter,
                users = emptyList(),
                artists = emptyList(),
                albums = emptyList(),
                isLoading = false,
                error = null
            )
            return@LaunchedEffect
        }
        job = scope.launch {
            delay(300)
            state = state.copy(query = query, selectedFilter = filter, isLoading = true, error = null)
            state = try {
                val cleanQuery = query.trim()
                when (filter) {
                    SearchFilter.All -> {
                        val usersDeferred = async {
                            try { searchUsers(cleanQuery) } catch (e: Exception) { emptyList() }
                        }
                        val catalogDeferred = async {
                            try { searchCatalog(cleanQuery, "all") } catch (e: Exception) { CatalogSearchResponseDto() }
                        }
                        val usersResult = usersDeferred.await()
                        val catalogResult = catalogDeferred.await()
                        state.copy(
                            users = usersResult,
                            artists = catalogResult.artists,
                            albums = catalogResult.albums,
                            isLoading = false
                        )
                    }
                    SearchFilter.Users -> {
                        state.copy(
                            users = searchUsers(cleanQuery),
                            artists = emptyList(),
                            albums = emptyList(),
                            isLoading = false
                        )
                    }
                    SearchFilter.Artists -> {
                        val catalogResult = searchCatalog(cleanQuery, "artist")
                        state.copy(
                            users = emptyList(),
                            artists = catalogResult.artists,
                            albums = emptyList(),
                            isLoading = false
                        )
                    }
                    SearchFilter.Albums -> {
                        val catalogResult = searchCatalog(cleanQuery, "album")
                        state.copy(
                            users = emptyList(),
                            artists = emptyList(),
                            albums = catalogResult.albums,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                state.copy(
                    users = emptyList(),
                    artists = emptyList(),
                    albums = emptyList(),
                    isLoading = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            label = { Text("O que você quer ouvir?") }
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SearchFilter.entries) { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Render Artists
            if (state.artists.isNotEmpty()) {
                item {
                    Text(
                        text = "Artistas",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(state.artists) { artist ->
                    SearchArtistCard(artist = artist, onClick = { onArtistClick(artist.id) })
                }
            }

            // Render Albums
            if (state.albums.isNotEmpty()) {
                item {
                    Text(
                        text = "Álbuns",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(state.albums) { album ->
                    SearchAlbumCard(album = album, onClick = { onAlbumClick(album.id) })
                }
            }

            // Render Users
            if (state.users.isNotEmpty()) {
                item {
                    Text(
                        text = "Usuários",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(state.users) { user ->
                    SearchUserCard(user = user, onClick = { onUserClick(user.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchArtistCard(artist: ArtistReadDto, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (artist.imageUrl.isNullOrBlank()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = artist.name.take(1).uppercase())
                    }
                } else {
                    AsyncImage(
                        model = artist.imageUrl,
                        contentDescription = artist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (artist.genres.isNotEmpty()) {
                    Text(
                        text = artist.genres.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchAlbumCard(album: AlbumReadDto, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(52.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (album.imageUrl.isNullOrBlank()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = album.title.take(1))
                    }
                } else {
                    AsyncImage(
                        model = album.imageUrl,
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.artistName} · ${album.releaseDate?.take(4) ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchUserCard(user: UserRead, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (user.avatarUrl.isNullOrBlank()) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    }
                } else {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
