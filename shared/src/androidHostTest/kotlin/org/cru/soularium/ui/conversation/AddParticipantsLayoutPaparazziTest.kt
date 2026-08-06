package org.cru.soularium.ui.conversation

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AddParticipantsLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    @Test
    fun `AddParticipantsLayout() - group - empty`() = snapshot {
        AddParticipantsLayout(state = addingParticipantsState(isGroup = true))
    }

    @Test
    fun `AddParticipantsLayout() - group - with participants`() = snapshot {
        AddParticipantsLayout(
            state = addingParticipantsState(isGroup = true, participantNames = listOf("Alice", "Bob")),
        )
    }

    @Test
    fun `AddParticipantsLayout() - solo - empty`() = snapshot {
        AddParticipantsLayout(state = addingParticipantsState(isGroup = false))
    }
}

private fun addingParticipantsState(isGroup: Boolean, participantNames: List<String> = emptyList()) =
    ConversationPresenter.UiState.AddingParticipants(
        participantNames = participantNames,
        isGroup = isGroup,
        showExitDialog = false,
        eventSink = {},
    )
