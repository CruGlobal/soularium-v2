package org.cru.soularium.domain.settings

import androidx.compose.ui.text.intl.Locale
import kotlinx.coroutines.flow.StateFlow

/**
 * The app language, backed by the platform's per-app language setting (Android's
 * per-app language feature; iOS's `AppleLanguages`). The platform both persists
 * and applies the choice, so selecting a language changes the displayed language.
 *
 * Note: this port speaks Compose's [Locale] (`androidx.compose.ui.text.intl.Locale`).
 * It is the one deliberate exception to the "domain must not import Compose" rule —
 * there is no first-party non-UI multiplatform locale type, and this keeps the same
 * currency type across the port, its platform impls, and the UI.
 */
interface LanguageRepository {
    /**
     * Emits the current app language and every subsequent change. Null when the effective
     * language is not one of the [supportedLanguages] (or no override is set) — nothing is selected.
     */
    val appLanguage: StateFlow<Locale?>

    /**
     * The languages the app ships translations for, read from each platform's language
     * registration mechanism: Android's `locales_config` resource and iOS's
     * `CFBundleLocalizations`.
     */
    val supportedLanguages: List<Locale>

    /** Sets the app language to one of the [supportedLanguages]. */
    suspend fun setAppLanguage(locale: Locale)
}
