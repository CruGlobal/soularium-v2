package org.cru.soularium.ui.nav

import androidx.compose.runtime.Composable
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.ports.Sharer
import org.cru.soularium.ui.conversation.ConversationLayout
import org.cru.soularium.ui.conversation.ConversationPresenter
import org.cru.soularium.ui.screens.AboutLayout
import org.cru.soularium.ui.screens.AboutPresenter
import org.cru.soularium.ui.screens.CardsAndQuestionsLayout
import org.cru.soularium.ui.screens.CardsAndQuestionsPresenter
import org.cru.soularium.ui.screens.HomeLayout
import org.cru.soularium.ui.screens.HomePresenter
import org.cru.soularium.ui.screens.IntroLayout
import org.cru.soularium.ui.screens.IntroPresenter
import org.cru.soularium.ui.screens.PastConversationsLayout
import org.cru.soularium.ui.screens.PastConversationsPresenter
import org.cru.soularium.ui.screens.ResourcesLayout
import org.cru.soularium.ui.screens.ResourcesPresenter
import org.cru.soularium.ui.screens.SettingsLayout
import org.cru.soularium.ui.screens.SettingsPresenter
import org.cru.soularium.ui.screens.TermsLayout
import org.cru.soularium.ui.screens.TermsPresenter

@Inject
@SingleIn(AppScope::class)
class SoulariumPresenterFactory(
    private val deviceStateRepo: DeviceStateRepository,
    private val sessionRepository: SessionRepository,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val sharer: Sharer,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? = when (screen) {
        IntroScreen -> IntroPresenter(navigator, deviceStateRepo)
        TermsScreen -> TermsPresenter(navigator, deviceStateRepo)
        HomeScreen -> HomePresenter(navigator)
        PastConversationsScreen ->
            PastConversationsPresenter(navigator, sessionRepository, crashReporter)
        AboutScreen -> AboutPresenter(navigator)
        ResourcesScreen -> ResourcesPresenter(navigator)
        CardsAndQuestionsScreen -> CardsAndQuestionsPresenter(navigator)
        SettingsScreen -> SettingsPresenter(navigator, deviceStateRepo)
        is ConversationScreen ->
            ConversationPresenter(
                navigator = navigator,
                screen = screen,
                sessionRepository = sessionRepository,
                analytics = analytics,
                crashReporter = crashReporter,
                sharer = sharer,
            )
        else -> null
    }
}

object SoulariumUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? = when (screen) {
        IntroScreen -> uiOf<IntroPresenter.UiState> { state, modifier -> IntroLayout(state, modifier) }
        TermsScreen -> uiOf<TermsPresenter.UiState> { state, modifier -> TermsLayout(state, modifier) }
        HomeScreen -> uiOf<HomePresenter.UiState> { state, modifier -> HomeLayout(state, modifier) }
        PastConversationsScreen ->
            uiOf<PastConversationsPresenter.UiState> { state, modifier ->
                PastConversationsLayout(state, modifier)
            }
        AboutScreen -> uiOf<AboutPresenter.UiState> { state, modifier -> AboutLayout(state, modifier) }
        ResourcesScreen ->
            uiOf<ResourcesPresenter.UiState> { state, modifier -> ResourcesLayout(state, modifier) }
        CardsAndQuestionsScreen ->
            uiOf<CardsAndQuestionsPresenter.UiState> { state, modifier ->
                CardsAndQuestionsLayout(state, modifier)
            }
        SettingsScreen ->
            uiOf<SettingsPresenter.UiState> { state, modifier -> SettingsLayout(state, modifier) }
        is ConversationScreen ->
            uiOf<ConversationPresenter.UiState> { state, modifier -> ConversationLayout(state, modifier) }
        else -> null
    }
}

/** Helper that captures a [CircuitUiState] type parameter for a Ui body. */
private inline fun <reified T : CircuitUiState> uiOf(
    noinline body: @Composable (T, androidx.compose.ui.Modifier) -> Unit,
): Ui<T> = ui<T> { state, modifier -> body(state, modifier) }
