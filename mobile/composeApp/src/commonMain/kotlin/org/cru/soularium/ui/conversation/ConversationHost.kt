package org.cru.soularium.ui.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.domain.content.Question
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.domain.session.QuestionActivity
import org.cru.soularium.domain.session.SessionEvent
import org.cru.soularium.domain.session.SessionState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val TOTAL_QUESTIONS = 5

/**
 * Single navigation destination that hosts the entire 5-question conversation
 * flow. It owns the [ConversationViewModel] and renders a subscreen derived
 * from [SessionState]; subscreens are never pushed onto the back stack.
 */
@Composable
fun ConversationHost(
    sessionId: SessionId,
    kind: SessionKind,
    onExit: () -> Unit,
    viewModel: ConversationViewModel = koinViewModel { parametersOf(sessionId) },
) {
    val state by viewModel.state.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val summaries by viewModel.summaries.collectAsState()

    LaunchedEffect(sessionId) { viewModel.ensureStarted(kind) }

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { current ->
        when (current) {
            SessionState.NotStarted -> ConversationStub("Starting…")

            SessionState.AddingParticipants ->
                AddParticipantsScreen(
                    participantNames = ui.participantNames,
                    onAddParticipant = { viewModel.dispatch(SessionEvent.AddParticipant(it)) },
                    onRemoveParticipant = { viewModel.dispatch(SessionEvent.RemoveParticipant(it)) },
                    onConfirm = { viewModel.dispatch(SessionEvent.ConfirmParticipants) },
                )

            is SessionState.InQuestion -> {
                val question = Questions.byNumber(current.questionNumber)
                val participantName =
                    ui.participantNames.getOrElse(current.activeParticipantIndex) { "" }
                when (current.activity) {
                    QuestionActivity.ShowingPrompt ->
                        QuestionPromptScreen(
                            questionNumber = current.questionNumber,
                            totalQuestions = TOTAL_QUESTIONS,
                            participantName = participantName,
                            isGroup = ui.participantNames.size > 1,
                            onBegin = { viewModel.dispatch(SessionEvent.BeginSelection) },
                        )

                    QuestionActivity.ShowingInstructions ->
                        InstructionPanelScreen(
                            onDismiss = { viewModel.dispatch(SessionEvent.DismissInstructions) },
                        )

                    QuestionActivity.SelectingRound1,
                    QuestionActivity.SelectingRound2,
                    ->
                        SelectionScreen(
                            questionNumber = current.questionNumber,
                            round = if (current.activity == QuestionActivity.SelectingRound2) 2 else 1,
                            selectedCardIds = ui.draftPicks,
                            isConfirmEnabled = isSelectionValid(question, current.activity, ui.draftPicks.size),
                            onToggleCard = { cardId ->
                                if (cardId in ui.draftPicks) {
                                    viewModel.dispatch(SessionEvent.UnpickCard(cardId))
                                } else {
                                    viewModel.dispatch(SessionEvent.PickCard(cardId))
                                }
                            },
                            onConfirm = { viewModel.dispatch(SessionEvent.ConfirmSelection) },
                        )

                    QuestionActivity.Finalizing ->
                        FinalizingScreen(
                            questionNumber = current.questionNumber,
                            cardIds = ui.draftPicks,
                            onConfirm = { viewModel.dispatch(SessionEvent.ConfirmFinal) },
                            onChangeSelection = { viewModel.dispatch(SessionEvent.BeginSelection) },
                        )

                    QuestionActivity.Discussing ->
                        DiscussingScreen(
                            questionNumber = current.questionNumber,
                            participantName = participantName,
                            cardIds = ui.draftPicks,
                            onDone = { viewModel.dispatch(SessionEvent.EndDiscussion) },
                        )
                }
            }

            SessionState.Summary ->
                SummaryScreen(
                    participants = summaries,
                    onShare = { viewModel.shareSummary(it) },
                    onAddContact = { index ->
                        val name = ui.participantNames.getOrElse(index) { "" }
                        viewModel.dispatch(
                            SessionEvent.CollectContact(index, org.cru.soularium.domain.ContactInfo(name)),
                        )
                    },
                    onDone = { viewModel.dispatch(SessionEvent.Conclude) },
                )

            is SessionState.CollectingContact ->
                ContactCollectionScreen(
                    participantName = ui.participantNames.getOrElse(current.participantIndex) { "" },
                    onSave = {
                        viewModel.dispatch(SessionEvent.CollectContact(current.participantIndex, it))
                    },
                    onSkip = { viewModel.dispatch(SessionEvent.SkipContact) },
                )

            SessionState.Concluded -> {
                LaunchedEffect(Unit) { onExit() }
                ConversationStub("Concluded")
            }
        }
    }
}

/**
 * Mirrors the count rules enforced by the transition function: round 1 of a
 * two-round question needs a wide set, every other round needs exactly the
 * required count.
 */
private fun isSelectionValid(
    question: Question,
    activity: QuestionActivity,
    count: Int,
): Boolean =
    when (activity) {
        QuestionActivity.SelectingRound1 ->
            if (question.selectionRounds == 2) {
                count >= question.requiredImageCount + 1
            } else {
                count == question.requiredImageCount
            }
        QuestionActivity.SelectingRound2 -> count == question.requiredImageCount
        else -> false
    }

@Composable
private fun ConversationStub(label: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "TODO: $label", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
