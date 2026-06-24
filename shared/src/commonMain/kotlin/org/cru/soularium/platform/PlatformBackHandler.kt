package org.cru.soularium.platform

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform back affordance while [enabled] is true.
 *
 * On Android this is the system back button / predictive-back gesture. iOS has
 * no system-level back for a standalone Compose destination, so the actual is
 * a no-op there and the conversation is exited via in-app controls instead.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
