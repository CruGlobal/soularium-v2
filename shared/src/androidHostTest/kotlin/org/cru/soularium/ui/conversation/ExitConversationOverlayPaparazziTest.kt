package org.cru.soularium.ui.conversation

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.slack.circuit.overlay.OverlayEffect
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class ExitConversationOverlayPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // The bookmark/discard exit confirmation over the Add Participants page.
    @Test
    fun `ExitConversationOverlay()`() = snapshot {
        AddParticipantsLayout(
            state = ConversationPresenter.UiState.AddingParticipants(
                participantNames = listOf("Alice", "Bob"),
                isGroup = true,
                showExitDialog = true,
                eventSink = {},
            ),
        )
        OverlayEffect {
            show(ExitConversationOverlay())
        }
    }
}
