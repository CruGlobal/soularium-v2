package org.cru.soularium.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.ccci.gto.android.common.testing.circuit.overlay.TestOverlayNavigator
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.menu_about
import org.cru.soularium.generated.resources.menu_cards_and_questions
import org.cru.soularium.generated.resources.menu_close
import org.cru.soularium.generated.resources.menu_my_soularium
import org.cru.soularium.generated.resources.menu_past_conversations
import org.cru.soularium.generated.resources.menu_resources
import org.cru.soularium.generated.resources.menu_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class HomeMenuOverlayTest {
    // region Menu
    @Test
    fun `Menu - SideEffect - selecting before it opens still finishes`() = runComposeUiTest {
        val navigator = TestOverlayNavigator<HomeMenuOverlay.Result>()

        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Hidden,
            skipHiddenState = false,
        )

        mainClock.autoAdvance = false
        setContent { HomeMenuOverlay(sheetState = sheetState).Content(navigator) }

        // Tap a row while the sheet is still animating open, so the row's hide() cancels show().
        val row = onNode(hasText(getString(Res.string.menu_my_soularium)))
        var frames = 0
        while (frames++ < 60 && runCatching { row.assertIsDisplayed() }.isFailure) {
            mainClock.advanceTimeByFrame()
        }
        row.assertIsDisplayed()
        row.performClick()

        mainClock.autoAdvance = true
        waitForIdle()

        assertEquals(HomeMenuOverlay.Result.MySoularium, navigator.awaitResult())
    }

    @Test
    fun `Menu - UI - shows every item`() = runComposeUiTest {
        setContent { HomeMenuOverlay(sheetState = expandedSheetState()).Content { } }
        waitForIdle()

        val labels = listOf(
            Res.string.menu_my_soularium,
            Res.string.menu_past_conversations,
            Res.string.menu_about,
            Res.string.menu_resources,
            Res.string.menu_cards_and_questions,
            Res.string.menu_settings,
            Res.string.menu_close,
        )
        for (label in labels) {
            onNode(hasText(getString(label))).assertExists()
        }
    }
    // endregion Menu

    // region Rows
    @Test
    fun `Row - About - Tap - finishes with About`() =
        tappingRowFinishesWith(Res.string.menu_about, HomeMenuOverlay.Result.About)

    @Test
    fun `Row - Cards and Questions - Tap - finishes with CardsAndQuestions`() =
        tappingRowFinishesWith(Res.string.menu_cards_and_questions, HomeMenuOverlay.Result.CardsAndQuestions)

    @Test
    fun `Row - Close - Tap - finishes with Dismissed`() =
        tappingRowFinishesWith(Res.string.menu_close, HomeMenuOverlay.Result.Dismissed)

    @Test
    fun `Row - My Soularium - Tap - finishes with MySoularium`() =
        tappingRowFinishesWith(Res.string.menu_my_soularium, HomeMenuOverlay.Result.MySoularium)

    @Test
    fun `Row - Past Conversations - Tap - finishes with PastConversations`() =
        tappingRowFinishesWith(Res.string.menu_past_conversations, HomeMenuOverlay.Result.PastConversations)

    @Test
    fun `Row - Resources - Tap - finishes with Resources`() =
        tappingRowFinishesWith(Res.string.menu_resources, HomeMenuOverlay.Result.Resources)

    @Test
    fun `Row - Settings - Tap - finishes with Settings`() =
        tappingRowFinishesWith(Res.string.menu_settings, HomeMenuOverlay.Result.Settings)
    // endregion Rows

    // region Helpers
    private fun tappingRowFinishesWith(label: StringResource, expected: HomeMenuOverlay.Result) = runComposeUiTest {
        val navigator = TestOverlayNavigator<HomeMenuOverlay.Result>()

        setContent { HomeMenuOverlay(sheetState = expandedSheetState()).Content(navigator) }
        waitForIdle()

        onNode(hasText(getString(label))).performScrollTo().performClick()
        waitForIdle()

        assertEquals(expected, navigator.awaitResult())
    }

    // skipHiddenState = false so a row tap's hide() still works.
    private fun expandedSheetState() = SheetState(
        skipPartiallyExpanded = true,
        positionalThreshold = { 0f },
        velocityThreshold = { 0f },
        initialValue = SheetValue.Expanded,
        skipHiddenState = false,
    )
    // endregion Helpers
}
