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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.content.Question
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.domain.session.QuestionActivity
import org.cru.soularium.domain.session.SessionEvent
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_cancel
import org.cru.soularium.generated.resources.conversation_exit_bookmark
import org.cru.soularium.generated.resources.conversation_exit_discard
import org.cru.soularium.generated.resources.conversation_exit_message
import org.cru.soularium.generated.resources.conversation_exit_title
import org.cru.soularium.platform.PlatformBackHandler
import org.cru.soularium.ui.nav.ConversationScreen
import org.jetbrains.compose.resources.stringResource

private const val TOTAL_QUESTIONS = 5

/**
 * The conversation Layout composable: renders one of several subscreens based
 * on the current [SessionState], plus the bookmark/discard exit dialog.
 *
 * Subscreens are stateless callback-driven composables; every callback is
 * routed through [ConversationPresenter.UiEvent] via [state.eventSink].
 */
@CircuitInject(ConversationScreen::class, AppScope::class)
@Composable
fun ConversationLayout(
    state: ConversationPresenter.UiState,
    modifier: Modifier = Modifier,
) {
    // Intercept the platform back affordance so leaving mid-conversation is a
    // deliberate choice between bookmarking and discarding progress.
    PlatformBackHandler(enabled = state.sessionState != SessionState.Concluded) {
        state.eventSink(ConversationPresenter.UiEvent.RequestExit)
    }

    if (state.showExitDialog) {
        ExitConversationDialog(
            onBookmark = { state.eventSink(ConversationPresenter.UiEvent.BookmarkAndExit) },
            onDiscard = { state.eventSink(ConversationPresenter.UiEvent.DiscardAndExit) },
            onCancel = { state.eventSink(ConversationPresenter.UiEvent.DismissExitDialog) },
        )
    }

    AnimatedContent(
        targetState = state.sessionState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier,
    ) { current ->
        val dispatch: (SessionEvent) -> Unit = { event ->
            state.eventSink(ConversationPresenter.UiEvent.Dispatch(event))
        }
        when (current) {
            SessionState.NotStarted -> ConversationLoading()

            SessionState.AddingParticipants ->
                AddParticipantsLayout(
                    participantNames = state.ui.participantNames,
                    onAddParticipant = { dispatch(SessionEvent.AddParticipant(it)) },
                    onRemoveParticipant = { dispatch(SessionEvent.RemoveParticipant(it)) },
                    onConfirm = { dispatch(SessionEvent.ConfirmParticipants) },
                )

            is SessionState.InQuestion -> {
                val question = Questions.byNumber(current.questionNumber)
                val participantName =
                    state.ui.participantNames.getOrElse(current.activeParticipantIndex) { "" }
                when (current.activity) {
                    QuestionActivity.ShowingPrompt ->
                        QuestionPromptLayout(
                            questionNumber = current.questionNumber,
                            totalQuestions = TOTAL_QUESTIONS,
                            participantName = participantName,
                            isGroup = state.ui.participantNames.size > 1,
                            onBegin = { dispatch(SessionEvent.BeginSelection) },
                        )

                    QuestionActivity.ShowingInstructions ->
                        InstructionPanelLayout(
                            onDismiss = { dispatch(SessionEvent.DismissInstructions) },
                        )

                    QuestionActivity.SelectingRound1,
                    QuestionActivity.SelectingRound2,
                    ->
                        SelectionLayout(
                            questionNumber = current.questionNumber,
                            round = if (current.activity == QuestionActivity.SelectingRound2) 2 else 1,
                            selectedCardIds = state.ui.draftPicks,
                            isConfirmEnabled = isSelectionValid(question, current.activity, state.ui.draftPicks.size),
                            onToggleCard = { cardId ->
                                if (cardId in state.ui.draftPicks) {
                                    dispatch(SessionEvent.UnpickCard(cardId))
                                } else {
                                    dispatch(SessionEvent.PickCard(cardId))
                                }
                            },
                            onConfirm = { dispatch(SessionEvent.ConfirmSelection) },
                        )

                    QuestionActivity.Finalizing ->
                        FinalizingLayout(
                            questionNumber = current.questionNumber,
                            cardIds = state.ui.draftPicks,
                            onConfirm = { dispatch(SessionEvent.ConfirmFinal) },
                            onChangeSelection = { dispatch(SessionEvent.BeginSelection) },
                        )

                    QuestionActivity.Discussing ->
                        DiscussingLayout(
                            questionNumber = current.questionNumber,
                            participantName = participantName,
                            cardIds = state.ui.draftPicks,
                            onDone = { dispatch(SessionEvent.EndDiscussion) },
                        )
                }
            }

            SessionState.Summary ->
                SummaryLayout(
                    participants = state.summaries,
                    onShare = { state.eventSink(ConversationPresenter.UiEvent.Share(it)) },
                    onAddContact = { index ->
                        val name = state.ui.participantNames.getOrElse(index) { "" }
                        state.eventSink(
                            ConversationPresenter.UiEvent.CollectContact(index, ContactInfo(name)),
                        )
                    },
                    onDone = { dispatch(SessionEvent.Conclude) },
                )

            is SessionState.CollectingContact ->
                ContactCollectionLayout(
                    participantName = state.ui.participantNames.getOrElse(current.participantIndex) { "" },
                    onSave = {
                        state.eventSink(
                            ConversationPresenter.UiEvent.CollectContact(current.participantIndex, it),
                        )
                    },
                    onSkip = { state.eventSink(ConversationPresenter.UiEvent.SkipContact) },
                )

            SessionState.Concluded -> ConversationLoading()
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
): Boolean = when (activity) {
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
