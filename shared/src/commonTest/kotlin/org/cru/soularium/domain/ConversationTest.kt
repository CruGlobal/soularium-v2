package org.cru.soularium.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Conversation embeds ContactInfo and is persisted via `ConversationEntity`
 * Room columns today. These tests exercise the JSON encoder/decoder
 * (including the ContactInfo composition) and lock the wire key names so
 * the format stays stable if this type is ever serialized to JSON (share
 * links, sync, export).
 */
class ConversationTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `Conversation with fully-populated contact roundtrips through JSON`() {
        val original = Conversation(
            id = ConversationId.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
            sessionId = SessionId.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
            displayOrder = 0,
            contact = ContactInfo(
                name = "Ada",
                surname = "Lovelace",
                email = "ada@example.com",
                phone = "+1 555 0100",
                notes = "Loves analytical engines.",
            ),
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Conversation>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `Conversation with name-only contact roundtrips through JSON`() {
        val original = Conversation(
            id = ConversationId.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
            sessionId = SessionId.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
            displayOrder = 2,
            contact = ContactInfo(name = "Solo"),
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Conversation>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `JSON wire keys match the persistence contract`() {
        val encoded = json.encodeToString(
            Conversation(
                id = ConversationId.fromString("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"),
                sessionId = SessionId.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff"),
                displayOrder = 1,
                contact = ContactInfo(name = "Grace"),
            ),
        )

        // Locks the JSON wire keys for Conversation. A Kotlin field rename
        // without an explicit @SerialName override would change the
        // auto-derived wire key and fail this assertion — protecting the
        // wire format for any future JSON serialization.
        listOf("\"id\"", "\"sessionId\"", "\"displayOrder\"", "\"contact\"").forEach { key ->
            assertTrue(
                encoded.contains(key),
                "Expected serialized Conversation to contain wire key $key, got: $encoded",
            )
        }
    }
}
