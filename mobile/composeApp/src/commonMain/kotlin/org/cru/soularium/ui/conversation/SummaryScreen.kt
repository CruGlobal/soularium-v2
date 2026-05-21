package org.cru.soularium.ui.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cru.soularium.ui.content.CardImages
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import soularium.composeapp.generated.resources.Res
import soularium.composeapp.generated.resources.action_done
import soularium.composeapp.generated.resources.action_share
import soularium.composeapp.generated.resources.cd_card_thumb
import soularium.composeapp.generated.resources.contact_save_conversation
import soularium.composeapp.generated.resources.summary_great_talking
import soularium.composeapp.generated.resources.summary_share_prompt
import soularium.composeapp.generated.resources.summary_thats_a_wrap
import soularium.composeapp.generated.resources.summary_title

/**
 * Holds the data for a single participant's summary card.
 *
 * @param participantIndex zero-based index of the participant.
 * @param name display name.
 * @param cardIds the participant's up-to-9 final image picks
 *   (Q1: 3 cards, Q2: 3 cards, Q3–Q5: 1 card each).
 */
data class ParticipantSummary(
    val participantIndex: Int,
    val name: String,
    val cardIds: List<Int>,
)

/**
 * End-of-conversation summary ("Life in Pictures") screen.
 *
 * Shows each participant's 9-card mosaic with share / add-contact actions.
 * Multiple participants are separated by a [TabRow]; a single participant is
 * shown directly without a tab bar.
 *
 * This composable is stateless except for the locally-remembered selected tab.
 * No ViewModel, no Koin, no navigation. All actions are delegated to callers.
 *
 * @param participants  one entry per participant, in order.
 * @param onShare       called with [ParticipantSummary.participantIndex] when the
 *                      Share button is tapped for that participant.
 * @param onAddContact  called with [ParticipantSummary.participantIndex] when the
 *                      "Save Conversation" button is tapped for that participant.
 * @param onDone        called when the user taps the Done button.
 * @param modifier      optional [Modifier] applied to the root [Surface].
 */
@Composable
fun SummaryScreen(
    participants: List<ParticipantSummary>,
    onShare: (Int) -> Unit,
    onAddContact: (Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val doneLabel = stringResource(Res.string.action_done)

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
            // Tab bar — only shown when there are multiple participants
            if (participants.size > 1) {
                TabRow(selectedTabIndex = selectedTabIndex) {
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

            // Participant content — scrollable area
            if (participants.isNotEmpty()) {
                val current = participants[selectedTabIndex.coerceIn(0, participants.lastIndex)]
                ParticipantSummaryContent(
                    participant = current,
                    onShare = { onShare(current.participantIndex) },
                    onAddContact = { onAddContact(current.participantIndex) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            // Done button — pinned to the bottom outside the scroll area
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .sizeIn(minHeight = 48.dp)
                    .semantics { contentDescription = doneLabel },
            ) {
                Text(
                    text = doneLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Scrollable content for a single participant's summary panel.
 */
@Composable
private fun ParticipantSummaryContent(
    participant: ParticipantSummary,
    onShare: () -> Unit,
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headingText = stringResource(Res.string.summary_title, participant.name)
    val sharePromptText = stringResource(Res.string.summary_share_prompt, participant.name)
    val shareLabel = stringResource(Res.string.action_share)
    val saveLabel = stringResource(Res.string.contact_save_conversation)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // "That's a Wrap" flourish
        Text(
            text = stringResource(Res.string.summary_thats_a_wrap),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "Great talking with you today!" body text
        Text(
            text = stringResource(Res.string.summary_great_talking),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Participant heading — "%s's Life In Pictures"
        Text(
            text = headingText,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3×3 card mosaic
        CardMosaic(
            cardIds = participant.cardIds,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Share prompt and button
        Text(
            text = sharePromptText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Share button
        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .semantics { contentDescription = shareLabel },
        ) {
            Text(
                text = shareLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add contact / Save Conversation button
        OutlinedButton(
            onClick = onAddContact,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .semantics { contentDescription = saveLabel },
        ) {
            Text(
                text = saveLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Renders up to 9 card thumbnails in a 3-column grid.
 *
 * Empty slots (fewer than 9 picks) are skipped; the grid simply has fewer rows.
 */
@Composable
private fun CardMosaic(
    cardIds: List<Int>,
    modifier: Modifier = Modifier,
) {
    val slots = cardIds.take(9)
    val rows = (slots.size + 2) / 3

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0..2) {
                    val slotIndex = row * 3 + col
                    if (slotIndex < slots.size) {
                        val cardId = slots[slotIndex]
                        val cardDesc = stringResource(Res.string.cd_card_thumb, cardId)
                        Image(
                            painter = painterResource(CardImages.thumb(cardId)),
                            contentDescription = cardDesc,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    } else {
                        // Empty placeholder to preserve grid alignment
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
