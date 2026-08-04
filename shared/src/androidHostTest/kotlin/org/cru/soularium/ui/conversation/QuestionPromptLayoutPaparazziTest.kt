package org.cru.soularium.ui.conversation

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class QuestionPromptLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // The group snapshot covers the "your turn" greeting branch.
    @Test
    fun `QuestionPromptLayout() - question 1 - solo`() = snapshot {
        QuestionPromptLayout(state = promptState(questionNumber = 1))
    }

    @Test
    fun `QuestionPromptLayout() - question 3 - group`() = snapshot {
        QuestionPromptLayout(state = promptState(questionNumber = 3, isGroup = true))
    }
}

private fun promptState(questionNumber: Int, isGroup: Boolean = false) = ConversationPresenter.UiState.QuestionPrompt(
    questionNumber = questionNumber,
    totalQuestions = 5,
    participantName = "Ada",
    isGroup = isGroup,
    showExitDialog = false,
    eventSink = {},
)
