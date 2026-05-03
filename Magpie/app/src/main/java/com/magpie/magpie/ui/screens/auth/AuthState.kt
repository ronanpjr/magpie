package com.magpie.magpie.ui.screens.auth

data class AuthUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val errorCode: String? = null
)
