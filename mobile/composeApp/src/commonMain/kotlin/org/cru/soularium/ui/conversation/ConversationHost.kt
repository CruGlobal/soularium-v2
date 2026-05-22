package org.cru.soularium.ui.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.domain.content.Question
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.domain.session.QuestionActivity
import org.cru.soularium.domain.session.SessionEvent
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.platform.PlatformBackHandler
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import soularium.composeapp.generated.resources.Res
import soularium.composeapp.generated.resources.action_cancel
import soularium.composeapp.generated.resources.conversation_exit_bookmark
import soularium.composeapp.generated.resources.conversation_exit_discard
import soularium.composeapp.generated.resources.conversation_exit_message
import soularium.composeapp.generated.resources.conversation_exit_title

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
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) { viewModel.ensureStarted(kind) }

    // Intercept the platform back affordance so leaving mid-conversation is a
    // deliberate choice between bookmarking and discarding progress.
    PlatformBackHandler(enabled = state != SessionState.Concluded) {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitConversationDialog(
            onBookmark = {
                showExitDialog = false
                viewModel.bookmarkAndExit(onExit)
            },
            onDiscard = {
                showExitDialog = false
                viewModel.discardAndExit(onExit)
            },
            onCancel = { showExitDialog = false },
        )
    }

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { current ->
        when (current) {
            SessionState.NotStarted -> ConversationLoading()

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
                ConversationLoading()
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

/**
 * Confirmation shown when the user backs out of an in-progress conversation:
 * bookmark it for later, discard the progress, or stay.
 */
@Composable
private fun ExitConversationDialog(
    onBookmark: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.conversation_exit_title)) },
        text = { Text(stringResource(Res.string.conversation_exit_message)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onBookmark) {
                    Text(stringResource(Res.string.conversation_exit_bookmark))
                }
                TextButton(onClick = onDiscard) {
                    Text(stringResource(Res.string.conversation_exit_discard))
                }
                TextButton(onClick = onCancel) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        },
    )
}

@Composable
private fun ConversationLoading() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
