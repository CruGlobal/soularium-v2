package org.cru.soularium.domain.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.text.intl.Locale
import androidx.core.os.LocaleListCompat
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.ccci.gto.android.common.androidx.core.app.LocaleConfigCompat

/**
 * Android [LanguageRepository] backed by the per-app language feature via
 * [AppCompatDelegate]. Setting the language recreates the activity to apply it
 * immediately and is persisted by AppCompat's auto-store service (API < 33) or
 * the system (API 33+). Requires the host activity to be an `AppCompatActivity`.
 *
 * [supportedLanguages] is read from the app's registered `locales_config` resource
 * (the same list the system per-app language screen uses).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class AndroidLanguageRepository(private val context: Context) : LanguageRepository {
    override val supportedLanguages: List<Locale> = readSupportedLanguages()

    private val language = MutableStateFlow(readApplicationLanguage())
    override val appLanguage = language.asStateFlow()

    override suspend fun setAppLanguage(locale: Locale) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale.toLanguageTag()))
        language.value = locale
    }

    private fun readApplicationLanguage(): Locale? = AppCompatDelegate.getApplicationLocales()
        .takeUnless { it.isEmpty }
        ?.get(0)
        ?.let { Locale(it.toLanguageTag()) }

    private fun readSupportedLanguages(): List<Locale> {
        val locales = LocaleConfigCompat.getSupportedLocales(context) ?: return emptyList()
        return (0 until locales.size()).mapNotNull { i -> locales.get(i)?.let { Locale(it.toLanguageTag()) } }
    }
}
