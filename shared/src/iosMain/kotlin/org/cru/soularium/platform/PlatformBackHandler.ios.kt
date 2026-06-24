package org.cru.soularium.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back affordance for a standalone Compose destination;
    // the conversation is exited through in-app controls.
}
