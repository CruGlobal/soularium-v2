package org.cru.soularium.ui.screens

import androidx.compose.runtime.Composable
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import org.cru.soularium.ui.nav.CardsAndQuestionsScreen

@AssistedInject
class CardsAndQuestionsPresenter(@Assisted private val navigator: Navigator) :
    Presenter<CardsAndQuestionsPresenter.UiState> {

    data class UiState(val eventSink: (UiEvent) -> Unit) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent
    }

    @Composable
    override fun present(): UiState = UiState { event ->
        when (event) {
            UiEvent.Back -> navigator.pop()
        }
    }

    @CircuitInject(CardsAndQuestionsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): CardsAndQuestionsPresenter
    }
}
