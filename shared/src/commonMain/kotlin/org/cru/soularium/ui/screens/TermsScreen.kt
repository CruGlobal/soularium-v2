package org.cru.soularium.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_agree
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.terms_confirm_prompt
import org.cru.soularium.generated.resources.terms_review_note
import org.cru.soularium.generated.resources.terms_title
import org.cru.soularium.ui.nav.HomeScreen
import org.jetbrains.compose.resources.stringResource

class TermsPresenter(
    private val navigator: Navigator,
    private val deviceStateRepo: DeviceStateRepository,
) : Presenter<TermsPresenter.UiState> {

    data class UiState(
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Agree : UiEvent
        data object Back : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        return UiState { event ->
            when (event) {
                UiEvent.Agree -> {
                    scope.launch { deviceStateRepo.markTosAgreed() }
                    navigator.resetRoot(HomeScreen)
                }
                UiEvent.Back -> navigator.pop()
            }
        }
    }
}

/**
 * Terms of Use gate shown after the intro flow. Displays the terms summary and
 * requires the user to tap Agree before accessing Home.
 */
@Composable
fun TermsLayout(
    state: TermsPresenter.UiState,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val agreeLabel = stringResource(Res.string.action_agree)
    val backLabel = stringResource(Res.string.action_back)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.terms_title),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(Res.string.terms_confirm_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.terms_review_note),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { state.eventSink(TermsPresenter.UiEvent.Agree) },
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = agreeLabel },
            ) {
                Text(
                    text = agreeLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { state.eventSink(TermsPresenter.UiEvent.Back) },
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = backLabel },
            ) {
                Text(
                    text = backLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
