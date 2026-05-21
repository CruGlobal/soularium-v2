package org.cru.soularium.ui.conversation

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cru.soularium.ui.content.CardImages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import soularium.composeapp.generated.resources.Res
import soularium.composeapp.generated.resources.action_done
import soularium.composeapp.generated.resources.cd_card_thumb
import soularium.composeapp.generated.resources.discuss_instructions
import soularium.composeapp.generated.resources.image_x_of_y
import soularium.composeapp.generated.resources.q1_discussion
import soularium.composeapp.generated.resources.q2_discussion
import soularium.composeapp.generated.resources.q3_discussion
import soularium.composeapp.generated.resources.q4_discussion
import soularium.composeapp.generated.resources.q5_discussion

/** Card artwork is 3:2 landscape (2208x1468). */
private const val CARD_ASPECT_RATIO = 3f / 2f

/**
 * Subscreen displayed while the group discusses a participant's finalized picks.
 *
 * Shows the active participant's name, the per-question discussion prompt, the
 * "DISCUSS" label, and either a single full-bleed card image (1 pick) or a
 * [HorizontalPager] with an "Image N of M" indicator (3 picks).  The "Done"
 * button fires [onDone].
 *
 * This is a stateless composable — trivial pager state is held locally.
 * No ViewModel, no Koin, no navigation logic.
 *
 * @param questionNumber   1-based question index (1..5); controls which
 *                         discussion prompt is shown.
 * @param participantName  name of the active participant.
 * @param cardIds          finalized card ids (1 or 3 values, each 1..50).
 * @param onDone           called when the user taps "Done".
 */
@Composable
fun DiscussingScreen(
    questionNumber: Int,
    participantName: String,
    cardIds: List<Int>,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val discussLabel = stringResource(Res.string.discuss_instructions)
    val doneLabel = stringResource(Res.string.action_done)
    val discussionPrompt = stringResource(questionDiscussionResource(questionNumber))

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
                    .padding(top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // "DISCUSS" label
                Text(
                    text = discussLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                // Participant name
                Text(
                    text = participantName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Discussion prompt text
                Text(
                    text = discussionPrompt,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Card image(s)
                if (cardIds.size == 1) {
                    SingleCardImage(
                        cardId = cardIds[0],
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    MultiCardPager(
                        cardIds = cardIds,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // "Done" primary action
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = doneLabel },
            ) {
                Text(
                    text = doneLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SingleCardImage(
    cardId: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(CardImages.full(cardId)),
        contentDescription = stringResource(Res.string.cd_card_thumb, cardId),
        contentScale = ContentScale.Fit,
        modifier = modifier.aspectRatio(CARD_ASPECT_RATIO),
    )
}

@Composable
private fun MultiCardPager(
    cardIds: List<Int>,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { cardIds.size })
    val currentPage = pagerState.currentPage
    val totalPages = cardIds.size

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "Image N of M" page indicator
        Text(
            text = stringResource(Res.string.image_x_of_y, currentPage + 1, totalPages),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val cardDesc = stringResource(Res.string.image_x_of_y, page + 1, totalPages)
                Image(
                    painter = painterResource(CardImages.full(cardIds[page])),
                    contentDescription = cardDesc,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(CARD_ASPECT_RATIO),
                )
            }
        }
    }
}

/** Returns the [StringResource] for the discussion prompt of the given 1-based [questionNumber]. */
private fun questionDiscussionResource(questionNumber: Int): StringResource =
    when (questionNumber) {
        1 -> Res.string.q1_discussion
        2 -> Res.string.q2_discussion
        3 -> Res.string.q3_discussion
        4 -> Res.string.q4_discussion
        else -> Res.string.q5_discussion
    }
