package org.cru.soularium.ui.conversation

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.domain.DomainError
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class ConversationSummaryLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // Loading state: the reactive collect hasn't emitted yet.
    @Test
    fun `ConversationSummaryLayout() - loading`() = snapshot {
        ConversationSummaryLayout(
            state = ConversationSummaryPresenter.UiState(
                participants = emptyList(),
                isLoading = true,
                error = null,
                eventSink = {},
            ),
        )
    }

    // Error state: repository observation failed; render the error affordance.
    @Test
    fun `ConversationSummaryLayout() - error`() = snapshot {
        ConversationSummaryLayout(
            state = ConversationSummaryPresenter.UiState(
                participants = emptyList(),
                isLoading = false,
                error = DomainError.PersistenceFailed,
                eventSink = {},
            ),
        )
    }

    // Single participant: no TabRow, just the mosaic + share + back.
    @Test
    fun `ConversationSummaryLayout() - single participant`() = snapshot {
        ConversationSummaryLayout(
            state = ConversationSummaryPresenter.UiState(
                participants = listOf(
                    ParticipantSummary(
                        participantIndex = 0,
                        name = "Ada",
                        cardIds = listOf(3, 17, 42, 8, 25, 33, 15, 41, 7),
                    ),
                ),
                isLoading = false,
                error = null,
                eventSink = {},
            ),
        )
    }

    // Multiple participants: TabRow at the top.
    @Test
    fun `ConversationSummaryLayout() - multiple participants`() = snapshot {
        ConversationSummaryLayout(
            state = ConversationSummaryPresenter.UiState(
                participants = listOf(
                    ParticipantSummary(
                        participantIndex = 0,
                        name = "Ada",
                        cardIds = listOf(3, 17, 42, 8, 25, 33, 15, 41, 7),
                    ),
                    ParticipantSummary(
                        participantIndex = 1,
                        name = "Grace",
                        cardIds = listOf(5, 12, 28, 19, 36, 44, 22, 30, 11),
                    ),
                    ParticipantSummary(
                        participantIndex = 2,
                        name = "Alan",
                        cardIds = listOf(1, 4, 9, 16, 21, 27, 34, 38, 47),
                    ),
                ),
                isLoading = false,
                error = null,
                eventSink = {},
            ),
        )
    }
}
