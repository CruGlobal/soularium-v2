package org.cru.soularium.ui.resources

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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.resource_feedback_subject
import org.cru.soularium.ui.external.ExternalScreen
import org.cru.soularium.ui.resources.terms.TermsScreen
import org.jetbrains.compose.resources.getString

internal const val URL_MYSOULARIUM = "https://mysoularium.com"
internal const val URL_CRU_SOULARIUM =
    "https://www.cru.org/us/en/train-and-grow/share-the-gospel/evangelism-principles/soularium.html"
internal const val URL_PRIVACY_POLICY = "https://www.cru.org/about/privacy.html"
internal const val EMAIL_FEEDBACK = "Soularium@cru.org"

@AssistedInject
class ResourcesPresenter(@Assisted private val navigator: Navigator) : Presenter<ResourcesPresenter.UiState> {
    data class UiState(val eventSink: (UiEvent) -> Unit = {}) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent
        data object OpenTerms : UiEvent
        data object OpenMySoularium : UiEvent
        data object OpenCruSoularium : UiEvent
        data object OpenPrivacyPolicy : UiEvent
        data object SendFeedback : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        return UiState { event ->
            when (event) {
                UiEvent.Back -> navigator.pop()
                UiEvent.OpenTerms -> navigator.goTo(TermsScreen)
                UiEvent.OpenMySoularium -> navigator.goTo(ExternalScreen.Url(URL_MYSOULARIUM))
                UiEvent.OpenCruSoularium -> navigator.goTo(ExternalScreen.Url(URL_CRU_SOULARIUM))
                UiEvent.OpenPrivacyPolicy -> navigator.goTo(ExternalScreen.Url(URL_PRIVACY_POLICY))
                UiEvent.SendFeedback -> scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    val subject = getString(Res.string.resource_feedback_subject)
                    navigator.goTo(ExternalScreen.Email(EMAIL_FEEDBACK, subject))
                }
            }
        }
    }

    @CircuitInject(ResourcesScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): ResourcesPresenter
    }
}
