package org.cru.soularium.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    // Start destination is HOME until first-launch routing (Intro/Terms) is
    // wired against device-state persistence in a later phase.
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.SPLASH) { StubScreen("Splash") }
        composable(Routes.INTRO) { StubScreen("Intro") }
        composable(Routes.TERMS) { StubScreen("Terms") }
        composable(Routes.HOME) { StubScreen("Home") }
        composable(Routes.PAST) { StubScreen("Past Conversations") }
        composable(Routes.ABOUT) { StubScreen("About") }
        composable(Routes.RESOURCES) { StubScreen("Resources") }
        composable(Routes.CARDS_AND_QUESTIONS) { StubScreen("Cards & Questions") }
        composable(Routes.SETTINGS) { StubScreen("Settings") }

        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType }),
        ) { entry ->
            val sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID).orEmpty()
            StubScreen("Conversation ($sessionId)")
        }

        composable(
            route = Routes.SUMMARY,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType }),
        ) { entry ->
            val sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID).orEmpty()
            StubScreen("Summary ($sessionId)")
        }
    }
}

@Composable
private fun StubScreen(label: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "TODO: $label", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
