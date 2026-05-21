package org.cru.soularium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import soularium.composeapp.generated.resources.Res
import soularium.composeapp.generated.resources.action_lets_begin
import soularium.composeapp.generated.resources.action_next
import soularium.composeapp.generated.resources.intro_page1_body
import soularium.composeapp.generated.resources.intro_page1_title
import soularium.composeapp.generated.resources.intro_page2_body
import soularium.composeapp.generated.resources.intro_page2_title
import soularium.composeapp.generated.resources.intro_ready_prompt

private const val PAGE_COUNT = 2
private const val PAGE_CONCEPT = 0
private const val PAGE_HOW_IT_WORKS = 1

/**
 * First-launch onboarding screen. A 2-page horizontally swipeable pager.
 *
 * Page 0 — concept overview (intro_page1_title / intro_page1_body).
 * Page 1 — how-it-works + ready prompt (intro_page2_title / intro_page2_body
 *           / intro_ready_prompt).
 *
 * @param onContinue called when the user advances past the second page; the
 *                   caller is responsible for navigating to Terms.
 */
@Composable
fun IntroScreen(onContinue: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) { page ->
                when (page) {
                    PAGE_CONCEPT -> IntroConcept()
                    PAGE_HOW_IT_WORKS -> IntroHowItWorks()
                }
            }

            PageIndicatorRow(
                pageCount = PAGE_COUNT,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(vertical = 16.dp),
            )

            val isLastPage = pagerState.currentPage == PAGE_HOW_IT_WORKS
            val nextLabel =
                if (isLastPage) {
                    stringResource(Res.string.action_lets_begin)
                } else {
                    stringResource(Res.string.action_next)
                }

            Button(
                onClick = {
                    if (isLastPage) {
                        onContinue()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .semantics { contentDescription = nextLabel },
            ) {
                Text(
                    text = nextLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IntroConcept(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(Res.string.intro_page1_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.intro_page1_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IntroHowItWorks(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(Res.string.intro_page2_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.intro_page2_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(Res.string.intro_ready_prompt),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PageIndicatorRow(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val color =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            val dotSize = if (isSelected) 10.dp else 8.dp
            Box(
                modifier =
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(color),
            )
        }
    }
}
