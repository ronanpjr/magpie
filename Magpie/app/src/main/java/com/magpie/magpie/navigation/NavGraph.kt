package com.magpie.magpie.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.magpie.magpie.R
import com.magpie.magpie.data.auth.RemoteAuthRepository
import com.magpie.magpie.data.auth.api.AuthApiService
import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.network.RetrofitClient
import com.magpie.magpie.data.profile.UserProfileRepository
import com.magpie.magpie.data.review.ReviewRepository
import com.magpie.magpie.data.review.api.ReviewApiService
import com.magpie.magpie.ui.main.MainShell
import com.magpie.magpie.ui.screens.auth.LoginScreen
import com.magpie.magpie.ui.screens.auth.RegisterScreen
import com.magpie.magpie.ui.screens.profile.UserProfileScreen
import com.magpie.magpie.ui.screens.profile.UserProfileViewModel
import com.magpie.magpie.ui.screens.profile.UserProfileViewType
import com.magpie.magpie.ui.screens.shell.MagpieBottomNav
import kotlinx.coroutines.launch

@Composable
fun MagpieNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember(context) { TokenManager(context.applicationContext) }
    val authRepository = remember(context) {
        RetrofitClient.initialize(context.applicationContext, tokenManager)
        val authApiService = RetrofitClient.createService(AuthApiService::class.java)
        RemoteAuthRepository(authApiService, tokenManager)
    }
    val authApiService = remember(context) {
        RetrofitClient.createService(AuthApiService::class.java)
    }
    val reviewApiService = remember(context) {
        RetrofitClient.createService(ReviewApiService::class.java)
    }
    val reviewRepository = remember(context) {
        ReviewRepository(reviewApiService, tokenManager)
    }
    val userProfileRepository = remember(context) {
        UserProfileRepository(authApiService, tokenManager, reviewRepository)
    }
    val startDestination = remember(tokenManager) {
        if (tokenManager.hasValidToken()) Screen.Feed.route else Screen.Login.route
    }
    val scope = rememberCoroutineScope()
    var authErrorCode by remember { mutableStateOf<String?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(
        Screen.Feed.route,
        Screen.Search.route,
        Screen.MyProfile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MagpieBottomNav(
                    currentDestination = currentRoute,
                    onFeedClick = {
                        navController.navigate(Screen.Feed.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProfileClick = {
                        navController.navigate(Screen.MyProfile.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    paddingValues = innerPadding,
                    onLogin = { username, password ->
                        scope.launch {
                            val result = authRepository.login(username, password)
                            authErrorCode = if (result.success) null else result.message
                            if (result.success && result.username != null) {
                                navController.navigate("${Screen.Main.route}/${result.username}") {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }
                    },
                    onNavigateToRegister = {
                        authErrorCode = null
                        navController.navigate(Screen.Register.route)
                    },
                    errorMessage = authErrorCode.asErrorMessage()
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    paddingValues = innerPadding,
                    onRegister = { username, email, password, displayName ->
                        scope.launch {
                            val result = authRepository.register(username, email, password, displayName)
                            authErrorCode = if (result.success) null else result.message
                            if (result.success) {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Register.route) { inclusive = true }
                                }
                            }
                        }
                    },
                    onNavigateToLogin = {
                        authErrorCode = null
                        navController.navigateUp()
                    },
                    errorMessage = authErrorCode.asErrorMessage()
                )
            }

            composable(Screen.Feed.route) {
                val viewModel = remember {
                    UserProfileViewModel(
                        repository = userProfileRepository,
                        viewType = UserProfileViewType.ME
                    )
                }
                UserProfileScreen(
                    paddingValues = innerPadding,
                    viewModel = viewModel,
                    onFollowersClick = {},
                    onFollowingClick = {},
                    onEditProfile = {},
                    onLogout = {
                        authErrorCode = null
                        scope.launch { authRepository.logout() }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Search.route) {
                val viewModel = remember {
                    UserProfileViewModel(
                        repository = userProfileRepository,
                        viewType = UserProfileViewType.ME
                    )
                }
                UserProfileScreen(
                    paddingValues = innerPadding,
                    viewModel = viewModel,
                    onFollowersClick = {},
                    onFollowingClick = {},
                    onEditProfile = {},
                    onLogout = {
                        authErrorCode = null
                        scope.launch { authRepository.logout() }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.MyProfile.route) {
                val viewModel = remember {
                    UserProfileViewModel(
                        repository = userProfileRepository,
                        viewType = UserProfileViewType.ME
                    )
                }
                UserProfileScreen(
                    paddingValues = innerPadding,
                    viewModel = viewModel,
                    onFollowersClick = {},
                    onFollowingClick = {},
                    onEditProfile = {},
                    onLogout = {
                        authErrorCode = null
                        scope.launch { authRepository.logout() }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.UserProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                val viewModel = remember {
                    UserProfileViewModel(
                        repository = userProfileRepository,
                        userId = userId,
                        viewType = UserProfileViewType.OTHER
                    )
                }
                UserProfileScreen(
                    paddingValues = innerPadding,
                    viewModel = viewModel,
                    onFollowersClick = {},
                    onFollowingClick = {},
                    onEditProfile = {},
                route = "${Screen.Main.route}/{username}",
                arguments = listOf(navArgument("username") { type = NavType.StringType })
            ) { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username").orEmpty()
                MainShell(
                    username = username,
                    onLogout = {
                        authErrorCode = null
                        scope.launch { authRepository.logout() }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun String?.asErrorMessage(): String? {
    return when (this) {
        "USER_EXISTS" -> stringResource(R.string.auth_error_user_exists)
        "USER_NOT_FOUND" -> stringResource(R.string.auth_error_user_not_found)
        "INVALID_CREDENTIALS" -> stringResource(R.string.auth_error_invalid_credentials)
        else -> null
    }
}
