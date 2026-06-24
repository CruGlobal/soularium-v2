package org.cru.soularium.ui.nav

import androidx.compose.runtime.Composable
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.screen.Screen

/**
 * Root of the Circuit-backed navigation graph. Builds a back stack rooted at
 * [startScreen] and renders the current screen via [NavigableCircuitContent].
 */
@Composable
fun NavGraph(circuit: Circuit, startScreen: Screen) {
    val backStack = rememberSaveableBackStack(startScreen)
    val navigator = rememberCircuitNavigator(backStack) {
        // Root pop (back at start) — leave handling to the platform.
    }
    CircuitCompositionLocals(circuit) {
        NavigableCircuitContent(
            navigator = navigator,
            backStack = backStack,
        )
    }
}
