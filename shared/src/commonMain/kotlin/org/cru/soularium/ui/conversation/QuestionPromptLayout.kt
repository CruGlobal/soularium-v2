package org.cru.soularium.ui.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_ready
import org.cru.soularium.generated.resources.q1_prompt
import org.cru.soularium.generated.resources.q2_prompt
import org.cru.soularium.generated.resources.q3_prompt
import org.cru.soularium.generated.resources.q4_prompt
import org.cru.soularium.generated.resources.q5_prompt
import org.cru.soularium.generated.resources.question_index
import org.cru.soularium.generated.resources.your_turn
import org.cru.soularium.ui.theme.QuestionProgressColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * UI state for the QuestionPrompt sub-layout — shown when a participant is
 * about to answer a question. `isGroup` controls whether the personal greeting
 * is displayed; the Presenter derives it from participant count.
 */
data class QuestionPromptUiState(
    val questionNumber: Int,
    val totalQuestions: Int,
    val participantName: String,
    val isGroup: Boolean,
    val onBegin: () -> Unit,
)

@Composable
fun QuestionPromptLayout(
    state: QuestionPromptUiState,
    modifier: Modifier = Modifier,
) {
    val accentColor = QuestionProgressColors.getOrElse(state.questionNumber - 1) {
        QuestionProgressColors.first()
    }

    val questionLabel = stringResource(Res.string.question_index, state.questionNumber, state.totalQuestions)
    val promptText = stringResource(questionPromptResource(state.questionNumber))
    val readyLabel = stringResource(Res.string.action_ready)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = questionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isGroup) {
                    Text(
                        text = stringResource(Res.string.your_turn, state.participantName),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }

                Text(
                    text = promptText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = state.onBegin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = readyLabel },
            ) {
                Text(
                    text = readyLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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
