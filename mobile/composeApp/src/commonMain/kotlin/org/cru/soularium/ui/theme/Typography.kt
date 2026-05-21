package org.cru.soularium.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import soularium.composeapp.generated.resources.OpenSans_Bold
import soularium.composeapp.generated.resources.OpenSans_BoldItalic
import soularium.composeapp.generated.resources.OpenSans_Italic
import soularium.composeapp.generated.resources.OpenSans_Light
import soularium.composeapp.generated.resources.OpenSans_LightItalic
import soularium.composeapp.generated.resources.OpenSans_Regular
import soularium.composeapp.generated.resources.OpenSans_Semibold
import soularium.composeapp.generated.resources.OpenSans_SemiboldItalic
import soularium.composeapp.generated.resources.Res

@Composable
fun openSansFamily(): FontFamily =
    FontFamily(
        Font(Res.font.OpenSans_Regular, FontWeight.Normal),
        Font(Res.font.OpenSans_Italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.OpenSans_Light, FontWeight.Light),
        Font(Res.font.OpenSans_LightItalic, FontWeight.Light, FontStyle.Italic),
        Font(Res.font.OpenSans_Semibold, FontWeight.SemiBold),
        Font(Res.font.OpenSans_SemiboldItalic, FontWeight.SemiBold, FontStyle.Italic),
        Font(Res.font.OpenSans_Bold, FontWeight.Bold),
        Font(Res.font.OpenSans_BoldItalic, FontWeight.Bold, FontStyle.Italic),
    )

@Composable
fun soulariumTypography(): Typography {
    val openSans = openSansFamily()
    val base = TextStyle(fontFamily = openSans)
    return Typography(
        displayLarge = base.copy(fontWeight = FontWeight.Light, fontSize = 48.sp),
        displayMedium = base.copy(fontWeight = FontWeight.Light, fontSize = 36.sp),
        headlineLarge = base.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
        headlineMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
        headlineSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        bodyLarge = base.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = base.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall = base.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        labelSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    )
}
