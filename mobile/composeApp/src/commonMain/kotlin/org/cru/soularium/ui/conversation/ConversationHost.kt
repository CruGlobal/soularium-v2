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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.session.QuestionActivity
import org.cru.soularium.domain.session.SessionState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Single navigation destination that hosts the entire 5-question conversation
 * flow. It owns the [ConversationViewModel] and renders a subscreen derived
 * from [SessionState]; subscreens are never pushed onto the back stack.
 *
 * Each `when` branch is replaced with a real subscreen composable as the
 * Phase 7 screen tasks land.
 */
@Composable
fun ConversationHost(
    sessionId: SessionId,
    onExit: () -> Unit,
    viewModel: ConversationViewModel = koinViewModel { parametersOf(sessionId) },
) {
    val state by viewModel.state.collectAsState()

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { current ->
        when (current) {
            SessionState.NotStarted -> ConversationStub("Starting…")
            SessionState.AddingParticipants -> ConversationStub("Add Participants")
            is SessionState.InQuestion ->
                when (current.activity) {
                    QuestionActivity.ShowingPrompt -> ConversationStub("Question ${current.questionNumber} — Prompt")
                    QuestionActivity.ShowingInstructions -> ConversationStub("Instructions")
                    QuestionActivity.SelectingRound1,
                    QuestionActivity.SelectingRound2,
                    -> ConversationStub("Question ${current.questionNumber} — Selection")
                    QuestionActivity.Finalizing -> ConversationStub("Question ${current.questionNumber} — Finalizing")
                    QuestionActivity.Discussing -> ConversationStub("Question ${current.questionNumber} — Discussing")
                }
            SessionState.Summary -> ConversationStub("Summary")
            is SessionState.CollectingContact -> ConversationStub("Collecting Contact")
            SessionState.Concluded -> ConversationStub("Concluded")
        }
    }
}

@Composable
private fun ConversationStub(label: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "TODO: $label", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
