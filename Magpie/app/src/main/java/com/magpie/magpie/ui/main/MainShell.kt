package com.magpie.magpie.ui.main

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
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
import com.magpie.magpie.ui.screens.menu.HelpScreen
import com.magpie.magpie.ui.screens.menu.MenuScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    username: String,
    onLogout: () -> Unit,
    onReviewClick: (Int) -> Unit,
    onAuthorClick: (Int) -> Unit,
    onWriteReviewClick: (String, Int) -> Unit,
    homeScreen: @Composable () -> Unit,
    profileScreen: @Composable (onEditProfile: () -> Unit, onFollowersClick: (Int) -> Unit, onFollowingClick: (Int) -> Unit) -> Unit,
    profileEditScreen: @Composable (PaddingValues, () -> Unit) -> Unit,
    followersScreen: @Composable (PaddingValues, Int) -> Unit,
    followingScreen: @Composable (PaddingValues, Int) -> Unit,
    artistScreen: @Composable (PaddingValues, Int, () -> Unit, (Int) -> Unit) -> Unit,
    albumScreen: @Composable (PaddingValues, Int, () -> Unit, (Int) -> Unit, (Int) -> Unit, () -> Unit, (Int) -> Unit, (Int) -> Unit) -> Unit,
    trackScreen: @Composable (PaddingValues, Int, () -> Unit, (Int) -> Unit, (Int) -> Unit, () -> Unit, (Int) -> Unit, (Int) -> Unit) -> Unit,
    searchScreen: @Composable (onArtistClick: (Int) -> Unit, onAlbumClick: (Int) -> Unit, onTrackClick: (Int) -> Unit) -> Unit,
    rateScreen: @Composable () -> Unit
) {
    val innerNav = rememberNavController()

    val navBackStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val selectedTab = MainTab.getTabByRoute(currentRoute) ?: MainTab.Home

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
            composable(
                route = MainTab.Home.route,
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(250)) }
            ) {
                homeScreen()
            }
            composable(
                route = MainTab.Search.route,
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(250)) }
            ) {
                searchScreen({ innerNav.navigate("artist/$it") }, { innerNav.navigate("album/$it") }, { innerNav.navigate("track/$it") })
            }
            composable(
                route = MainTab.Rate.route,
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(250)) }
            ) {
                rateScreen()
            }
            composable(
                route = MainTab.Profile.route,
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(250)) }
            ) {
                profileScreen({ innerNav.navigate("profile/edit") }, { userId -> innerNav.navigate("profile/followers/$userId") }, { userId -> innerNav.navigate("profile/following/$userId") })
            }
            composable(
                route = MainTab.More.route,
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(250)) }
            ) {
                MenuScreen(
                    onNavigateToHelp = { innerNav.navigate("help") },
                    onBackClick = { innerNav.popBackStack() }
                )
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
                val albumId = backStackEntry.arguments?.getInt("albumId") ?: 0
                albumScreen(
                    PaddingValues(0.dp),
                    albumId,
                    { innerNav.popBackStack() },
                    { artistId -> innerNav.navigate("artist/$artistId") },
                    { trackId -> innerNav.navigate("track/$trackId") },
                    { onWriteReviewClick("album", albumId) },
                    { reviewId -> onReviewClick(reviewId) },
                    { authorId -> onAuthorClick(authorId) }
                )
            }
            composable("track/{trackId}", arguments = listOf(navArgument("trackId") { type = NavType.IntType })) { backStackEntry ->
                val trackId = backStackEntry.arguments?.getInt("trackId") ?: 0
                trackScreen(
                    PaddingValues(0.dp),
                    trackId,
                    { innerNav.popBackStack() },
                    { artistId -> innerNav.navigate("artist/$artistId") },
                    { albumId -> innerNav.navigate("album/$albumId") },
                    { onWriteReviewClick("track", trackId) },
                    { reviewId -> onReviewClick(reviewId) },
                    { authorId -> onAuthorClick(authorId) }
                )
            }
            composable("help") {
                HelpScreen(
                    onBackClick = { innerNav.popBackStack() }
                )
            }
        }
    }
}
