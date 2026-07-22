package org.cru.soularium.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.intl.Locale
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import org.cru.soularium.domain.settings.LanguageRepository
import org.cru.soularium.ui.util.getDisplayName

@AssistedInject
class SettingsPresenter(@Assisted private val navigator: Navigator, private val languageRepo: LanguageRepository) :
    Presenter<SettingsPresenter.UiState> {

    /**
     * @param selectedLanguage the active app language, or null when following the system default.
     * @param supportedLanguages the languages the app ships translations for, in display order.
     */
    data class UiState(
        val selectedLanguage: Locale?,
        val supportedLanguages: List<Locale>,
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent

        /** Select one of the supported [locale]s. */
        data class SelectLanguage(val locale: Locale) : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        val supportedLanguages = remember { languageRepo.supportedLanguages.sortedBy { it.getDisplayName(it) } }

        return UiState(
            selectedLanguage = languageRepo.appLanguage.collectAsState().value,
            supportedLanguages = supportedLanguages,
        ) { event ->
            when (event) {
                UiEvent.Back -> navigator.pop()
                is UiEvent.SelectLanguage -> scope.launch { languageRepo.setAppLanguage(event.locale) }
            }
        }
    }

    @CircuitInject(SettingsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): SettingsPresenter
    }
}
