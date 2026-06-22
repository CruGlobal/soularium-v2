package org.cru.soularium.ui.conversation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_confirm
import org.cru.soularium.generated.resources.cd_card_thumb
import org.cru.soularium.generated.resources.q1_selection
import org.cru.soularium.generated.resources.q2_selection
import org.cru.soularium.generated.resources.q3_selection
import org.cru.soularium.generated.resources.q4_selection
import org.cru.soularium.generated.resources.q5_selection
import org.cru.soularium.generated.resources.selection_choose_1
import org.cru.soularium.generated.resources.selection_choose_3
import org.cru.soularium.generated.resources.selection_choose_wide
import org.cru.soularium.generated.resources.selection_finish_picks
import org.cru.soularium.generated.resources.selection_navigation_instructions
import org.cru.soularium.generated.resources.selection_round_1_label
import org.cru.soularium.generated.resources.selection_round_2_label
import org.cru.soularium.generated.resources.selection_x_selected
import org.cru.soularium.ui.content.CardImages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val CARD_GRID_COLUMNS = 3
private const val TOTAL_CARDS = 50
private const val TWO_ROUND_QUESTION_MAX = 2

/**
 * Core image-selection screen for a single selection round.
 *
 * This composable is fully stateless — [selectedCardIds] drives displayed
 * selection state and [onToggleCard] reports a tap. The caller owns the
 * selection logic and validity rules.
 *
 * @param questionNumber     1-based question index (1..5).
 * @param selectedCardIds    IDs of cards currently marked as selected.
 * @param isConfirmEnabled   whether the Confirm button should be enabled;
 *                           the caller derives this from the count rules.
 * @param onToggleCard       called with the card id (1..50) when a card is tapped.
 * @param onConfirm          called when the user taps the enabled Confirm button.
 * @param modifier           optional [Modifier] applied to the root [Surface].
 */
@Composable
fun SelectionScreen(
    questionNumber: Int,
    round: Int,
    selectedCardIds: List<Int>,
    isConfirmEnabled: Boolean,
    onToggleCard: (Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTwoRoundQuestion = questionNumber <= TWO_ROUND_QUESTION_MAX
    val selectionPrompt = stringResource(questionSelectionRes(questionNumber))
    // Round 1 of a two-round question is a *wide* pick (>= requiredImageCount + 1);
    // round 2 narrows to exactly the required count. One-round questions pick one.
    val chooseLabel = stringResource(
        when {
            !isTwoRoundQuestion -> Res.string.selection_choose_1
            round >= 2 -> Res.string.selection_choose_3
            else -> Res.string.selection_choose_wide
        },
    )
    val selectedCountLabel = stringResource(Res.string.selection_x_selected, selectedCardIds.size)
    val confirmLabel = stringResource(Res.string.action_confirm)
    val finishPicksLabel = stringResource(Res.string.selection_finish_picks)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header section
            SelectionHeader(
                isTwoRoundQuestion = isTwoRoundQuestion,
                round = round,
                selectionPrompt = selectionPrompt,
                chooseLabel = chooseLabel,
                selectedCountLabel = selectedCountLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
            )

            // Card grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(CARD_GRID_COLUMNS),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(TOTAL_CARDS) { index ->
                    val cardId = index + 1
                    val isSelected = cardId in selectedCardIds
                    SelectableCardItem(
                        cardId = cardId,
                        isSelected = isSelected,
                        onToggle = { onToggleCard(cardId) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hint text when confirm is not yet available
            if (!isConfirmEnabled) {
                Text(
                    text = finishPicksLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Confirm button
            Button(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 24.dp)
                    .semantics { contentDescription = confirmLabel },
            ) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SelectionHeader(
    isTwoRoundQuestion: Boolean,
    round: Int,
    selectionPrompt: String,
    chooseLabel: String,
    selectedCountLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isTwoRoundQuestion) {
            Text(
                text = stringResource(
                    if (round >= 2) {
                        Res.string.selection_round_2_label
                    } else {
                        Res.string.selection_round_1_label
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = selectionPrompt,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = chooseLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = selectedCountLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(Res.string.selection_navigation_instructions),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectableCardItem(
    cardId: Int,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardLabel = stringResource(Res.string.cd_card_thumb, cardId)

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val cardShape = MaterialTheme.shapes.extraSmall

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cardShape)
            .then(
                if (isSelected) {
                    Modifier.border(
                        border = BorderStroke(3.dp, primaryColor),
                        shape = cardShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onToggle)
            .semantics {
                contentDescription = cardLabel
                selected = isSelected
                role = Role.Checkbox
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        Image(
            painter = painterResource(CardImages.thumb(cardId)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color = primaryColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = onPrimaryColor,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

private fun questionSelectionRes(questionNumber: Int): StringResource =
    when (questionNumber) {
        1 -> Res.string.q1_selection
        2 -> Res.string.q2_selection
        3 -> Res.string.q3_selection
        4 -> Res.string.q4_selection
        else -> Res.string.q5_selection
    }
