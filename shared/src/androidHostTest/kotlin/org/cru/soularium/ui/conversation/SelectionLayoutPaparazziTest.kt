package org.cru.soularium.ui.conversation

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.content.CardAsset
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SelectionLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // No picks yet: the "finish making your selections" hint is visible, Confirm is
    // disabled, and each card shows only its zoom badge.
    @Test
    fun `SelectionLayout() - no selections`() = snapshot {
        SelectionLayout(state = selectionState())
    }

    // Picks complete: selected cards show their check badge, the hint is gone, and
    // Confirm is enabled. Low card ids keep the badges within the visible rows.
    @Test
    fun `SelectionLayout() - selections complete`() = snapshot {
        SelectionLayout(state = selectionState(selectedCardIds = listOf(2, 4, 9), isConfirmEnabled = true))
    }

    // The zoom overlay stacked over the grid: the translucent scrim dims the cards
    // behind the full-size artwork and its Close / Select actions.
    @Test
    fun `SelectionLayout() - card zoom overlay`() = snapshot {
        SelectionLayout(state = selectionState())
        CardZoomOverlay(CardAsset.CARD_02, isSelected = false).Content { }
    }
}

private fun selectionState(selectedCardIds: List<Int> = emptyList(), isConfirmEnabled: Boolean = false) =
    ConversationPresenter.UiState.Selection(
        questionNumber = 1,
        selectedCardIds = selectedCardIds,
        isConfirmEnabled = isConfirmEnabled,
        showExitDialog = false,
        eventSink = {},
    )
