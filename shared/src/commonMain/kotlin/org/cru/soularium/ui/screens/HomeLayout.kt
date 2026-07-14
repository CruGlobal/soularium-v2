package org.cru.soularium.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.app_tagline
import org.cru.soularium.generated.resources.cd_menu_button
import org.cru.soularium.generated.resources.cta_create_my_soularium
import org.cru.soularium.generated.resources.cta_start_conversation
import org.cru.soularium.generated.resources.home_hero_subtitle
import org.cru.soularium.generated.resources.home_hero_title
import org.cru.soularium.generated.resources.home_my_soularium_caption
import org.cru.soularium.generated.resources.menu_about
import org.cru.soularium.generated.resources.menu_cards_and_questions
import org.cru.soularium.generated.resources.menu_close
import org.cru.soularium.generated.resources.menu_my_soularium
import org.cru.soularium.generated.resources.menu_past_conversations
import org.cru.soularium.generated.resources.menu_resources
import org.cru.soularium.generated.resources.menu_settings
import org.cru.soularium.ui.nav.HomeScreen
import org.jetbrains.compose.resources.stringResource

/**
 * Home screen — entry point after onboarding/terms.
 *
 * Displays the branded MySoularium hero, a primary "Start a Conversation" CTA,
 * a secondary "Create MySoularium" CTA, and a menu trigger icon in the top-right
 * corner. Tapping the icon opens [MenuBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun HomeLayout(state: HomePresenter.UiState, modifier: Modifier = Modifier) {
    var showMenu by remember { mutableStateOf(false) }
    val menuButtonLabel = stringResource(Res.string.cd_menu_button)
    val startLabel = stringResource(Res.string.cta_start_conversation)
    val createLabel = stringResource(Res.string.cta_create_my_soularium)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier =
                    Modifier
                        .size(48.dp)
                        .semantics { contentDescription = menuButtonLabel },
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
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
                        .height(52.dp)
                        .semantics { contentDescription = startLabel },
                ) {
                    Text(
                        text = startLabel,
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
                        .height(52.dp)
                        .semantics { contentDescription = createLabel },
                ) {
                    Text(
                        text = createLabel,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    if (showMenu) {
        MenuBottomSheet(
            onDismiss = { showMenu = false },
            onMySoularium = {
                showMenu = false
                state.eventSink(HomePresenter.UiEvent.StartSoloConversation)
            },
            onPastConversations = {
                showMenu = false
                state.eventSink(HomePresenter.UiEvent.OpenPastConversations)
            },
            onAbout = {
                showMenu = false
                state.eventSink(HomePresenter.UiEvent.OpenAbout)
            },
            onResources = {
                showMenu = false
                state.eventSink(HomePresenter.UiEvent.OpenResources)
            },
            onCardsAndQuestions = {
                showMenu = false
                state.eventSink(HomePresenter.UiEvent.OpenCardsAndQuestions)
            },
            onSettings = {
                showMenu = false
                state.eventSink(HomePresenter.UiEvent.OpenSettings)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuBottomSheet(
    onDismiss: () -> Unit,
    onMySoularium: () -> Unit,
    onPastConversations: () -> Unit,
    onAbout: () -> Unit,
    onResources: () -> Unit,
    onCardsAndQuestions: () -> Unit,
    onSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        MenuBottomSheetContent(
            onMySoularium = onMySoularium,
            onPastConversations = onPastConversations,
            onAbout = onAbout,
            onResources = onResources,
            onCardsAndQuestions = onCardsAndQuestions,
            onSettings = onSettings,
            onClose = onDismiss,
        )
    }
}

@Composable
internal fun MenuBottomSheetContent(
    onMySoularium: () -> Unit,
    onPastConversations: () -> Unit,
    onAbout: () -> Unit,
    onResources: () -> Unit,
    onCardsAndQuestions: () -> Unit,
    onSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        MenuRow(
            icon = Icons.Default.AccountCircle,
            label = stringResource(Res.string.menu_my_soularium),
            onClick = onMySoularium,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MenuRow(
            icon = Icons.Default.History,
            label = stringResource(Res.string.menu_past_conversations),
            onClick = onPastConversations,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MenuRow(
            icon = Icons.Default.Info,
            label = stringResource(Res.string.menu_about),
            onClick = onAbout,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MenuRow(
            icon = Icons.Default.ChatBubble,
            label = stringResource(Res.string.menu_resources),
            onClick = onResources,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MenuRow(
            icon = Icons.Default.GridView,
            label = stringResource(Res.string.menu_cards_and_questions),
            onClick = onCardsAndQuestions,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MenuRow(
            icon = Icons.Default.Settings,
            label = stringResource(Res.string.menu_settings),
            onClick = onSettings,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MenuRow(
            icon = Icons.Default.Close,
            label = stringResource(Res.string.menu_close),
            onClick = onClose,
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier =
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
