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
import org.jetbrains.compose.resources.stringResource
import soularium.composeapp.generated.resources.Res
import soularium.composeapp.generated.resources.app_tagline
import soularium.composeapp.generated.resources.cd_menu_button
import soularium.composeapp.generated.resources.cta_create_my_soularium
import soularium.composeapp.generated.resources.cta_start_conversation
import soularium.composeapp.generated.resources.home_hero_subtitle
import soularium.composeapp.generated.resources.home_hero_title
import soularium.composeapp.generated.resources.home_my_soularium_caption
import soularium.composeapp.generated.resources.menu_about
import soularium.composeapp.generated.resources.menu_cards_and_questions
import soularium.composeapp.generated.resources.menu_close
import soularium.composeapp.generated.resources.menu_my_soularium
import soularium.composeapp.generated.resources.menu_past_conversations
import soularium.composeapp.generated.resources.menu_resources
import soularium.composeapp.generated.resources.menu_settings

/**
 * Home screen — entry point after onboarding/terms.
 *
 * Displays the branded MySoularium hero, a primary "Start a Conversation" CTA,
 * a secondary "Create MySoularium" CTA, and a menu trigger icon in the top-right
 * corner. Tapping the icon opens [MenuBottomSheet].
 *
 * All navigation decisions are delegated to the provided callbacks.
 *
 * @param onStartConversation       called when the user taps "Start a Conversation".
 * @param onMySoularium             called when the user taps "Create MySoularium" or
 *                                  the MySoularium row in the menu.
 * @param onMenuPastConversations   called when the user taps Past Conversations in
 *                                  the menu.
 * @param onMenuAbout               called when the user taps About in the menu.
 * @param onMenuResources           called when the user taps Resources in the menu.
 * @param onMenuCardsAndQuestions   called when the user taps Images & Questions in
 *                                  the menu.
 * @param onMenuSettings            called when the user taps Settings in the menu.
 * @param modifier                  applied to the root [Surface].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartConversation: () -> Unit,
    onMySoularium: () -> Unit,
    onMenuPastConversations: () -> Unit,
    onMenuAbout: () -> Unit,
    onMenuResources: () -> Unit,
    onMenuCardsAndQuestions: () -> Unit,
    onMenuSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            // ── Top bar ──────────────────────────────────────────────────────
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

            // ── Hero section ──────────────────────────────────────────────────
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // "My Soularium" wordmark — two-word stacked display with primary accent
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

                // ── CTAs ──────────────────────────────────────────────────────
                Button(
                    onClick = onStartConversation,
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
                    onClick = onMySoularium,
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
                onMySoularium()
            },
            onPastConversations = {
                showMenu = false
                onMenuPastConversations()
            },
            onAbout = {
                showMenu = false
                onMenuAbout()
            },
            onResources = {
                showMenu = false
                onMenuResources()
            },
            onCardsAndQuestions = {
                showMenu = false
                onMenuCardsAndQuestions()
            },
            onSettings = {
                showMenu = false
                onMenuSettings()
            },
        )
    }
}

/**
 * Modal bottom sheet navigation menu with rows for the main app destinations.
 *
 * @param onDismiss             called when the sheet is dismissed (back gesture or
 *                              the close row).
 * @param onMySoularium         called when the MySoularium row is tapped.
 * @param onPastConversations   called when the Past Conversations row is tapped.
 * @param onAbout               called when the About row is tapped.
 * @param onResources           called when the Resources row is tapped.
 * @param onCardsAndQuestions   called when the Images & Questions row is tapped.
 * @param onSettings            called when the Settings row is tapped.
 */
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
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
