package org.cru.soularium.ui.conversation

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.slack.circuit.overlay.OverlayEffect
import org.cru.soularium.ui.content.CardAsset
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SelectionLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // Selection states: empty, partial (multi-pick questions 1-2 require 3
    // cards), and complete with the confirm button enabled.
    @Test
    fun `SelectionLayout() - question 1 - no selection`() = snapshot {
        SelectionLayout(state = selectionState(questionNumber = 1, selectedCardIds = emptyList()))
    }

    @Test
    fun `SelectionLayout() - question 2 - partial selection`() = snapshot {
        SelectionLayout(state = selectionState(questionNumber = 2, selectedCardIds = listOf(8, 25)))
    }

    @Test
    fun `SelectionLayout() - question 3 - selection complete`() = snapshot {
        SelectionLayout(
            state = selectionState(questionNumber = 3, selectedCardIds = listOf(15), isConfirmEnabled = true),
        )
    }

    // The zoom overlay shown over the grid: the translucent scrim dims the cards
    // behind the full-size artwork and its Close / Select actions.
    @Test
    fun `SelectionLayout() - card zoom overlay`() = snapshot {
        SelectionLayout(state = selectionState(questionNumber = 1, selectedCardIds = emptyList()))
        OverlayEffect {
            show(CardZoomOverlay(CardAsset.CARD_02, isSelected = false))
        }
    }
}

private fun selectionState(questionNumber: Int, selectedCardIds: List<Int>, isConfirmEnabled: Boolean = false) =
    ConversationPresenter.UiState.Selection(
        questionNumber = questionNumber,
        selectedCardIds = selectedCardIds,
        isConfirmEnabled = isConfirmEnabled,
        showExitDialog = false,
        eventSink = {},
    )
