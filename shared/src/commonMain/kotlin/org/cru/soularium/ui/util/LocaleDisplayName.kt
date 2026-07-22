package org.cru.soularium.ui.util

import androidx.compose.ui.text.intl.Locale

/**
 * The display name of this language localized for [inLocale], resolved by the platform's
 * locale data (Android `java.util.Locale.getDisplayName`; iOS `NSLocale`). Pass the locale
 * itself for its endonym (its name in its own language), or `Locale.current` for its name
 * in the active language.
 */
expect fun Locale.getDisplayName(inLocale: Locale): String
