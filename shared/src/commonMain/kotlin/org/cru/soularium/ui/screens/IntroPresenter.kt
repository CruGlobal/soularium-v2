package org.cru.soularium.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.ui.nav.TermsScreen

class IntroPresenter(
    private val navigator: Navigator,
    private val deviceStateRepo: DeviceStateRepository,
) : Presenter<IntroPresenter.UiState> {

    data class UiState(
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

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
}
