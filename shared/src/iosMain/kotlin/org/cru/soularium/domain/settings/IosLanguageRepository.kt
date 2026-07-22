package org.cru.soularium.domain.settings

import androidx.compose.ui.text.intl.Locale
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

/**
 * iOS [LanguageRepository] backed by the `AppleLanguages` user-defaults key —
 * the same key iOS's per-app language Settings screen writes.
 *
 * Setting the language writes `AppleLanguages` and **takes effect on the next
 * launch** (iOS has no in-process locale hot swap); the picker still reflects
 * the choice immediately via the emitted flow. The current language is read as
 * the effective preferred language mapped onto a supported language, or null
 * when none matches (follow the system default).
 *
 * [supportedLanguages] is read from the bundle's `CFBundleLocalizations`.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class IosLanguageRepository : LanguageRepository {
    private companion object {
        const val APPLE_LANGUAGES_KEY = "AppleLanguages"
        const val BASE_LOCALIZATION = "Base"

        private val BundledLanguages by lazy {
            NSBundle.mainBundle.localizations
                .filterIsInstance<String>()
                .filterNot { it == BASE_LOCALIZATION }
                .map { Locale(it) }
        }

        private fun readApplicationLanguage(): Locale? {
            val preferred = NSLocale.preferredLanguages.firstOrNull() as? String ?: return null
            val current = Locale(preferred)
            return BundledLanguages.firstOrNull {
                it.language == current.language && (it.script.isEmpty() || it.script == current.script)
            }
        }
    }

    override val supportedLanguages get() = BundledLanguages

    private val language = MutableStateFlow(readApplicationLanguage())
    override val appLanguage = language.asStateFlow()

    override suspend fun setAppLanguage(locale: Locale) {
        NSUserDefaults.standardUserDefaults.setObject(listOf(locale.toLanguageTag()), APPLE_LANGUAGES_KEY)
        language.value = locale
    }
}
