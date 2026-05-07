package com.magpie.magpie.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Feed : Screen("feed")
    data object Search : Screen("search")
    data object MyProfile : Screen("profile/me")
    data object UserProfile : Screen("profile/{userId}") {
        fun createRoute(userId: Int): String = "profile/$userId"

    /** Shell principal com bottom bar; `username` vem do login. */
    data object Main : Screen("main")
}

/** Rotas internas da bottom bar */
sealed class MainTab(val route: String) {
    data object Home : MainTab("tab_home")
    data object Search : MainTab("tab_search")
    data object Rate : MainTab("tab_rate")
    data object Profile : MainTab("tab_profile")
    data object More : MainTab("tab_more")

    companion object {
        val entries: List<MainTab> = listOf(Home, Search, Rate, Profile, More)
    }
}
