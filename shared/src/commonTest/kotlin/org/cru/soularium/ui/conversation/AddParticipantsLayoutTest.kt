package org.cru.soularium.ui.conversation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.contact_create_prompt
import org.cru.soularium.generated.resources.participants_empty
import org.cru.soularium.generated.resources.participants_empty_solo
import org.cru.soularium.generated.resources.participants_name_placeholder
import org.cru.soularium.generated.resources.participants_title
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class AddParticipantsLayoutTest {
    private fun state(isGroup: Boolean, participantNames: List<String> = emptyList()) =
        ConversationPresenter.UiState.AddingParticipants(
            participantNames = participantNames,
            isGroup = isGroup,
            showExitDialog = false,
            eventSink = {},
        )

    @Test
    fun `UI - title - group asks who else is in the conversation`() = runComposeUiTest {
        setContent { AddParticipantsLayout(state(isGroup = true)) }

        onNode(hasText(getString(Res.string.participants_title))).assertExists()
    }

    @Test
    fun `UI - title - solo shows the MySoularium prompt`() = runComposeUiTest {
        setContent { AddParticipantsLayout(state(isGroup = false)) }

        onNode(hasText(getString(Res.string.contact_create_prompt))).assertExists()
        onNode(hasText(getString(Res.string.participants_title))).assertDoesNotExist()
    }

    @Test
    fun `UI - empty hint - group asks to add at least one person`() = runComposeUiTest {
        setContent { AddParticipantsLayout(state(isGroup = true)) }

        onNode(hasText(getString(Res.string.participants_empty))).assertExists()
    }

    @Test
    fun `UI - empty hint - solo asks for your name`() = runComposeUiTest {
        setContent { AddParticipantsLayout(state(isGroup = false)) }

        onNode(hasText(getString(Res.string.participants_empty_solo))).assertExists()
        onNode(hasText(getString(Res.string.participants_empty))).assertDoesNotExist()
    }

    @Test
    fun `UI - name placeholder - solo omits the example name`() = runComposeUiTest {
        setContent { AddParticipantsLayout(state(isGroup = false)) }

        onNode(hasText(getString(Res.string.participants_name_placeholder))).assertDoesNotExist()
    }
}
