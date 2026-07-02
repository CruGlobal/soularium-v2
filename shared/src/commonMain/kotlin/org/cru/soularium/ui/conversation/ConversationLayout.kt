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

/**
 * Renders the active sub-Layout based on [ConversationPresenter.UiState.subState],
 * plus the bookmark/discard exit dialog and the platform back-handler that
 * gates leaving mid-conversation.
 *
 * This is a pure dispatch table. All state derivation and callback wiring
 * happens in [ConversationPresenter] — sub-Layouts receive a ready-to-render
 * UiState of their own and emit intent through callbacks already wired to
 * the Presenter's event sink.
 */
@CircuitInject(ConversationScreen::class, AppScope::class)
@Composable
fun ConversationLayout(
    state: ConversationPresenter.UiState,
    modifier: Modifier = Modifier,
) {
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
        targetState = state.subState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier,
        contentKey = { it::class },
    ) { current ->
        when (current) {
            ConversationPresenter.SubState.Loading -> ConversationLoading()
            is ConversationPresenter.SubState.AddingParticipants -> AddParticipantsLayout(current.state)
            is ConversationPresenter.SubState.QuestionPrompt -> QuestionPromptLayout(current.state)
            is ConversationPresenter.SubState.Instructions -> InstructionPanelLayout(current.state)
            is ConversationPresenter.SubState.Selecting -> SelectionLayout(current.state)
            is ConversationPresenter.SubState.Finalizing -> FinalizingLayout(current.state)
            is ConversationPresenter.SubState.Discussing -> DiscussingLayout(current.state)
            is ConversationPresenter.SubState.Summary -> SummaryLayout(current.state)
            is ConversationPresenter.SubState.CollectingContact -> ContactCollectionLayout(current.state)
        }
    }
}

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
