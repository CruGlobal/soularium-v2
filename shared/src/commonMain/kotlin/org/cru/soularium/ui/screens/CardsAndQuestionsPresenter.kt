package org.cru.soularium.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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

internal const val TAB_IMAGES = 0
internal const val TAB_QUESTIONS = 1

@AssistedInject
class CardsAndQuestionsPresenter(@Assisted private val navigator: Navigator) :
    Presenter<CardsAndQuestionsPresenter.UiState> {

    data class UiState(
        val selectedTab: MutableState<Int> = mutableStateOf(TAB_IMAGES),
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent
    }

    @Composable
    override fun present(): UiState = UiState(
        selectedTab = rememberSaveable { mutableStateOf(TAB_IMAGES) },
    ) { event ->
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
