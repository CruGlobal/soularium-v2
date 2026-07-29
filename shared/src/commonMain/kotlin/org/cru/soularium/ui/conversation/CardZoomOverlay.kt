package org.cru.soularium.ui.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayNavigator
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_deselect
import org.cru.soularium.generated.resources.action_select
import org.cru.soularium.generated.resources.cd_card_zoom_close
import org.cru.soularium.platform.PlatformBackHandler
import org.cru.soularium.ui.content.CardAsset
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// Heavy enough that the full-size artwork dominates; the selection grid stays
// faintly visible behind it to keep the sense of floating over the screen.
private const val SCRIM_ALPHA = 0.9f

/**
 * An [Overlay] that shows [card]'s full-size artwork over the selection grid and
 * returns whether the user chose to toggle the card's selection while viewing it.
 */
internal class CardZoomOverlay(private val card: CardAsset, private val isSelected: Boolean) :
    Overlay<CardZoomOverlay.Result> {
    sealed interface Result {
        data object ToggleSelection : Result
        data object Dismissed : Result
    }

    @Composable
    override fun Content(navigator: OverlayNavigator<Result>) {
        PlatformBackHandler(enabled = true) { navigator.finish(Result.Dismissed) }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                // Consume taps so they can't reach the selection grid behind the
                // translucent scrim; tapping outside the actions dismisses.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = stringResource(Res.string.cd_card_zoom_close),
                ) { navigator.finish(Result.Dismissed) },
            color = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledTonalIconButton(onClick = { navigator.finish(Result.Dismissed) }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.cd_card_zoom_close),
                        )
                    }
                }

                Image(
                    painter = painterResource(card.full),
                    contentDescription = card.contentDescription?.let { stringResource(it) },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                val actionRes = if (isSelected) Res.string.action_deselect else Res.string.action_select
                Button(
                    onClick = { navigator.finish(Result.ToggleSelection) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = stringResource(actionRes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
