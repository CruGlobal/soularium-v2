package org.cru.soularium.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun SoulariumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SoulariumLightColors,
        typography = soulariumTypography(),
        content = content,
    )
}
