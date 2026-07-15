package org.cru.soularium.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.OverlayEffect
import dev.zacsweers.metro.AppScope
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.app_tagline
import org.cru.soularium.generated.resources.cd_menu_button
import org.cru.soularium.generated.resources.cta_create_my_soularium
import org.cru.soularium.generated.resources.cta_start_conversation
import org.cru.soularium.generated.resources.home_hero_subtitle
import org.cru.soularium.generated.resources.home_hero_title
import org.cru.soularium.generated.resources.home_my_soularium_caption
import org.jetbrains.compose.resources.stringResource

/**
 * Home screen — entry point after onboarding/terms.
 *
 * Displays the branded MySoularium hero, a primary "Start a Conversation" CTA,
 * a secondary "Create MySoularium" CTA, and a menu trigger icon in the top-right
 * corner. Tapping the icon opens the [HomeMenuOverlay] bottom sheet.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(HomeScreen::class, AppScope::class)
fun HomeLayout(state: HomePresenter.UiState, modifier: Modifier = Modifier) {
    var showMenu by state.showMenu

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = { showMenu = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(Res.string.cd_menu_button),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                val heroTitle = stringResource(Res.string.home_hero_title)
                val heroSubtitle = stringResource(Res.string.home_hero_subtitle)
                val wordmark =
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(heroTitle)
                        }
                        append("\n")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                            append(heroSubtitle)
                        }
                    }

                Text(
                    text = wordmark,
                    style =
                    MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.app_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { state.eventSink(HomePresenter.UiEvent.StartGroupConversation) },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.cta_start_conversation),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.home_my_soularium_caption),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { state.eventSink(HomePresenter.UiEvent.StartSoloConversation) },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.cta_create_my_soularium),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    if (showMenu) {
        OverlayEffect(Unit) {
            when (show(HomeMenuOverlay())) {
                HomeMenuOverlay.Result.MySoularium -> state.eventSink(HomePresenter.UiEvent.StartSoloConversation)
                HomeMenuOverlay.Result.PastConversations -> state.eventSink(HomePresenter.UiEvent.OpenPastConversations)
                HomeMenuOverlay.Result.About -> state.eventSink(HomePresenter.UiEvent.OpenAbout)
                HomeMenuOverlay.Result.Resources -> state.eventSink(HomePresenter.UiEvent.OpenResources)
                HomeMenuOverlay.Result.CardsAndQuestions -> state.eventSink(HomePresenter.UiEvent.OpenCardsAndQuestions)
                HomeMenuOverlay.Result.Settings -> state.eventSink(HomePresenter.UiEvent.OpenSettings)
                HomeMenuOverlay.Result.Dismissed -> Unit
            }
            showMenu = false
        }
    }
}
