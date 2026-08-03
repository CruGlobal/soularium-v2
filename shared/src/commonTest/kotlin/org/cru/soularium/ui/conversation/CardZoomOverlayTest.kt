package org.cru.soularium.ui.conversation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.ccci.gto.android.common.testing.circuit.overlay.TestOverlayNavigator
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_deselect
import org.cru.soularium.generated.resources.action_select
import org.cru.soularium.generated.resources.card_a11y_description_1
import org.cru.soularium.generated.resources.cd_card_zoom_close
import org.cru.soularium.ui.content.CardAsset
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class CardZoomOverlayTest {
    @Test
    fun `Content - UI - shows the full-size card image`() = runComposeUiTest {
        setContent { CardZoomOverlay(CardAsset.CARD_01, isSelected = false).Content { } }
        waitForIdle()

        onNode(hasContentDescription(getString(Res.string.card_a11y_description_1))).assertExists()
    }

    @Test
    fun `Content - UI - unselected card offers Select`() = runComposeUiTest {
        setContent { CardZoomOverlay(CardAsset.CARD_01, isSelected = false).Content { } }
        waitForIdle()

        onNode(hasText(getString(Res.string.action_select))).assertExists()
        onNode(hasText(getString(Res.string.action_deselect))).assertDoesNotExist()
    }

    @Test
    fun `Content - UI - selected card offers Deselect`() = runComposeUiTest {
        setContent { CardZoomOverlay(CardAsset.CARD_01, isSelected = true).Content { } }
        waitForIdle()

        onNode(hasText(getString(Res.string.action_deselect))).assertExists()
        onNode(hasText(getString(Res.string.action_select))).assertDoesNotExist()
    }

    @Test
    fun `Close - Tap - finishes with Dismissed`() = runComposeUiTest {
        val navigator = TestOverlayNavigator<CardZoomOverlay.Result>()
        setContent { CardZoomOverlay(CardAsset.CARD_01, isSelected = false).Content(navigator) }
        waitForIdle()

        onNode(hasContentDescription(getString(Res.string.cd_card_zoom_close))).performClick()
        waitForIdle()

        assertEquals(CardZoomOverlay.Result.Dismissed, navigator.awaitResult())
    }

    @Test
    fun `Scrim - Tap - finishes with Dismissed`() = runComposeUiTest {
        val navigator = TestOverlayNavigator<CardZoomOverlay.Result>()
        setContent { CardZoomOverlay(CardAsset.CARD_01, isSelected = false).Content(navigator) }
        waitForIdle()

        // The screen center is the image area — a child of the scrim's clickable.
        onRoot().performClick()
        waitForIdle()

        assertEquals(CardZoomOverlay.Result.Dismissed, navigator.awaitResult())
    }

    @Test
    fun `Select - Tap - finishes with ToggleSelection`() = runComposeUiTest {
        val navigator = TestOverlayNavigator<CardZoomOverlay.Result>()
        setContent { CardZoomOverlay(CardAsset.CARD_01, isSelected = false).Content(navigator) }
        waitForIdle()

        onNode(hasText(getString(Res.string.action_select))).performClick()
        waitForIdle()

        assertEquals(CardZoomOverlay.Result.ToggleSelection, navigator.awaitResult())
    }

    @Test
    fun `Deselect - Tap - finishes with ToggleSelection`() = runComposeUiTest {
        val navigator = TestOverlayNavigator<CardZoomOverlay.Result>()
        setContent { CardZoomOverlay(CardAsset.CARD_01, isSelected = true).Content(navigator) }
        waitForIdle()

        onNode(hasText(getString(Res.string.action_deselect))).performClick()
        waitForIdle()

        assertEquals(CardZoomOverlay.Result.ToggleSelection, navigator.awaitResult())
    }
}
