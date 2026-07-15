package org.cru.soularium.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.menu_about
import org.cru.soularium.generated.resources.menu_cards_and_questions
import org.cru.soularium.generated.resources.menu_close
import org.cru.soularium.generated.resources.menu_my_soularium
import org.cru.soularium.generated.resources.menu_past_conversations
import org.cru.soularium.generated.resources.menu_resources
import org.cru.soularium.generated.resources.menu_settings
import org.jetbrains.compose.resources.stringResource

/**
 * An [Overlay] that presents the home menu in a [ModalBottomSheet] and returns the chosen [Result].
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class HomeMenuOverlay(private val sheetState: SheetState? = null) : Overlay<HomeMenuOverlay.Result> {
    sealed interface Result {
        data object MySoularium : Result
        data object PastConversations : Result
        data object About : Result
        data object Resources : Result
        data object CardsAndQuestions : Result
        data object Settings : Result
        data object Dismissed : Result
    }

    @Composable
    override fun Content(navigator: OverlayNavigator<Result>) {
        val scope = rememberCoroutineScope()
        val state = sheetState ?: rememberModalBottomSheetState()
        var pendingResult by remember { mutableStateOf<Result?>(null) }

        ModalBottomSheet(
            onDismissRequest = { navigator.finish(Result.Dismissed) },
            sheetState = state,
        ) {
            MenuBottomSheetContent(
                onSelect = { result ->
                    scope.launch {
                        // Delay the result until the sheet has finished animating away.
                        pendingResult = result
                        state.hide()
                    }
                },
            )
        }

        LaunchedEffect(Unit) {
            // we launch the show in a child coroutine so cancellation doesn't cancel the snapshotFlow
            launch(start = CoroutineStart.UNDISPATCHED) { state.show() }

            // finish the Overlay when it is no longer visible and an animation is no longer running
            snapshotFlow { state.isVisible || state.isAnimationRunning }
                .filterNot { it }
                .collect {
                    navigator.finish(pendingResult ?: Result.Dismissed)
                }
        }
    }

    @Composable
    private fun MenuBottomSheetContent(onSelect: (Result) -> Unit) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            MenuRow(
                icon = Icons.Default.AccountCircle,
                label = stringResource(Res.string.menu_my_soularium),
                onClick = { onSelect(Result.MySoularium) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MenuRow(
                icon = Icons.Default.History,
                label = stringResource(Res.string.menu_past_conversations),
                onClick = { onSelect(Result.PastConversations) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MenuRow(
                icon = Icons.Default.Info,
                label = stringResource(Res.string.menu_about),
                onClick = { onSelect(Result.About) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MenuRow(
                icon = Icons.Default.ChatBubble,
                label = stringResource(Res.string.menu_resources),
                onClick = { onSelect(Result.Resources) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MenuRow(
                icon = Icons.Default.GridView,
                label = stringResource(Res.string.menu_cards_and_questions),
                onClick = { onSelect(Result.CardsAndQuestions) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MenuRow(
                icon = Icons.Default.Settings,
                label = stringResource(Res.string.menu_settings),
                onClick = { onSelect(Result.Settings) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MenuRow(
                icon = Icons.Default.Close,
                label = stringResource(Res.string.menu_close),
                onClick = { onSelect(Result.Dismissed) },
            )
        }
    }

    @Composable
    private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
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
}
