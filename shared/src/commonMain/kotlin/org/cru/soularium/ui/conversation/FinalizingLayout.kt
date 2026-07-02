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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_change_selection
import org.cru.soularium.generated.resources.action_confirm
import org.cru.soularium.generated.resources.cd_card_thumb
import org.cru.soularium.generated.resources.finalizing_review_hint
import org.cru.soularium.generated.resources.finalizing_title
import org.cru.soularium.generated.resources.q1_finalizing
import org.cru.soularium.generated.resources.q2_finalizing
import org.cru.soularium.generated.resources.q3_finalizing
import org.cru.soularium.generated.resources.q4_finalizing
import org.cru.soularium.generated.resources.q5_finalizing
import org.cru.soularium.ui.content.CardImages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * UI state for the Finalizing sub-layout — confirmation screen showing the
 * participant's chosen cards before discussion begins.
 */
data class FinalizingUiState(
    val questionNumber: Int,
    val cardIds: List<Int>,
    val onConfirm: () -> Unit,
    val onChangeSelection: () -> Unit,
)

@Composable
fun FinalizingLayout(
    state: FinalizingUiState,
    modifier: Modifier = Modifier,
) {
    val confirmLabel = stringResource(Res.string.action_confirm)
    val changeSelectionLabel = stringResource(Res.string.action_change_selection)

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
                    text = stringResource(Res.string.finalizing_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(questionFinalizingResource(state.questionNumber)),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.finalizing_review_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (state.cardIds.size == 1) {
                    val cardId = state.cardIds.first()
                    val cardDesc = stringResource(Res.string.cd_card_thumb, cardId)
                    Image(
                        painter = painterResource(CardImages.full(cardId)),
                        contentDescription = cardDesc,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 2f),
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.cardIds.forEach { cardId ->
                            val cardDesc = stringResource(Res.string.cd_card_thumb, cardId)
                            Image(
                                painter = painterResource(CardImages.full(cardId)),
                                contentDescription = cardDesc,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(3f / 2f),
                            )
                        }
                    }
                }
            }

            Button(
                onClick = state.onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = confirmLabel },
            ) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = state.onChangeSelection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = changeSelectionLabel },
            ) {
                Text(
                    text = changeSelectionLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun questionFinalizingResource(questionNumber: Int): StringResource = when (questionNumber) {
    1 -> Res.string.q1_finalizing
    2 -> Res.string.q2_finalizing
    3 -> Res.string.q3_finalizing
    4 -> Res.string.q4_finalizing
    else -> Res.string.q5_finalizing
}
