package com.magpie.magpie.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.magpie.magpie.data.auth.models.UserRead

@Composable
fun UserListScreen(paddingValues: PaddingValues, title: String, users: List<UserRead>) {
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
        Text(title)
        LazyColumn {
            items(users) { user ->
                Text(text = "${user.displayName} @${user.username}")
            }
        }
    }
}
