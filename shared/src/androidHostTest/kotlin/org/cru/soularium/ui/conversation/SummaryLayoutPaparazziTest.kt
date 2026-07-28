package org.cru.soularium.ui.conversation

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SummaryLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // Single participant: no TabRow; renders the "That's a Wrap" flourish, the
    // participant heading, the per-question sections, and Share + Add Contact.
    @Test
    fun `SummaryLayout() - single participant`() = snapshot {
        SummaryLayout(
            state = ConversationPresenter.UiState.Summary(
                participants = listOf(ada()),
                showExitDialog = false,
                eventSink = {},
            ),
        )
    }

    // Multiple participants: TabRow at the top; the selected participant's
    // sections render below (default: tab 0).
    @Test
    fun `SummaryLayout() - multiple participants`() = snapshot {
        SummaryLayout(
            state = ConversationPresenter.UiState.Summary(
                participants = listOf(ada(), grace(), alan()),
                showExitDialog = false,
                eventSink = {},
            ),
        )
    }
}

private fun ada() = ParticipantSummary(
    participantIndex = 0,
    name = "Ada",
    selections = listOf(
        QuestionSelections(1, listOf(3, 17, 42)),
        QuestionSelections(2, listOf(8, 25, 33)),
        QuestionSelections(3, listOf(15)),
        QuestionSelections(4, listOf(41)),
        QuestionSelections(5, listOf(7)),
    ),
)

private fun grace() = ParticipantSummary(
    participantIndex = 1,
    name = "Grace",
    selections = listOf(
        QuestionSelections(1, listOf(5, 12, 28)),
        QuestionSelections(2, listOf(19, 36, 44)),
        QuestionSelections(3, listOf(22)),
        QuestionSelections(4, listOf(30)),
        QuestionSelections(5, listOf(11)),
    ),
)

private fun alan() = ParticipantSummary(
    participantIndex = 2,
    name = "Alan",
    selections = listOf(
        QuestionSelections(1, listOf(1, 4, 9)),
        QuestionSelections(2, listOf(16, 21, 27)),
        QuestionSelections(3, listOf(34)),
        QuestionSelections(4, listOf(38)),
        QuestionSelections(5, listOf(47)),
    ),
)
