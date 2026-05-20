package com.magpie.magpie.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.magpie.magpie.R
import com.magpie.magpie.navigation.MainTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    username: String,
    onLogout: () -> Unit,
    homeScreen: @Composable () -> Unit,
    profileScreen: @Composable () -> Unit,
    searchScreen: @Composable () -> Unit
) {
    val innerNav = rememberNavController()

    val navBackStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val selectedTab = MainTab.entries.find { it.route == currentRoute } ?: MainTab.Home

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = when (selectedTab) {
                            MainTab.Home -> stringResource(R.string.feed_screen_title)
                            MainTab.Search -> stringResource(R.string.nav_search)
                            MainTab.Rate -> stringResource(R.string.nav_rate)
                            MainTab.Profile -> username
                            MainTab.More -> stringResource(R.string.nav_more)
                        },
                        maxLines = 1,
                        color = if (selectedTab == MainTab.Home) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(text = stringResource(R.string.auth_logout_action))
                    }
                }
            )
        },
        bottomBar = {
            MagpieBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    innerNav.navigate(tab.route) {
                        popUpTo(innerNav.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = innerNav,
            startDestination = MainTab.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(MainTab.Home.route) {
                homeScreen()
            }
            composable(MainTab.Search.route) {
                searchScreen()
            }
            composable(MainTab.Rate.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_rate)
            }
            composable(MainTab.Profile.route) {
                profileScreen()
            }
            composable(MainTab.More.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_more)
            }
        }
    }
}
