package org.cru.soularium.ui.conversation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.card_a11y_description_1
import org.cru.soularium.generated.resources.cd_card_zoom_named
import org.cru.soularium.ui.content.CardAsset
import org.cru.soularium.ui.test.FakeOverlayHost
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class SelectionLayoutTest {
    private val overlayHost = FakeOverlayHost()
    private val eventSink = TestEventSink<ConversationPresenter.UiEvent>()

    private val state = ConversationPresenter.UiState.Selection(
        questionNumber = 1,
        selectedCardIds = emptyList(),
        isConfirmEnabled = false,
        showExitDialog = false,
        eventSink = eventSink::invoke,
    )

    @Test
    fun `Zoom - Tap - shows the card zoom overlay`() = runComposeUiTest {
        setSelectionLayoutContent(state)

        onNode(hasContentDescription(card1ZoomLabel())).performClick()
        val overlay = assertIs<CardZoomOverlay>(overlayHost.awaitOverlay())
        assertEquals(CardAsset.CARD_01, overlay.card)
        assertFalse(overlay.isSelected)
    }

    @Test
    fun `Zoom - selected card - shows the overlay as selected`() = runComposeUiTest {
        setSelectionLayoutContent(state.copy(selectedCardIds = listOf(1)))

        onNode(hasContentDescription(card1ZoomLabel())).performClick()
        val overlay = assertIs<CardZoomOverlay>(overlayHost.awaitOverlay())
        assertEquals(CardAsset.CARD_01, overlay.card)
        assertTrue(overlay.isSelected)
    }

    @Test
    fun `Zoom - Select in overlay - emits ToggleCard for that card`() = runComposeUiTest {
        setSelectionLayoutContent(state)

        onNode(hasContentDescription(card1ZoomLabel())).performClick()
        overlayHost.awaitOverlayNavigator().finish(CardZoomOverlay.Result.ToggleSelection)
        awaitIdle()

        eventSink.assertEvent(ConversationPresenter.UiEvent.Selection.ToggleCard(1))
    }

    @Test
    fun `Zoom - Close - emits nothing`() = runComposeUiTest {
        setSelectionLayoutContent(state)

        onNode(hasContentDescription(card1ZoomLabel())).performClick()
        overlayHost.awaitOverlayNavigator().finish(CardZoomOverlay.Result.Dismissed)
        awaitIdle()

        eventSink.assertNoEvents()
    }

    private fun ComposeUiTest.setSelectionLayoutContent(state: ConversationPresenter.UiState.Selection) = setContent {
        CompositionLocalProvider(LocalOverlayHost provides overlayHost) { SelectionLayout(state) }
    }

    private suspend fun card1ZoomLabel() =
        getString(Res.string.cd_card_zoom_named, getString(Res.string.card_a11y_description_1))
}
