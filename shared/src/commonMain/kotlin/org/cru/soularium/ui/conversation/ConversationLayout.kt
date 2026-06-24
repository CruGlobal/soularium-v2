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
import org.cru.soularium.domain.session.SessionEvent
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_cancel
import org.cru.soularium.generated.resources.conversation_exit_bookmark
import org.cru.soularium.generated.resources.conversation_exit_discard
import org.cru.soularium.generated.resources.conversation_exit_message
import org.cru.soularium.generated.resources.conversation_exit_title
import org.cru.soularium.platform.PlatformBackHandler
import org.cru.soularium.ui.nav.ConversationScreen
import org.jetbrains.compose.resources.stringResource

/**
 * Renders the page indicated by the presenter's [ConversationPresenter.UiState],
 * plus the bookmark/discard exit dialog. The Layout owns no business logic:
 * every variant of the sealed [ConversationPresenter.UiState] arrives with its
 * props already resolved, and every callback funnels into a single
 * [ConversationPresenter.UiEvent].
 */
@CircuitInject(ConversationScreen::class, AppScope::class)
@Composable
fun ConversationLayout(state: ConversationPresenter.UiState, modifier: Modifier = Modifier) {
    // Intercept the platform back affordance so leaving mid-conversation is a
    // deliberate choice between bookmarking and discarding progress.
    PlatformBackHandler(enabled = state !is ConversationPresenter.UiState.Loading) {
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
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it::class },
        modifier = modifier,
    ) { current ->
        val dispatch: (SessionEvent) -> Unit = { event ->
            current.eventSink(ConversationPresenter.UiEvent.Dispatch(event))
        }
        when (current) {
            is ConversationPresenter.UiState.Loading -> ConversationLoading()

            is ConversationPresenter.UiState.AddingParticipants ->
                AddParticipantsLayout(
                    participantNames = current.participantNames,
                    onAddParticipant = { dispatch(SessionEvent.AddParticipant(it)) },
                    onRemoveParticipant = { dispatch(SessionEvent.RemoveParticipant(it)) },
                    onConfirm = { dispatch(SessionEvent.ConfirmParticipants) },
                )

            is ConversationPresenter.UiState.QuestionPrompt ->
                QuestionPromptLayout(
                    questionNumber = current.questionNumber,
                    totalQuestions = current.totalQuestions,
                    participantName = current.participantName,
                    isGroup = current.isGroup,
                    onBegin = { dispatch(SessionEvent.BeginSelection) },
                )

            is ConversationPresenter.UiState.Instructions ->
                InstructionPanelLayout(
                    onDismiss = { dispatch(SessionEvent.DismissInstructions) },
                )

            is ConversationPresenter.UiState.Selection ->
                SelectionLayout(
                    questionNumber = current.questionNumber,
                    round = current.round,
                    selectedCardIds = current.selectedCardIds,
                    isConfirmEnabled = current.isConfirmEnabled,
                    onToggleCard = { cardId ->
                        current.eventSink(ConversationPresenter.UiEvent.ToggleCard(cardId))
                    },
                    onConfirm = { dispatch(SessionEvent.ConfirmSelection) },
                )

            is ConversationPresenter.UiState.Finalizing ->
                FinalizingLayout(
                    questionNumber = current.questionNumber,
                    cardIds = current.cardIds,
                    onConfirm = { dispatch(SessionEvent.ConfirmFinal) },
                    onChangeSelection = { dispatch(SessionEvent.BeginSelection) },
                )

            is ConversationPresenter.UiState.Discussing ->
                DiscussingLayout(
                    questionNumber = current.questionNumber,
                    participantName = current.participantName,
                    cardIds = current.cardIds,
                    onDone = { dispatch(SessionEvent.EndDiscussion) },
                )

            is ConversationPresenter.UiState.Summary ->
                SummaryLayout(
                    participants = current.participants,
                    onShare = { current.eventSink(ConversationPresenter.UiEvent.Share(it)) },
                    onAddContact = { index ->
                        val name = current.participants.firstOrNull { it.participantIndex == index }?.name.orEmpty()
                        current.eventSink(
                            ConversationPresenter.UiEvent.CollectContact(index, ContactInfo(name)),
                        )
                    },
                    onDone = { dispatch(SessionEvent.Conclude) },
                )

            is ConversationPresenter.UiState.CollectingContact ->
                ContactCollectionLayout(
                    participantName = current.participantName,
                    onSave = {
                        current.eventSink(
                            ConversationPresenter.UiEvent.CollectContact(current.participantIndex, it),
                        )
                    },
                    onSkip = { current.eventSink(ConversationPresenter.UiEvent.SkipContact) },
                )
        }
    }
}

/**
 * Confirmation shown when the user backs out of an in-progress conversation:
 * bookmark it for later, discard the progress, or stay.
 */
@Composable
private fun ExitConversationDialog(onBookmark: () -> Unit, onDiscard: () -> Unit, onCancel: () -> Unit) {
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
