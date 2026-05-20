package com.magpie.magpie.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.magpie.magpie.ui.screens.profile.ProfileEditScreen
import com.magpie.magpie.ui.screens.profile.UserProfileScreen
import com.magpie.magpie.ui.screens.profile.UserProfileViewModel
import com.magpie.magpie.ui.screens.profile.UserProfileViewType
import com.magpie.magpie.ui.screens.profile.UserListScreen
import com.magpie.magpie.ui.screens.search.SearchScreen
import com.magpie.magpie.data.catalog.CatalogRepository
import com.magpie.magpie.data.catalog.api.CatalogApiService
import com.magpie.magpie.ui.screens.catalog.ArtistScreen
import com.magpie.magpie.ui.screens.catalog.ArtistViewModel
import com.magpie.magpie.ui.screens.catalog.AlbumScreen
import com.magpie.magpie.ui.screens.catalog.AlbumViewModel
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
    val catalogApiService = remember(context) {
        RetrofitClient.createService(CatalogApiService::class.java)
    }
    val catalogRepository = remember(context) {
        CatalogRepository(catalogApiService)
    }
    val startDestination = Screen.Login.route
    val scope = rememberCoroutineScope()
    var authErrorCode by remember { mutableStateOf<String?>(null) }
    Scaffold { innerPadding: PaddingValues ->
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

            composable(Screen.ProfileEdit.route) {
                val viewModel = remember {
                    UserProfileViewModel(repository = userProfileRepository, viewType = UserProfileViewType.ME)
                }
                val state = viewModel.uiState.collectAsState()
                val profile = (state.value as? com.magpie.magpie.ui.screens.profile.UserProfileUiState.Success)?.profile
                if (profile != null) {
                    ProfileEditScreen(
                        paddingValues = innerPadding,
                        displayName = profile.displayName,
                        bio = profile.bio,
                        onSave = { displayName, bio -> viewModel.editProfile(displayName, bio) },
                        onCancel = { navController.popBackStack() }
                    )
                }
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
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Feed.route) { inclusive = true }
                }
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
                    onFollowersClick = { navController.navigate(Screen.ProfileFollowers.createRoute(userId)) },
                    onFollowingClick = { navController.navigate(Screen.ProfileFollowing.createRoute(userId)) },
                    onEditProfile = {},
                    onLogout = {}
                )
            }

            composable(
                route = Screen.ProfileFollowers.route,
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                val state = remember { mutableStateOf(emptyList<com.magpie.magpie.data.auth.models.UserRead>()) }
                LaunchedEffect(userId) {
                    state.value = userProfileRepository.getFollowers(userId).items
                }
                UserListScreen(paddingValues = innerPadding, title = "Followers", users = state.value)
            }

            composable(
                route = Screen.ProfileFollowing.route,
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                val state = remember { mutableStateOf(emptyList<com.magpie.magpie.data.auth.models.UserRead>()) }
                LaunchedEffect(userId) {
                    state.value = userProfileRepository.getFollowing(userId).items
                }
                UserListScreen(paddingValues = innerPadding, title = "Following", users = state.value)
            }

            composable(
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
                    },
                    searchScreen = {
                        SearchScreen(
                            paddingValues = innerPadding,
                            onUserClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                            onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                            onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetail.createRoute(albumId)) },
                            searchUsers = { query -> userProfileRepository.searchUsers(query).items },
                            searchCatalog = { query, type -> catalogRepository.search(query, type) }
                        )
                    },
                    profileScreen = {
                        val viewModel = remember {
                            UserProfileViewModel(
                                repository = userProfileRepository,
                                viewType = UserProfileViewType.ME
                            )
                        }
                        UserProfileScreen(
                            paddingValues = innerPadding,
                            viewModel = viewModel,
                            onFollowersClick = { navController.navigate(Screen.ProfileFollowers.createRoute((viewModel.uiState.value as? com.magpie.magpie.ui.screens.profile.UserProfileUiState.Success)?.profile?.id ?: 0)) },
                            onFollowingClick = { navController.navigate(Screen.ProfileFollowing.createRoute((viewModel.uiState.value as? com.magpie.magpie.ui.screens.profile.UserProfileUiState.Success)?.profile?.id ?: 0)) },
                            onEditProfile = { navController.navigate(Screen.ProfileEdit.route) },
                            onLogout = {
                                authErrorCode = null
                                scope.launch { authRepository.logout() }
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        )
                    }
                )
            }

            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(navArgument("artistId") { type = NavType.IntType })
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getInt("artistId") ?: 0
                val viewModel = remember(artistId) {
                    ArtistViewModel(catalogRepository = catalogRepository, artistId = artistId)
                }
                ArtistScreen(
                    paddingValues = innerPadding,
                    viewModel = viewModel,
                    onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetail.createRoute(albumId)) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(navArgument("albumId") { type = NavType.IntType })
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getInt("albumId") ?: 0
                val viewModel = remember(albumId) {
                    AlbumViewModel(catalogRepository = catalogRepository, reviewRepository = reviewRepository, albumId = albumId)
                }
                AlbumScreen(
                    paddingValues = innerPadding,
                    viewModel = viewModel,
                    onArtistClick = { artistId -> navController.navigate(Screen.ArtistDetail.createRoute(artistId)) },
                    onWriteReviewClick = { albumId ->
                        android.widget.Toast.makeText(context, "Avaliar álbum $albumId (em breve)", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onBackClick = { navController.popBackStack() }
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
