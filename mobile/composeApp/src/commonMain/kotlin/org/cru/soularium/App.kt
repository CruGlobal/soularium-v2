package org.cru.soularium

import androidx.compose.runtime.Composable
import org.cru.soularium.ui.nav.NavGraph
import org.cru.soularium.ui.theme.SoulariumTheme

@Composable
fun App() {
    SoulariumTheme {
        NavGraph()
    }
}
