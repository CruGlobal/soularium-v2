package org.cru.soularium.ui.screens

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.model.Session
import org.cru.soularium.model.SessionId
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PastConversationsLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    @Test
    fun `PastConversationsLayout() - empty`() = snapshot {
        PastConversationsLayout(
            state = PastConversationsPresenter.UiState(
                completed = emptyList(),
                bookmarked = emptyList(),
                eventSink = {},
            ),
        )
    }

    @Test
    fun `PastConversationsLayout() - populated`() = snapshot {
        PastConversationsLayout(
            state = PastConversationsPresenter.UiState(
                completed = listOf(
                    PastConversationItem(
                        sessionId = SessionId.random(),
                        kind = Session.Kind.GROUP,
                        formattedDate = "Jul 10, 2026",
                        participantNames = listOf("Ada", "Grace", "Alan"),
                    ),
                    PastConversationItem(
                        sessionId = SessionId.random(),
                        kind = Session.Kind.SOLO,
                        formattedDate = "Jun 28, 2026",
                        participantNames = listOf("Ada"),
                    ),
                ),
                bookmarked = listOf(
                    PastConversationItem(
                        sessionId = SessionId.random(),
                        kind = Session.Kind.GROUP,
                        formattedDate = "Jul 12, 2026",
                        participantNames = listOf("Riley", "Sam"),
                    ),
                ),
                eventSink = {},
            ),
        )
    }
}
