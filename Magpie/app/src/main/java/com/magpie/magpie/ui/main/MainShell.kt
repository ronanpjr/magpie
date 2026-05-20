package com.magpie.magpie.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.magpie.magpie.R
import com.magpie.magpie.navigation.MainTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    username: String,
    onLogout: () -> Unit,
    profileScreen: @Composable (onEditProfile: () -> Unit, onFollowersClick: (Int) -> Unit, onFollowingClick: (Int) -> Unit) -> Unit,
    profileEditScreen: @Composable (PaddingValues, () -> Unit) -> Unit,
    followersScreen: @Composable (PaddingValues, Int) -> Unit,
    followingScreen: @Composable (PaddingValues, Int) -> Unit,
    artistScreen: @Composable (PaddingValues, Int, () -> Unit, (Int) -> Unit) -> Unit,
    albumScreen: @Composable (PaddingValues, Int, () -> Unit, (Int) -> Unit) -> Unit,
    searchScreen: @Composable (onArtistClick: (Int) -> Unit, onAlbumClick: (Int) -> Unit) -> Unit
) {
    val innerNav = rememberNavController()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.app_name), maxLines = 1) },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(text = stringResource(R.string.auth_logout_action))
                    }
                }
            )
        },
        bottomBar = {
            val navBackStack by innerNav.currentBackStackEntryAsState()
            val currentRoute = navBackStack?.destination?.route
            val selectedTab = MainTab.getTabByRoute(currentRoute)

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
                TabPlaceholderScreen(titleRes = R.string.nav_home)
            }
            composable(MainTab.Search.route) {
                searchScreen({ innerNav.navigate("artist/$it") }, { innerNav.navigate("album/$it") })
            }
            composable(MainTab.Rate.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_rate)
            }
            composable(MainTab.Profile.route) {
                profileScreen({ innerNav.navigate("profile/edit") }, { userId -> innerNav.navigate("profile/followers/$userId") }, { userId -> innerNav.navigate("profile/following/$userId") })
            }
            composable("profile/edit") {
                profileEditScreen(PaddingValues(0.dp), { innerNav.popBackStack() })
            }
            composable("profile/followers/{userId}", arguments = listOf(navArgument("userId") { type = NavType.IntType })) { backStackEntry ->
                followersScreen(PaddingValues(0.dp), backStackEntry.arguments?.getInt("userId") ?: 0)
            }
            composable("profile/following/{userId}", arguments = listOf(navArgument("userId") { type = NavType.IntType })) { backStackEntry ->
                followingScreen(PaddingValues(0.dp), backStackEntry.arguments?.getInt("userId") ?: 0)
            }
            composable("artist/{artistId}", arguments = listOf(navArgument("artistId") { type = NavType.IntType })) { backStackEntry ->
                artistScreen(PaddingValues(0.dp), backStackEntry.arguments?.getInt("artistId") ?: 0, { innerNav.popBackStack() }, { albumId -> innerNav.navigate("album/$albumId") })
            }
            composable("album/{albumId}", arguments = listOf(navArgument("albumId") { type = NavType.IntType })) { backStackEntry ->
                albumScreen(PaddingValues(0.dp), backStackEntry.arguments?.getInt("albumId") ?: 0, { innerNav.popBackStack() }, { artistId -> innerNav.navigate("artist/$artistId") })
            }
            composable(MainTab.More.route) {
                TabPlaceholderScreen(titleRes = R.string.nav_more)
            }
        }
    }
}
