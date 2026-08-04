package org.cru.soularium.ui.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.OverlayEffect
import dev.zacsweers.metro.AppScope
import org.cru.soularium.platform.PlatformBackHandler
import org.cru.soularium.ui.nav.ConversationScreen

/**
 * Renders the page indicated by the presenter's [ConversationPresenter.UiState],
 * plus the bookmark/discard [ExitConversationOverlay]. The Layout owns no business logic:
 * every variant of the sealed [ConversationPresenter.UiState] arrives with its
 * props already resolved, and each subscreen handles its own page-specific
 * events through the shared [ConversationPresenter.UiEvent] hierarchy.
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
        OverlayEffect {
            state.eventSink(
                when (show(ExitConversationOverlay())) {
                    ExitConversationOverlay.Result.Bookmark -> ConversationPresenter.UiEvent.BookmarkAndExit
                    ExitConversationOverlay.Result.Discard -> ConversationPresenter.UiEvent.DiscardAndExit
                    ExitConversationOverlay.Result.Cancelled -> ConversationPresenter.UiEvent.DismissExitDialog
                },
            )
        }
    }

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it::class },
        modifier = modifier,
    ) { current ->
        when (current) {
            is ConversationPresenter.UiState.Loading -> ConversationLoading()
            is ConversationPresenter.UiState.AddingParticipants -> AddParticipantsLayout(current)
            is ConversationPresenter.UiState.QuestionPrompt -> QuestionPromptLayout(current)
            is ConversationPresenter.UiState.Instructions -> InstructionPanelLayout(current)
            is ConversationPresenter.UiState.Selection -> SelectionLayout(current)
            is ConversationPresenter.UiState.Finalizing -> FinalizingLayout(current)
            is ConversationPresenter.UiState.Discussing -> DiscussingLayout(current)
            is ConversationPresenter.UiState.Summary -> SummaryLayout(current)
            is ConversationPresenter.UiState.CollectingContact -> ContactCollectionLayout(current)
        }
    }
}

@Composable
private fun ConversationLoading() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
