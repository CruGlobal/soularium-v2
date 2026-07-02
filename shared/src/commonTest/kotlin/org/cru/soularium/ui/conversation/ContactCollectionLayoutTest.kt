package org.cru.soularium.ui.conversation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactCollectionLayoutTest {

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
