package org.cru.soularium.ui.screens

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter

class AboutPresenter(
    private val navigator: Navigator,
) : Presenter<AboutPresenter.UiState> {

    data class UiState(
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent
    }

    @Composable
    override fun present(): UiState = UiState { event ->
        when (event) {
            UiEvent.Back -> navigator.pop()
        }
    }
}
