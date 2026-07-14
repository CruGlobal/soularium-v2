package org.cru.soularium.ui.resources.terms

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
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.ui.nav.HomeScreen

@AssistedInject
class TermsPresenter(@Assisted private val navigator: Navigator, private val deviceStateRepo: DeviceStateRepository) :
    Presenter<TermsPresenter.UiState> {

    /**
     * @param showAgree whether the Agree call-to-action should be offered — true only
     *   while the user has not yet accepted the terms (the Intro → Terms gate). When the
     *   screen is reached from Resources after agreeing, only Back is shown.
     */
    data class UiState(val showAgree: Boolean, val eventSink: (UiEvent) -> Unit) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Agree : UiEvent
        data object Back : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        // Null until device state resolves; treat "unknown" as already-agreed so the
        // Agree button never flashes for users reviewing the terms from Resources.
        val deviceState by remember { deviceStateRepo.deviceState }.collectAsState(initial = null)
        return UiState(showAgree = deviceState?.agreedToTos == false) { event ->
            when (event) {
                UiEvent.Agree -> {
                    scope.launch { deviceStateRepo.markTosAgreed() }
                    navigator.resetRoot(HomeScreen)
                }
                UiEvent.Back -> navigator.pop()
            }
        }
    }

    @CircuitInject(TermsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): TermsPresenter
    }
}
