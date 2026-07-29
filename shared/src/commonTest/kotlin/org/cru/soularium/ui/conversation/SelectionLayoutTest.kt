package org.cru.soularium.ui.conversation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.overlay.ContentWithOverlays
import kotlin.test.Test
import kotlin.test.assertEquals
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_deselect
import org.cru.soularium.generated.resources.action_select
import org.cru.soularium.generated.resources.card_a11y_description_1
import org.cru.soularium.generated.resources.cd_card_zoom_close
import org.cru.soularium.generated.resources.cd_card_zoom_named
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class SelectionLayoutTest {
    @Test
    fun `Zoom - Tap - shows the full screen image overlay`() = runComposeUiTest {
        setContent { ContentWithOverlays { SelectionLayout(selectionState()) } }
        waitForIdle()

        onNode(hasContentDescription(card1ZoomLabel())).performClick()
        waitForIdle()

        onNode(hasContentDescription(getString(Res.string.cd_card_zoom_close))).assertIsDisplayed()
    }

    @Test
    fun `Zoom - Select in overlay - emits ToggleCard for that card and closes`() = runComposeUiTest {
        val events = mutableListOf<ConversationPresenter.UiEvent>()
        setContent { ContentWithOverlays { SelectionLayout(selectionState(eventSink = events::add)) } }
        waitForIdle()

        onNode(hasContentDescription(card1ZoomLabel())).performClick()
        waitForIdle()
        onNode(hasText(getString(Res.string.action_select))).performClick()
        waitForIdle()

        val expected = listOf<ConversationPresenter.UiEvent>(ConversationPresenter.UiEvent.Selection.ToggleCard(1))
        assertEquals(expected, events)
        onNode(hasContentDescription(getString(Res.string.cd_card_zoom_close))).assertDoesNotExist()
    }

    @Test
    fun `Zoom - selected card - offers Deselect in overlay`() = runComposeUiTest {
        setContent { ContentWithOverlays { SelectionLayout(selectionState(selectedCardIds = listOf(1))) } }
        waitForIdle()

        onNode(hasContentDescription(card1ZoomLabel())).performClick()
        waitForIdle()

        onNode(hasText(getString(Res.string.action_deselect))).assertIsDisplayed()
    }

    @Test
    fun `Zoom - Close - emits nothing and returns to the grid`() = runComposeUiTest {
        val events = mutableListOf<ConversationPresenter.UiEvent>()
        setContent { ContentWithOverlays { SelectionLayout(selectionState(eventSink = events::add)) } }
        waitForIdle()

        onNode(hasContentDescription(card1ZoomLabel())).performClick()
        waitForIdle()
        onNode(hasContentDescription(getString(Res.string.cd_card_zoom_close))).performClick()
        waitForIdle()

        assertEquals(emptyList<ConversationPresenter.UiEvent>(), events)
        onNode(hasContentDescription(getString(Res.string.cd_card_zoom_close))).assertDoesNotExist()
    }

    private suspend fun card1ZoomLabel() =
        getString(Res.string.cd_card_zoom_named, getString(Res.string.card_a11y_description_1))

    private fun selectionState(
        selectedCardIds: List<Int> = emptyList(),
        eventSink: (ConversationPresenter.UiEvent) -> Unit = {},
    ) = ConversationPresenter.UiState.Selection(
        questionNumber = 1,
        selectedCardIds = selectedCardIds,
        isConfirmEnabled = false,
        showExitDialog = false,
        eventSink = eventSink,
    )
}
