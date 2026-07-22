package org.cru.soularium.ui.util

import androidx.compose.ui.text.intl.Locale
import platform.Foundation.NSLocale
import platform.Foundation.localizedStringForLocaleIdentifier

actual fun Locale.getDisplayName(inLocale: Locale): String = NSLocale(localeIdentifier = inLocale.toLanguageTag())
    .localizedStringForLocaleIdentifier(toLanguageTag())
