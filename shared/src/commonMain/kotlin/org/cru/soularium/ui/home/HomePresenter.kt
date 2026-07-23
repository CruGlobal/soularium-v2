package org.cru.soularium.ui.home

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
import org.cru.soularium.model.Session
import org.cru.soularium.model.SessionId
import org.cru.soularium.ui.nav.CardsAndQuestionsScreen
import org.cru.soularium.ui.nav.ConversationScreen
import org.cru.soularium.ui.nav.PastConversationsScreen
import org.cru.soularium.ui.resources.ResourcesScreen
import org.cru.soularium.ui.resources.about.AboutScreen
import org.cru.soularium.ui.settings.SettingsScreen

@AssistedInject
class HomePresenter(@Assisted private val navigator: Navigator) : Presenter<HomePresenter.UiState> {

    data class UiState(
        val showMenu: MutableState<Boolean> = mutableStateOf(false),
        val eventSink: (UiEvent) -> Unit = {},
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object StartGroupConversation : UiEvent
        data object StartSoloConversation : UiEvent
        data object OpenPastConversations : UiEvent
        data object OpenAbout : UiEvent
        data object OpenResources : UiEvent
        data object OpenCardsAndQuestions : UiEvent
        data object OpenSettings : UiEvent
    }

    @Composable
    override fun present() = UiState(
        showMenu = rememberSaveable { mutableStateOf(false) },
    ) { event ->
        when (event) {
            UiEvent.StartGroupConversation -> navigator.goTo(ConversationScreen(SessionId.random(), Session.Kind.GROUP))
            UiEvent.StartSoloConversation -> navigator.goTo(ConversationScreen(SessionId.random(), Session.Kind.SOLO))
            UiEvent.OpenPastConversations -> navigator.goTo(PastConversationsScreen)
            UiEvent.OpenAbout -> navigator.goTo(AboutScreen)
            UiEvent.OpenResources -> navigator.goTo(ResourcesScreen)
            UiEvent.OpenCardsAndQuestions -> navigator.goTo(CardsAndQuestionsScreen)
            UiEvent.OpenSettings -> navigator.goTo(SettingsScreen)
        }
    }

    @CircuitInject(HomeScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): HomePresenter
    }
}
