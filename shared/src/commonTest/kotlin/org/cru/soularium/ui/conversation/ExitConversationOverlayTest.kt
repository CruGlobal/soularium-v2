package org.cru.soularium.ui.conversation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.ccci.gto.android.common.testing.circuit.overlay.TestOverlayNavigator
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_cancel
import org.cru.soularium.generated.resources.conversation_exit_bookmark
import org.cru.soularium.generated.resources.conversation_exit_discard
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class ExitConversationOverlayTest {
    @Test
    fun `Bookmark - Tap - finishes with Bookmark`() = runComposeUiTest {
        val navigator = TestOverlayNavigator<ExitConversationOverlay.Result>()
        setContent { ExitConversationOverlay().Content(navigator) }
        waitForIdle()

        onNode(hasText(getString(Res.string.conversation_exit_bookmark))).performClick()
        waitForIdle()

        assertEquals(ExitConversationOverlay.Result.Bookmark, navigator.awaitResult())
    }

    @Test
    fun `Discard - Tap - finishes with Discard`() = runComposeUiTest {
        val navigator = TestOverlayNavigator<ExitConversationOverlay.Result>()
        setContent { ExitConversationOverlay().Content(navigator) }
        waitForIdle()

        onNode(hasText(getString(Res.string.conversation_exit_discard))).performClick()
        waitForIdle()

        assertEquals(ExitConversationOverlay.Result.Discard, navigator.awaitResult())
    }

    @Test
    fun `Cancel - Tap - finishes with Cancelled`() = runComposeUiTest {
        val navigator = TestOverlayNavigator<ExitConversationOverlay.Result>()
        setContent { ExitConversationOverlay().Content(navigator) }
        waitForIdle()

        onNode(hasText(getString(Res.string.action_cancel))).performClick()
        waitForIdle()

        assertEquals(ExitConversationOverlay.Result.Cancelled, navigator.awaitResult())
    }
}
