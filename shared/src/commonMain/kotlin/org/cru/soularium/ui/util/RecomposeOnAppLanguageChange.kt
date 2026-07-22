package org.cru.soularium.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.asContribution
import org.cru.soularium.di.LocalSoulariumAppGraph
import org.cru.soularium.domain.settings.LanguageRepository

object RecomposeOnAppLanguageChange {
    private val LocalAppLanguage = staticCompositionLocalOf<Locale?> { null }

    @ContributesTo(AppScope::class)
    interface AppGraphAccessor {
        val languageRepository: LanguageRepository
    }

    /**
     * Recomposes [content] whenever the app language changes so string resources re-resolve to the new
     * locale. The [LanguageRepository] is read from [LocalSoulariumAppGraph].
     */
    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        val appLanguage = when {
            LocalSoulariumAppGraph.isProvided -> {
                val appGraph = LocalSoulariumAppGraph.current.asContribution<AppGraphAccessor>()
                appGraph.languageRepository.appLanguage.collectAsState().value
            }

            else -> null
        }
        CompositionLocalProvider(LocalAppLanguage provides appLanguage, content = content)
    }
}
