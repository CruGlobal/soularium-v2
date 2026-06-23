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
 * Subscreen shown when a participant is about to answer a question.
 *
 * Displays the "Question N of [totalQuestions]" label, the question prompt, and
 * — prominently when [isGroup] is true — the active participant's name formatted
 * as "Alright, [name]. Your turn."  A single primary button lets the user begin
 * image selection.
 *
 * This is a stateless composable. No ViewModel, no navigation logic.
 *
 * @param questionNumber     1-based index of the current question (1..5).
 * @param totalQuestions     total number of questions in this session (usually 5).
 * @param participantName    name of the participant whose turn it is.
 * @param isGroup            true when there are multiple participants; the name
 *                           greeting is shown only in group sessions.
 * @param onBegin            called when the user taps the "Ready" button.
 */
@Composable
fun QuestionPromptScreen(
    questionNumber: Int,
    totalQuestions: Int,
    participantName: String,
    isGroup: Boolean,
    onBegin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = QuestionProgressColors.getOrElse(questionNumber - 1) {
        QuestionProgressColors.first()
    }

    val questionLabel = stringResource(Res.string.question_index, questionNumber, totalQuestions)
    val promptText = stringResource(questionPromptResource(questionNumber))
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
                // "Question N of 5" label
                Text(
                    text = questionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Group greeting — "Alright, Name.\nYour turn."
                if (isGroup) {
                    Text(
                        text = stringResource(Res.string.your_turn, participantName),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Question prompt text
                Text(
                    text = promptText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Primary action button
            Button(
                onClick = onBegin,
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

/** Returns the [StringResource] for the prompt of the given 1-based [questionNumber]. */
private fun questionPromptResource(questionNumber: Int): StringResource = when (questionNumber) {
    1 -> Res.string.q1_prompt
    2 -> Res.string.q2_prompt
    3 -> Res.string.q3_prompt
    4 -> Res.string.q4_prompt
    else -> Res.string.q5_prompt
}
