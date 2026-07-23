package org.cru.soularium.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ContactInfo is persisted alongside the enclosing conversation via
 * `ConversationEntity` Room columns today. These tests exercise the JSON
 * encoder/decoder and lock the wire key names so the format stays stable
 * if this type is ever serialized to JSON (share links, sync, export).
 */
class ContactInfoTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `fully-populated ContactInfo roundtrips through JSON`() {
        val original = ContactInfo(
            name = "Ada",
            surname = "Lovelace",
            email = "ada@example.com",
            phone = "+1 555 0100",
            notes = "Loves analytical engines.",
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ContactInfo>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `ContactInfo with only required name roundtrips through JSON`() {
        val original = ContactInfo(name = "Ada")

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ContactInfo>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `JSON wire keys match the persistence contract`() {
        val encoded = json.encodeToString(
            ContactInfo(
                name = "Ada",
                surname = "Lovelace",
                email = "ada@example.com",
                phone = "+1 555 0100",
                notes = "note",
            ),
        )

        // Locks the JSON wire keys for ContactInfo. A Kotlin field rename
        // without an explicit @SerialName override would change the
        // auto-derived wire key and fail this assertion — protecting the
        // wire format for any future JSON serialization.
        listOf("\"name\"", "\"surname\"", "\"email\"", "\"phone\"", "\"notes\"").forEach { key ->
            assertTrue(
                encoded.contains(key),
                "Expected serialized ContactInfo to contain wire key $key, got: $encoded",
            )
        }
    }
}
