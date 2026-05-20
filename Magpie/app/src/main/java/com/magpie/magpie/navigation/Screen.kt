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
    data object ProfileEdit : Screen("profile/edit")
    data object ProfileFollowers : Screen("profile/followers/{userId}") {
        fun createRoute(userId: Int): String = "profile/followers/$userId"
    }
    data object ProfileFollowing : Screen("profile/following/{userId}") {
        fun createRoute(userId: Int): String = "profile/following/$userId"
    }

    data object ArtistDetail : Screen("catalog/artist/{artistId}") {
        fun createRoute(artistId: Int): String = "catalog/artist/$artistId"
    }

    data object AlbumDetail : Screen("catalog/album/{albumId}") {
        fun createRoute(albumId: Int): String = "catalog/album/$albumId"
    }

    data object TrackDetail : Screen("catalog/track/{trackId}") {
        fun createRoute(trackId: Int): String = "catalog/track/$trackId"
    }

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
        
        fun getTabByRoute(route: String?): MainTab {
            if (route == null) return Home
            return entries.find { it.route == route }
                ?: when {
                    route.startsWith("profile/") -> Profile
                    route.startsWith("artist/") -> Search
                    route.startsWith("album/") -> Search
                    else -> Home
                }
        }
    }
}
