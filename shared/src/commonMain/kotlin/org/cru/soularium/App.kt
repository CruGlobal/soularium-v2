package org.cru.soularium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.screen.Screen
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.ui.nav.HomeScreen
import org.cru.soularium.ui.nav.IntroScreen
import org.cru.soularium.ui.nav.NavGraph
import org.cru.soularium.ui.nav.TermsScreen
import org.cru.soularium.ui.screens.SplashScreen
import org.cru.soularium.ui.theme.SoulariumTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    SoulariumTheme {
        val deviceStateRepo: DeviceStateRepository = koinInject()
        val circuit: Circuit = koinInject()
        val deviceState by remember { deviceStateRepo.deviceState }.collectAsState(initial = null)

        val startScreen: Screen? =
            when (val state = deviceState) {
                null -> null
                else -> resolveStartScreen(state)
            }

        when (val initial = startScreen) {
            null -> SplashScreen()
            else -> {
                // Capture the first resolved screen so later device-state
                // changes (e.g. agreeing to terms) don't rebuild the back stack.
                val initialScreen = remember { initial }
                NavGraph(circuit = circuit, startScreen = initialScreen)
            }
        }
    }
}

internal fun resolveStartScreen(state: DeviceState): Screen =
    when {
        !state.hasSeenIntro -> IntroScreen
        !state.agreedToTos -> TermsScreen
        else -> HomeScreen
    }
