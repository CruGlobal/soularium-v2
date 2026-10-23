package org.cru.soularium.ui.conversation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.LocalOverlayState
import com.slack.circuit.overlay.OverlayState
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test
import kotlin.test.assertIs
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.ui.test.FakeOverlayHost

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class ConversationLayoutTest {
    private val overlayHost = FakeOverlayHost()
    private val eventSink = TestEventSink<ConversationPresenter.UiEvent>()

    private fun addingParticipants(showExitDialog: Boolean) = ConversationPresenter.UiState.AddingParticipants(
        participantNames = listOf("Alice"),
        isGroup = true,
        showExitDialog = showExitDialog,
        eventSink = eventSink::invoke,
    )

    private fun ComposeUiTest.setConversationLayoutContent(state: ConversationPresenter.UiState) = setContent {
        CompositionLocalProvider(
            LocalOverlayHost provides overlayHost,
            LocalOverlayState provides OverlayState.HIDDEN,
        ) { ConversationLayout(state) }
    }

    @Test
    fun `UI - ExitOverlay - shown while showExitDialog is set`() = runComposeUiTest {
        setConversationLayoutContent(addingParticipants(showExitDialog = true))
        awaitIdle()

        assertIs<ExitConversationOverlay>(overlayHost.awaitOverlay())
    }

    @Test
    fun `UI - ExitOverlay - Bookmark result emits BookmarkAndExit`() = runComposeUiTest {
        setConversationLayoutContent(addingParticipants(showExitDialog = true))
        awaitIdle()

        overlayHost.awaitOverlayNavigator().finish(ExitConversationOverlay.Result.Bookmark)
        awaitIdle()

        eventSink.assertEvent(ConversationPresenter.UiEvent.BookmarkAndExit)
    }

    @Test
    fun `UI - ExitOverlay - Discard result emits DiscardAndExit`() = runComposeUiTest {
        setConversationLayoutContent(addingParticipants(showExitDialog = true))
        awaitIdle()

        overlayHost.awaitOverlayNavigator().finish(ExitConversationOverlay.Result.Discard)
        awaitIdle()

        eventSink.assertEvent(ConversationPresenter.UiEvent.DiscardAndExit)
    }

    @Test
    fun `UI - ExitOverlay - Cancelled result emits DismissExitDialog`() = runComposeUiTest {
        setConversationLayoutContent(addingParticipants(showExitDialog = true))
        awaitIdle()

        overlayHost.awaitOverlayNavigator().finish(ExitConversationOverlay.Result.Cancelled)
        awaitIdle()

        eventSink.assertEvent(ConversationPresenter.UiEvent.DismissExitDialog)
    }
}
