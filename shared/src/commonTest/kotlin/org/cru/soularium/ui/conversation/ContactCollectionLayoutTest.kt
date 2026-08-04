package org.cru.soularium.ui.conversation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_save
import org.cru.soularium.generated.resources.contact_email_hint
import org.cru.soularium.generated.resources.contact_first_name
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class ContactCollectionLayoutTest {
    private val eventSink = TestEventSink<ConversationPresenter.UiEvent>()

    private fun collectingContact(name: String) = ConversationPresenter.UiState.CollectingContact(
        participantIndex = 0,
        firstName = mutableStateOf(name),
        lastName = mutableStateOf(""),
        email = mutableStateOf(""),
        phone = mutableStateOf(""),
        notes = mutableStateOf(""),
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

    @Test
    fun `UI - Save - emits Save`() = runComposeUiTest {
        setContent { ContactCollectionLayout(collectingContact(name = "Alice")) }

        onNode(hasText(getString(Res.string.action_save))).performClick()

        eventSink.assertEvent(ConversationPresenter.UiEvent.CollectingContact.Save)
    }

    // ── blank / empty → always valid (optional field) ──────────────────────

    @Test
    fun `empty string is valid`() {
        assertTrue(isPhoneValid(""))
    }

    @Test
    fun `whitespace-only string is valid`() {
        assertTrue(isPhoneValid("   "))
    }

    // ── standard valid numbers ──────────────────────────────────────────────

    @Test
    fun `7-digit local number is valid`() {
        assertTrue(isPhoneValid("5551234"))
    }

    @Test
    fun `10-digit US number is valid`() {
        assertTrue(isPhoneValid("4085551234"))
    }

    @Test
    fun `formatted US number with dashes is valid`() {
        assertTrue(isPhoneValid("408-555-1234"))
    }

    @Test
    fun `formatted US number with parens and spaces is valid`() {
        assertTrue(isPhoneValid("(408) 555-1234"))
    }

    @Test
    fun `E164 international number with plus is valid`() {
        assertTrue(isPhoneValid("+14085551234"))
    }

    @Test
    fun `15-digit number is valid`() {
        assertTrue(isPhoneValid("123456789012345"))
    }

    // ── too short → invalid ─────────────────────────────────────────────────

    @Test
    fun `6-digit number is invalid`() {
        assertFalse(isPhoneValid("123456"))
    }

    @Test
    fun `4-digit partial number is invalid`() {
        assertFalse(isPhoneValid("1234"))
    }

    // ── too long → invalid ──────────────────────────────────────────────────

    @Test
    fun `16-digit number is invalid`() {
        assertFalse(isPhoneValid("1234567890123456"))
    }

    // ── non-digit content stripped before counting ──────────────────────────

    @Test
    fun `dots and spaces are stripped before validation`() {
        // +1 (408) 555-1234 → 11 digits → valid
        assertTrue(isPhoneValid("+1 (408) 555-1234"))
    }

    @Test
    fun `number with spaces that resolves to 6 digits is invalid`() {
        assertFalse(isPhoneValid("1 2 3 4 5 6"))
    }
}
