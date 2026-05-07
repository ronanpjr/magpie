package com.magpie.magpie.ui.screens.shell

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magpie.magpie.R
import com.magpie.magpie.navigation.Screen

@Composable
fun MagpieBottomNav(
    currentDestination: String?,
    onFeedClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            NavigationBarItem(
                selected = currentDestination == Screen.Feed.route,
                onClick = onFeedClick,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_feed),
                        contentDescription = stringResource(R.string.nav_feed)
                    )
                },
                label = { Text(text = stringResource(R.string.nav_feed)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            NavigationBarItem(
                selected = currentDestination == Screen.Search.route,
                onClick = onSearchClick,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_search),
                        contentDescription = stringResource(R.string.nav_search)
                    )
                },
                label = { Text(text = stringResource(R.string.nav_search)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            NavigationBarItem(
                selected = currentDestination == Screen.MyProfile.route,
                onClick = onProfileClick,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_profile),
                        contentDescription = stringResource(R.string.nav_profile)
                    )
                },
                label = { Text(text = stringResource(R.string.nav_profile)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
