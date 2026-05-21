package org.cru.soularium.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.ui.conversation.ConversationHost
import org.cru.soularium.ui.past.PastConversationsViewModel
import org.cru.soularium.ui.screens.AboutScreen
import org.cru.soularium.ui.screens.AppLocale
import org.cru.soularium.ui.screens.CardsAndQuestionsScreen
import org.cru.soularium.ui.screens.HomeScreen
import org.cru.soularium.ui.screens.IntroScreen
import org.cru.soularium.ui.screens.PastConversationsScreen
import org.cru.soularium.ui.screens.ResourcesScreen
import org.cru.soularium.ui.screens.SettingsScreen
import org.cru.soularium.ui.screens.TermsScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    // In-memory until device-state persistence lands; see SettingsScreen TODO.
    var locale by remember { mutableStateOf(AppLocale.EN) }

    // Start destination is HOME until first-launch routing (Intro/Terms) is
    // wired against device-state persistence in a later phase.
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.SPLASH) { StubScreen("Splash") }
        composable(Routes.INTRO) {
            IntroScreen(onContinue = { navController.navigate(Routes.TERMS) })
        }
        composable(Routes.TERMS) {
            TermsScreen(
                onAgree = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onStartConversation = {
                    navController.navigate(
                        Routes.conversation(SessionId.random().value, SessionKind.GROUP.name),
                    )
                },
                onMySoularium = {
                    navController.navigate(
                        Routes.conversation(SessionId.random().value, SessionKind.SOLO.name),
                    )
                },
                onMenuPastConversations = { navController.navigate(Routes.PAST) },
                onMenuAbout = { navController.navigate(Routes.ABOUT) },
                onMenuResources = { navController.navigate(Routes.RESOURCES) },
                onMenuCardsAndQuestions = { navController.navigate(Routes.CARDS_AND_QUESTIONS) },
                onMenuSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.PAST) {
            val viewModel = koinViewModel<PastConversationsViewModel>()
            val completed by viewModel.completed.collectAsState()
            val bookmarked by viewModel.bookmarked.collectAsState()
            PastConversationsScreen(
                completed = completed,
                bookmarked = bookmarked,
                onOpen = { sessionId ->
                    val item = (completed + bookmarked).firstOrNull { it.sessionId == sessionId }
                    if (item != null) {
                        navController.navigate(
                            Routes.conversation(sessionId.value, item.kind.name),
                        )
                    }
                },
                onDelete = { viewModel.delete(it) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.RESOURCES) {
            ResourcesScreen(
                onBack = { navController.popBackStack() },
                onOpenTerms = { navController.navigate(Routes.TERMS) },
            )
        }
        composable(Routes.CARDS_AND_QUESTIONS) {
            CardsAndQuestionsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                selectedLocale = locale,
                onLocaleSelected = { locale = it },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(
                navArgument(Routes.ARG_SESSION_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_KIND) { type = NavType.StringType },
            ),
        ) { entry ->
            val sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID).orEmpty()
            val kind = entry.arguments?.getString(Routes.ARG_KIND)
                ?.let { runCatching { SessionKind.valueOf(it) }.getOrNull() }
                ?: SessionKind.GROUP
            ConversationHost(
                sessionId = SessionId(sessionId),
                kind = kind,
                onExit = { navController.popBackStack() },
            )
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
