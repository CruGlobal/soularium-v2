package org.cru.soularium.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Previews and screenshot tests have no OnBackPressedDispatcherOwner to
    // register with, and back handling is meaningless there anyway.
    if (!LocalInspectionMode.current) {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}
