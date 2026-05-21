package org.cru.soularium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.cru.soularium.ui.nav.DeviceStateViewModel
import org.cru.soularium.ui.nav.NavGraph
import org.cru.soularium.ui.screens.SplashScreen
import org.cru.soularium.ui.theme.SoulariumTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    SoulariumTheme {
        val deviceStateViewModel = koinViewModel<DeviceStateViewModel>()
        val startRoute by deviceStateViewModel.startRoute.collectAsState()
        when (val route = startRoute) {
            null -> SplashScreen()
            else -> {
                // Capture the first resolved route so later device-state
                // changes (e.g. agreeing to terms) don't rebuild the graph.
                val initialRoute = remember { route }
                NavGraph(
                    startDestination = initialRoute,
                    deviceStateViewModel = deviceStateViewModel,
                )
            }
        }
    }
}
