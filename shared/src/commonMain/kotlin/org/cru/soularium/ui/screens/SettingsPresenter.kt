package org.cru.soularium.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.locale_en
import org.cru.soularium.generated.resources.locale_es
import org.cru.soularium.generated.resources.locale_fr
import org.cru.soularium.generated.resources.locale_pl
import org.cru.soularium.generated.resources.locale_zh_hans
import org.cru.soularium.ui.nav.SettingsScreen
import org.jetbrains.compose.resources.StringResource

/**
 * Supported app locales. Each entry carries its IETF [code] (persisted in
 * device state) and maps to its display-name string resource.
 *
 * The chosen locale is persisted via the device-state store. Runtime
 * application of a non-system locale still depends on a Compose-resources
 * locale-override API (CMP 1.8+); until then the picker records the
 * preference and reflects it in the UI.
 */
enum class AppLocale(
    val code: String,
) {
    EN("en"),
    ES("es"),
    FR("fr"),
    PL("pl"),
    ZH_HANS("zh-Hans"),
    ;

    val labelRes: StringResource
        get() =
            when (this) {
                EN -> Res.string.locale_en
                ES -> Res.string.locale_es
                FR -> Res.string.locale_fr
                PL -> Res.string.locale_pl
                ZH_HANS -> Res.string.locale_zh_hans
            }

    companion object {
        /** Maps a stored locale code back to an [AppLocale], defaulting to [EN]. */
        fun fromCode(code: String?): AppLocale = entries.firstOrNull { it.code == code } ?: EN
    }
}

@AssistedInject
class SettingsPresenter(
    @Assisted private val navigator: Navigator,
    private val deviceStateRepo: DeviceStateRepository,
) : Presenter<SettingsPresenter.UiState> {

    data class UiState(
        val selectedLocale: AppLocale,
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent
        data class SelectLocale(val locale: AppLocale) : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        val deviceState by remember { deviceStateRepo.deviceState }.collectAsState(DeviceState())
        return UiState(
            selectedLocale = AppLocale.fromCode(deviceState.locale),
        ) { event ->
            when (event) {
                UiEvent.Back -> navigator.pop()
                is UiEvent.SelectLocale -> scope.launch { deviceStateRepo.setLocale(event.locale.code) }
            }
        }
    }

    @CircuitInject(SettingsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): SettingsPresenter
    }
}
