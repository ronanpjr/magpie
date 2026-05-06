package com.magpie.magpie.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Feed : Screen("feed")
    data object Search : Screen("search")
    data object MyProfile : Screen("profile/me")
    data object UserProfile : Screen("profile/{userId}") {
        fun createRoute(userId: Int): String = "profile/$userId"
    }
}
