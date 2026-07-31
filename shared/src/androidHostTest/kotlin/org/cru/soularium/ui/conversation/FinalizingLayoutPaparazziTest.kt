package org.cru.soularium.ui.conversation

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class FinalizingLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // Questions 1-2 render the three-card row, 3-5 the single image.
    @Test
    fun `FinalizingLayout() - question 1 - three cards`() = snapshot {
        FinalizingLayout(state = finalizingState(questionNumber = 1, cardIds = listOf(3, 17, 42)))
    }

    @Test
    fun `FinalizingLayout() - question 3 - single card`() = snapshot {
        FinalizingLayout(state = finalizingState(questionNumber = 3, cardIds = listOf(15)))
    }
}

private fun finalizingState(questionNumber: Int, cardIds: List<Int>) = ConversationPresenter.UiState.Finalizing(
    questionNumber = questionNumber,
    cardIds = cardIds,
    showExitDialog = false,
    eventSink = {},
)
