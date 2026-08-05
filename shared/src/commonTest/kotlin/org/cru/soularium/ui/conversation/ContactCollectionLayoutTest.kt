package org.cru.soularium.ui.conversation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test
import kotlin.test.assertEquals
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_save
import org.cru.soularium.generated.resources.contact_email_hint
import org.cru.soularium.generated.resources.contact_first_name
import org.cru.soularium.generated.resources.contact_invalid_email
import org.cru.soularium.generated.resources.contact_invalid_phone
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class ContactCollectionLayoutTest {
    private val eventSink = TestEventSink<ConversationPresenter.UiEvent>()

    private fun collectingContact(name: String, emailError: Boolean = false, phoneError: Boolean = false) =
        ConversationPresenter.UiState.CollectingContact(
            participantIndex = 0,
            firstName = mutableStateOf(name),
            lastName = mutableStateOf(""),
            email = mutableStateOf(""),
            phone = mutableStateOf(""),
            notes = mutableStateOf(""),
            emailError = emailError,
            phoneError = phoneError,
            showExitDialog = false,
            eventSink = eventSink::invoke,
        )

    // ── fields bind to the state's MutableState properties ──────────────────

    @Test
    fun `UI - FirstName - renders the state's seeded first name`() = runComposeUiTest {
        setContent { ContactCollectionLayout(collectingContact(name = "Alice")) }

        onNode(hasSetTextAction() and hasText(getString(Res.string.contact_first_name)))
            .assertTextContains("Alice")
    }

    @Test
    fun `UI - Email - typing writes into the state`() = runComposeUiTest {
        val state = collectingContact(name = "Alice")
        setContent { ContactCollectionLayout(state) }

        onNode(hasSetTextAction() and hasText(getString(Res.string.contact_email_hint)))
            .performTextInput("alice@example.com")

        assertEquals("alice@example.com", state.email.value)
    }

    // ── validation state is reflected, not computed here ────────────────────

    @Test
    fun `UI - Email - error flag shows the inline message`() = runComposeUiTest {
        setContent { ContactCollectionLayout(collectingContact(name = "Alice", emailError = true)) }

        onNode(hasText(getString(Res.string.contact_invalid_email))).assertExists()
    }

    @Test
    fun `UI - Phone - error flag shows the inline message`() = runComposeUiTest {
        setContent { ContactCollectionLayout(collectingContact(name = "Alice", phoneError = true)) }

        onNode(hasText(getString(Res.string.contact_invalid_phone))).assertExists()
    }

    @Test
    fun `UI - Save - disabled while the first name is blank`() = runComposeUiTest {
        val state = collectingContact(name = "Alice")
        setContent { ContactCollectionLayout(state) }

        state.firstName.value = ""
        waitForIdle()

        onNode(hasText(getString(Res.string.action_save))).assertIsNotEnabled()
    }

    @Test
    fun `UI - Save - disabled while a field has an error`() = runComposeUiTest {
        setContent { ContactCollectionLayout(collectingContact(name = "Alice", emailError = true)) }

        onNode(hasText(getString(Res.string.action_save))).assertIsNotEnabled()
    }

    @Test
    fun `UI - Save - enabled with a name and no errors`() = runComposeUiTest {
        setContent { ContactCollectionLayout(collectingContact(name = "Alice")) }

        onNode(hasText(getString(Res.string.action_save))).assertIsEnabled()
    }

    @Test
    fun `UI - Save - emits Save`() = runComposeUiTest {
        setContent { ContactCollectionLayout(collectingContact(name = "Alice")) }

        onNode(hasText(getString(Res.string.action_save))).performClick()

        eventSink.assertEvent(ConversationPresenter.UiEvent.CollectingContact.Save)
    }
}
