package com.magpie.magpie.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.magpie.magpie.data.auth.models.UserRead
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SearchFilter { All, Users, Artists, Albums, Tracks }

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchFilter = SearchFilter.All,
    val users: List<UserRead> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    paddingValues: PaddingValues,
    onUserClick: (Int) -> Unit,
    searchUsers: suspend (String) -> List<UserRead>
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.All) }
    var state by remember { mutableStateOf(SearchUiState()) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(query, filter) {
        job?.cancel()
        if (query.isBlank()) {
            state = state.copy(query = query, selectedFilter = filter, users = emptyList(), isLoading = false, error = null)
            return@LaunchedEffect
        }
        job = scope.launch {
            delay(300)
            state = state.copy(query = query, selectedFilter = filter, isLoading = true, error = null)
            state = try {
                state.copy(users = searchUsers(query.trim()), isLoading = false)
            } catch (e: Exception) {
                state.copy(users = emptyList(), isLoading = false, error = e.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search") }
        )
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SearchFilter.entries) { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item.name) }
                )
            }
        }
        if (state.isLoading) CircularProgressIndicator()
        state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.users) { user ->
                AssistChip(onClick = { onUserClick(user.id) }, label = { Text("${user.displayName} @${user.username}") })
            }
        }
    }
}
