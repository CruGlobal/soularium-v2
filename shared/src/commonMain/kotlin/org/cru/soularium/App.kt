package org.cru.soularium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import org.cru.soularium.di.LocalSoulariumAppGraph
import org.cru.soularium.di.SoulariumAppGraph
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.ui.external.rememberExternalScreenInterceptor
import org.cru.soularium.ui.home.HomeScreen
import org.cru.soularium.ui.nav.IntroScreen
import org.cru.soularium.ui.resources.terms.TermsScreen
import org.cru.soularium.ui.screens.SplashScreen
import org.cru.soularium.ui.theme.SoulariumTheme
import org.cru.soularium.ui.util.RecomposeOnAppLanguageChange

@Composable
fun App(graph: SoulariumAppGraph) {
    CompositionLocalProvider(LocalSoulariumAppGraph provides graph) {
        SoulariumTheme {
            RecomposeOnAppLanguageChange {
                val deviceState by remember { graph.deviceStateRepo.deviceState }.collectAsState(null)

                val startScreen: Screen? =
                    when (val state = deviceState) {
                        null -> null
                        else -> resolveStartScreen(state)
                    }

                when (val initial = startScreen) {
                    null -> SplashScreen()
                    else -> {
                        val backStack = rememberSaveableBackStack(remember { initial })
                        CircuitCompositionLocals(graph.circuit) {
                            ContentWithOverlays {
                                NavigableCircuitContent(
                                    navigator = rememberInterceptingNavigator(
                                        navigator = rememberCircuitNavigator(backStack) {},
                                        interceptors = listOf(rememberExternalScreenInterceptor()),
                                    ),
                                    backStack = backStack,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun resolveStartScreen(state: DeviceState): Screen = when {
    !state.hasSeenIntro -> IntroScreen
    !state.agreedToTos -> TermsScreen
    else -> HomeScreen
}
