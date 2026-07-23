package org.cru.soularium.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ContactInfo is embedded in the persisted session-state JSON snapshot, so its
 * wire format must be stable across Kotlin renames. These tests pin the JSON
 * key names via @SerialName; if anyone renames a field without adjusting the
 * annotation, the "wire keys match expected names" assertion will fail loudly.
 */
class ContactInfoTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `Serialization - round-trips when fully populated`() {
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
    fun `Serialization - round-trips with only the required name`() {
        val original = ContactInfo(name = "Ada")

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<ContactInfo>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `Serialization - JSON keys match the persistence contract`() {
        val encoded = json.encodeToString(
            ContactInfo(
                name = "Ada",
                surname = "Lovelace",
                email = "ada@example.com",
                phone = "+1 555 0100",
                notes = "note",
            ),
        )

        // These are the exact keys already written to persisted sessions.
        // If anyone renames a Kotlin field without updating @SerialName, these
        // assertions catch it before it ships and orphans stored sessions.
        listOf("\"name\"", "\"surname\"", "\"email\"", "\"phone\"", "\"notes\"").forEach { key ->
            assertTrue(
                encoded.contains(key),
                "Expected serialized ContactInfo to contain wire key $key, got: $encoded",
            )
        }
    }
}
