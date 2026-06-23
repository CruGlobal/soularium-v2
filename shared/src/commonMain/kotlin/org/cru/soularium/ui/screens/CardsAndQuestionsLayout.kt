package org.cru.soularium.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.cru.soularium.domain.content.Question
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.cards_and_questions_title
import org.cru.soularium.generated.resources.cards_card_number
import org.cru.soularium.generated.resources.cards_question_number
import org.cru.soularium.generated.resources.cards_tab_cards
import org.cru.soularium.generated.resources.cards_tab_questions
import org.cru.soularium.generated.resources.q1_prompt
import org.cru.soularium.generated.resources.q2_prompt
import org.cru.soularium.generated.resources.q3_prompt
import org.cru.soularium.generated.resources.q4_prompt
import org.cru.soularium.generated.resources.q5_prompt
import org.cru.soularium.ui.content.CardImages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val CARD_GRID_COLUMNS = 3
private const val TOTAL_CARDS = 50
private const val TAB_IMAGES = 0
private const val TAB_QUESTIONS = 1

/**
 * Reference screen showing all 50 Soularium card images in a grid and the 5
 * questions in a scrollable list. Tapping a card opens a full-screen viewer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsAndQuestionsLayout(
    state: CardsAndQuestionsPresenter.UiState,
    modifier: Modifier = Modifier,
) {
    val backLabel = stringResource(Res.string.action_back)
    var selectedTab by remember { mutableIntStateOf(TAB_IMAGES) }
    var viewerCardId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.cards_and_questions_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { state.eventSink(CardsAndQuestionsPresenter.UiEvent.Back) },
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backLabel,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == TAB_IMAGES,
                    onClick = { selectedTab = TAB_IMAGES },
                    text = {
                        Text(
                            text = stringResource(Res.string.cards_tab_cards),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
                Tab(
                    selected = selectedTab == TAB_QUESTIONS,
                    onClick = { selectedTab = TAB_QUESTIONS },
                    text = {
                        Text(
                            text = stringResource(Res.string.cards_tab_questions),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }

            when (selectedTab) {
                TAB_IMAGES ->
                    CardsGrid(
                        onCardTap = { cardId -> viewerCardId = cardId },
                        modifier = Modifier.fillMaxSize(),
                    )
                TAB_QUESTIONS ->
                    QuestionsList(
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }

    viewerCardId?.let { cardId ->
        CardFullScreenViewer(
            cardId = cardId,
            onDismiss = { viewerCardId = null },
        )
    }
}

@Composable
private fun CardsGrid(
    onCardTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(CARD_GRID_COLUMNS),
        modifier = modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(TOTAL_CARDS) { index ->
            val cardId = index + 1
            CardThumbnailItem(
                cardId = cardId,
                onTap = { onCardTap(cardId) },
            )
        }
    }
}

@Composable
private fun CardThumbnailItem(
    cardId: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDesc = stringResource(Res.string.cards_card_number, cardId)
    Image(
        painter = painterResource(CardImages.thumb(cardId)),
        contentDescription = contentDesc,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onTap),
    )
}

@Composable
private fun QuestionsList(
    modifier: Modifier = Modifier,
) {
    val questions = remember { Questions.all }
    LazyColumn(modifier = modifier) {
        items(questions) { question ->
            QuestionItem(question = question)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun QuestionItem(
    question: Question,
    modifier: Modifier = Modifier,
) {
    val promptRes = questionPromptRes(question.number)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.cards_question_number, question.number),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(promptRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun questionPromptRes(questionNumber: Int): StringResource = when (questionNumber) {
    1 -> Res.string.q1_prompt
    2 -> Res.string.q2_prompt
    3 -> Res.string.q3_prompt
    4 -> Res.string.q4_prompt
    else -> Res.string.q5_prompt
}

@Composable
private fun CardFullScreenViewer(
    cardId: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDesc = stringResource(Res.string.cards_card_number, cardId)
    val closeLabel = stringResource(Res.string.action_back)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(CardImages.full(cardId)),
            contentDescription = contentDesc,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = closeLabel,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}
