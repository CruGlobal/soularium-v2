package org.cru.soularium.ui.terms

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.runBlocking
import org.ccci.gto.android.common.compose.foundation.verticalFadingEdgeEffect
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_agree
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.terms_confirm_prompt
import org.cru.soularium.generated.resources.terms_title
import org.jetbrains.compose.resources.stringResource

private const val TERMS_MARKDOWN_PATH = "files/terms_of_use.md"

/**
 * Terms of Use screen. Renders the full Terms of Use (bundled Markdown) in a scrollable
 * body. When the user has not yet accepted the terms — the Intro → Terms gate — an Agree
 * call-to-action is shown; otherwise (reached from Resources) only Back is offered.
 */
@CircuitInject(TermsScreen::class, AppScope::class)
@Composable
fun TermsLayout(state: TermsPresenter.UiState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

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
            Text(
                text = stringResource(Res.string.terms_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp)
                    .semantics { heading() }
            )

            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (val markdown = rememberTerms()) {
                    null ->
                        CircularProgressIndicator(
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    else ->
                        Markdown(
                            content = markdown,
                            modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalFadingEdgeEffect(scrollState)
                                .verticalScroll(scrollState)
                                .padding(bottom = 24.dp)
                        )
                }
            }

            if (state.showAgree) {
                Text(
                    text = stringResource(Res.string.terms_confirm_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
                Button(
                    onClick = { state.eventSink(TermsPresenter.UiEvent.Agree) },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.action_agree),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedButton(
                onClick = { state.eventSink(TermsPresenter.UiEvent.Back) },
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(Res.string.action_back),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Loads the bundled Terms of Use Markdown.
 *
 * Under normal use the content is read asynchronously via [produceState] (null while
 * loading). Under [LocalInspectionMode] — Compose previews and Paparazzi snapshots, which
 * render a single frame — the async load would never resolve, so the content is read
 * synchronously instead.
 */
@Composable
private fun rememberTerms(): String? = when {
    // Compose previews and Paparazzi snapshots render a single frame, so the async
    // produceState load would never resolve — read the bundled Markdown synchronously.
    LocalInspectionMode.current -> remember { runBlocking { Res.readBytes(TERMS_MARKDOWN_PATH).decodeToString() } }

    else -> produceState<String?>(null) { value = Res.readBytes(TERMS_MARKDOWN_PATH).decodeToString() }.value
}
