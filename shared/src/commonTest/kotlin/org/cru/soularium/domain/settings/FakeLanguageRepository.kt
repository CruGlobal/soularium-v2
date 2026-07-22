package org.cru.soularium.domain.settings

import androidx.compose.ui.text.intl.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [LanguageRepository] fake for tests, seeded with an optional [initial] language. */
internal class FakeLanguageRepository(
    initial: Locale? = null,
    override val supportedLanguages: List<Locale> =
        listOf(Locale("en"), Locale("es"), Locale("fr"), Locale("pl"), Locale("zh-Hans")),
) : LanguageRepository {
    private val language = MutableStateFlow(initial)

    override val appLanguage: StateFlow<Locale?> = language.asStateFlow()

    override suspend fun setAppLanguage(locale: Locale) {
        language.value = locale
    }
}
