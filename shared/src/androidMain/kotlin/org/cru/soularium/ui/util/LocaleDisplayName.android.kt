package org.cru.soularium.ui.util

import androidx.compose.ui.text.intl.Locale
import java.util.Locale as JavaLocale

actual fun Locale.getDisplayName(inLocale: Locale): String = JavaLocale.forLanguageTag(toLanguageTag())
    .getDisplayName(JavaLocale.forLanguageTag(inLocale.toLanguageTag()))
