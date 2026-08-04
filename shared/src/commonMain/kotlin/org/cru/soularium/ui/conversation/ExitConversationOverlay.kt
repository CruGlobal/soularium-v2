package org.cru.soularium.ui.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_cancel
import org.cru.soularium.generated.resources.conversation_exit_bookmark
import org.cru.soularium.generated.resources.conversation_exit_discard
import org.cru.soularium.generated.resources.conversation_exit_message
import org.cru.soularium.generated.resources.conversation_exit_title
import org.jetbrains.compose.resources.stringResource

/**
 * An [Overlay] confirming a back-out of an in-progress conversation: bookmark it
 * for later, discard the progress, or stay.
 */
internal class ExitConversationOverlay : Overlay<ExitConversationOverlay.Result> {
    sealed interface Result {
        data object Bookmark : Result
        data object Discard : Result
        data object Cancelled : Result
    }

    @Composable
    override fun Content(navigator: OverlayNavigator<Result>) {
        AlertDialog(
            onDismissRequest = { navigator.finish(Result.Cancelled) },
            title = { Text(stringResource(Res.string.conversation_exit_title)) },
            text = { Text(stringResource(Res.string.conversation_exit_message)) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { navigator.finish(Result.Bookmark) }) {
                        Text(stringResource(Res.string.conversation_exit_bookmark))
                    }
                    TextButton(onClick = { navigator.finish(Result.Discard) }) {
                        Text(stringResource(Res.string.conversation_exit_discard))
                    }
                    TextButton(onClick = { navigator.finish(Result.Cancelled) }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                }
            },
        )
    }
}
