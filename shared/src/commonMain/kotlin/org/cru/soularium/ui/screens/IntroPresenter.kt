package org.cru.soularium.ui.screens

import androidx.compose.runtime.Composable
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
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.ui.nav.IntroScreen
import org.cru.soularium.ui.terms.TermsScreen

@AssistedInject
class IntroPresenter(@Assisted private val navigator: Navigator, private val deviceStateRepo: DeviceStateRepository) :
    Presenter<IntroPresenter.UiState> {

    data class UiState(val eventSink: (UiEvent) -> Unit) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Continue : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        return UiState { event ->
            when (event) {
                UiEvent.Continue -> {
                    scope.launch { deviceStateRepo.markIntroSeen() }
                    navigator.goTo(TermsScreen)
                }
            }
        }
    }

    @CircuitInject(IntroScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): IntroPresenter
    }
}
