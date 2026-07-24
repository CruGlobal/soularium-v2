package org.cru.soularium.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.action_share
import org.cru.soularium.generated.resources.summary_load_failed
import org.cru.soularium.generated.resources.summary_share_prompt
import org.cru.soularium.generated.resources.summary_title
import org.cru.soularium.ui.content.CardAsset
import org.cru.soularium.ui.nav.ConversationSummaryScreen
import org.jetbrains.compose.resources.stringResource

/**
 * Read-only summary rendered when a completed session is reopened from
 * PastConversations. Shows each participant's final-picks mosaic with a Share
 * action; Back returns to the past-conversations list. No gameplay affordances
 * (no Add Contact, no Done → Conclude).
 */
@CircuitInject(ConversationSummaryScreen::class, AppScope::class)
@Composable
fun ConversationSummaryLayout(state: ConversationSummaryPresenter.UiState, modifier: Modifier = Modifier) {
    val participants = state.participants
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            if (participants.size > 1) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    participants.forEachIndexed { index, participant ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = participant.name,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                }
            }

            val panelModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            when {
                state.error != null -> SummaryErrorPanel(modifier = panelModifier)
                state.isLoading -> SummaryLoadingPanel(modifier = panelModifier)
                participants.isNotEmpty() -> {
                    val current = participants[selectedTabIndex.coerceIn(0, participants.lastIndex)]
                    SummaryParticipantContent(
                        participant = current,
                        onShare = {
                            state.eventSink(ConversationSummaryPresenter.UiEvent.Share(current.participantIndex))
                        },
                        modifier = panelModifier,
                    )
                }
                // Loaded, but genuinely empty — leave the panel blank rather
                // than showing a spinner that would never resolve.
                else -> Spacer(modifier = panelModifier)
            }

            OutlinedButton(
                onClick = { state.eventSink(ConversationSummaryPresenter.UiEvent.Back) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .sizeIn(minHeight = 48.dp),
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

@Composable
private fun SummaryLoadingPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SummaryErrorPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.summary_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun SummaryParticipantContent(
    participant: ParticipantSummary,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headingText = stringResource(Res.string.summary_title, participant.name)
    val sharePromptText = stringResource(Res.string.summary_share_prompt, participant.name)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headingText,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        CardMosaic(
            cards = participant.cardIds.map(CardAsset::fromId),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = sharePromptText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp),
        ) {
            Text(
                text = stringResource(Res.string.action_share),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
