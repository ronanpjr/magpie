package com.magpie.magpie.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.magpie.magpie.R
import com.magpie.magpie.navigation.MainTab

private val NavBarBackground = Color(0xFF1A1C1E)
private val NavBarContent = Color(0xFFE8E8E8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    username: String,
    onLogout: () -> Unit
) {
    val innerNav = rememberNavController()
    val flatNavBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = NavBarContent,
        selectedTextColor = NavBarContent,
        unselectedIconColor = NavBarContent,
        unselectedTextColor = NavBarContent,
        indicatorColor = Color.Transparent
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = username, maxLines = 1) },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(text = stringResource(R.string.auth_logout_action))
                    }
                }
            )
        },
        bottomBar = {
            val navBackStack by innerNav.currentBackStackEntryAsState()
            val current = navBackStack?.destination

            NavigationBar(containerColor = NavBarBackground) {
                MainTab.entries.forEach { tab ->
                    val selected = current?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            innerNav.navigate(tab.route) {
                                popUpTo(innerNav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = tab.icon(), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = flatNavBarItemColors
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = innerNav,
            startDestination = MainTab.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(MainTab.Home.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_home)
            }
            composable(MainTab.Search.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_search)
            }
            composable(MainTab.Rate.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_rate)
            }
            composable(MainTab.Profile.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_profile)
            }
            composable(MainTab.More.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_more)
            }
        }
    }
}

private fun MainTab.icon() = when (this) {
    MainTab.Home -> Icons.Filled.Home
    MainTab.Search -> Icons.Filled.Search
    MainTab.Rate -> Icons.Filled.Add
    MainTab.Profile -> Icons.Filled.Person
    MainTab.More -> Icons.Filled.Menu
}

private val MainTab.labelRes: Int
    get() = when (this) {
        MainTab.Home -> R.string.nav_home
        MainTab.Search -> R.string.nav_search
        MainTab.Rate -> R.string.nav_rate
        MainTab.Profile -> R.string.nav_profile
        MainTab.More -> R.string.nav_more
    }
