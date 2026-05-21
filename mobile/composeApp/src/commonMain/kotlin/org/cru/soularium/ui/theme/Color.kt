package org.cru.soularium.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val SoulariumOrange = Color(0xFFF05D2C)
val SoulariumOrangeLight = Color(0xFFF27619)
val SoulariumDark = Color(0xFF1A1A1A)
val SoulariumBackground = Color(0xFFECEAEB)
val SoulariumSurface = Color(0xFFFFFFFF)
val SoulariumOnSurface = Color(0xFF1A1A1A)
val SoulariumSurfaceVariant = Color(0xFFF3F1F2)
val SoulariumOnSurfaceVariant = Color(0xFF595759)
val SoulariumOutline = Color(0xFF8C8A8B)
val SoulariumOutlineVariant = Color(0xFFCFCDCE)
val SoulariumError = Color(0xFFBA1A1A)

val QuestionProgressColors =
    listOf(
        Color(0xFF17BD97),
        Color(0xFF16A986),
        Color(0xFF1C9AA4),
        Color(0xFF25A9C4),
        Color(0xFF1680BD),
    )

val SoulariumLightColors =
    lightColorScheme(
        primary = SoulariumOrange,
        onPrimary = Color.White,
        secondary = SoulariumOrangeLight,
        onSecondary = Color.White,
        background = SoulariumBackground,
        onBackground = SoulariumOnSurface,
        surface = SoulariumSurface,
        onSurface = SoulariumOnSurface,
        surfaceVariant = SoulariumSurfaceVariant,
        onSurfaceVariant = SoulariumOnSurfaceVariant,
        outline = SoulariumOutline,
        outlineVariant = SoulariumOutlineVariant,
        error = SoulariumError,
        onError = Color.White,
    )
