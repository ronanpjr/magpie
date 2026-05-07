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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.magpie.magpie.R
import com.magpie.magpie.data.auth.RemoteAuthRepository
import com.magpie.magpie.data.auth.api.AuthApiService
import com.magpie.magpie.data.auth.token.TokenManager
import com.magpie.magpie.data.network.RetrofitClient
import com.magpie.magpie.ui.main.MainShell
import com.magpie.magpie.ui.screens.auth.LoginScreen
import com.magpie.magpie.ui.screens.auth.RegisterScreen
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
    val scope = rememberCoroutineScope()
    var authErrorCode by remember { mutableStateOf<String?>(null) }

    Scaffold { innerPadding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route
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

            composable(
                route = "${Screen.Main.route}/{username}",
                arguments = listOf(navArgument("username") { type = NavType.StringType })
            ) { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username").orEmpty()
                MainShell(
                    username = username,
                    onLogout = {
                        authErrorCode = null
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
