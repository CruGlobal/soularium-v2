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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_done
import org.cru.soularium.generated.resources.action_share
import org.cru.soularium.generated.resources.contact_save_conversation
import org.cru.soularium.generated.resources.q1_prompt
import org.cru.soularium.generated.resources.q2_prompt
import org.cru.soularium.generated.resources.q3_prompt
import org.cru.soularium.generated.resources.q4_prompt
import org.cru.soularium.generated.resources.q5_prompt
import org.cru.soularium.generated.resources.question_index
import org.cru.soularium.generated.resources.summary_great_talking
import org.cru.soularium.generated.resources.summary_share_prompt
import org.cru.soularium.generated.resources.summary_thats_a_wrap
import org.cru.soularium.generated.resources.summary_title
import org.cru.soularium.model.CardPick
import org.cru.soularium.ui.content.CardAsset
import org.cru.soularium.ui.theme.QuestionProgressColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val SELECTIONS_COLUMNS = 3
private const val TOTAL_QUESTIONS = 5

/**
 * Holds the data for a single participant's summary card.
 *
 * @param participantIndex zero-based index of the participant.
 * @param name display name.
 * @param selections the participant's final picks grouped by question, sorted
 *   by questionNumber. Q1/Q2 hold 3 cards; Q3–Q5 hold 1 card each. A question
 *   with no picks is omitted from the list.
 */
data class ParticipantSummary(val participantIndex: Int, val name: String, val selections: List<QuestionSelections>)

/**
 * One question's worth of picks within a [ParticipantSummary].
 */
data class QuestionSelections(val questionNumber: Int, val cardIds: List<Int>)

/**
 * Groups a conversation's picks into per-question [QuestionSelections]: keeps only the
 * final picks, orders the sections by question number, and orders each section's cards by
 * pick order. Shared by both summary entry points so they can't drift apart.
 */
internal fun List<CardPick>.toQuestionSelections(): List<QuestionSelections> = filter { it.isFinal }
    .groupBy { it.questionNumber }
    .entries
    .sortedBy { it.key }
    .map { (questionNumber, questionPicks) ->
        QuestionSelections(
            questionNumber = questionNumber,
            cardIds = questionPicks.sortedBy { it.pickOrder }.map { it.cardId },
        )
    }

/**
 * End-of-conversation summary ("Life in Pictures") screen. Shows each
 * participant's picks broken down per question with share / add-contact actions.
 */
@Composable
fun SummaryLayout(state: ConversationPresenter.UiState.Summary, modifier: Modifier = Modifier) {
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
                    onShare = {
                        state.eventSink(ConversationPresenter.UiEvent.Summary.Share(current.participantIndex))
                    },
                    onAddContact = {
                        state.eventSink(
                            ConversationPresenter.UiEvent.Summary.StartCollectingContact(current.participantIndex),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            // Done button — pinned to the bottom outside the scroll area
            Button(
                onClick = { state.eventSink(ConversationPresenter.UiEvent.Summary.Done) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .sizeIn(minHeight = 48.dp)
            ) {
                Text(
                    text = stringResource(Res.string.action_done),
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

        Spacer(modifier = Modifier.height(24.dp))

        // Per-question breakdown sections
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            participant.selections.forEach { section ->
                QuestionSelectionsSection(section, modifier = Modifier.fillMaxWidth())
            }
        }

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
        ) {
            Text(
                text = stringResource(Res.string.action_share),
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
        ) {
            Text(
                text = stringResource(Res.string.contact_save_conversation),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * One question's block: numbered "Question N of 5" label in the question's
 * accent color, the question prompt, and a row of the selected images. Rows
 * always render [SELECTIONS_COLUMNS] cells wide so that a one-image question
 * (Q3–Q5) left-aligns its pick within the same grid as the three-image
 * questions (Q1/Q2).
 */
@Composable
internal fun QuestionSelectionsSection(section: QuestionSelections, modifier: Modifier = Modifier) {
    val accentColor = QuestionProgressColors.getOrElse(section.questionNumber - 1) {
        QuestionProgressColors.first()
    }
    val cards = section.cardIds.map(CardAsset::fromId)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.question_index, section.questionNumber, TOTAL_QUESTIONS),
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
        )
        Text(
            text = stringResource(questionPromptResource(section.questionNumber)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            cards.forEach { card ->
                Image(
                    painter = painterResource(card.thumbnail ?: card.full),
                    contentDescription = card.contentDescription?.let { stringResource(it) },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                )
            }
            // Pad short rows so single-image questions align to the grid
            repeat(SELECTIONS_COLUMNS - cards.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun questionPromptResource(questionNumber: Int): StringResource = when (questionNumber) {
    1 -> Res.string.q1_prompt
    2 -> Res.string.q2_prompt
    3 -> Res.string.q3_prompt
    4 -> Res.string.q4_prompt
    else -> Res.string.q5_prompt
}
