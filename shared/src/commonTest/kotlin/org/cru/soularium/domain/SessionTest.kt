package org.cru.soularium.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Session is persisted via `SessionEntity` Room columns today. These tests
 * exercise the JSON encoder/decoder and lock the wire key names so the
 * format stays stable if this type is ever serialized to JSON (share links,
 * sync, export).
 */
class SessionTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `fully-populated Session roundtrips through JSON`() {
        val original = Session(
            id = SessionId.fromString("11111111-1111-4111-8111-111111111111"),
            kind = SessionKind.GROUP,
            startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            endedAt = Instant.fromEpochMilliseconds(1_700_000_500_000L),
            bookmarkedAt = Instant.fromEpochMilliseconds(1_700_000_250_000L),
            selectionInstructionsShown = true,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Session>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `Session with only required fields roundtrips through JSON`() {
        val original = Session(
            id = SessionId.fromString("22222222-2222-4222-8222-222222222222"),
            kind = SessionKind.SOLO,
            startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Session>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `JSON wire keys match the persistence contract`() {
        val encoded = json.encodeToString(
            Session(
                id = SessionId.fromString("33333333-3333-4333-8333-333333333333"),
                kind = SessionKind.SOLO,
                startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
                endedAt = Instant.fromEpochMilliseconds(1_700_000_500_000L),
                bookmarkedAt = Instant.fromEpochMilliseconds(1_700_000_250_000L),
                selectionInstructionsShown = true,
            ),
        )

        // Locks the JSON wire keys for Session. A Kotlin field rename without
        // an explicit @SerialName override would change the auto-derived wire
        // key and fail this assertion — protecting the wire format for any
        // future JSON serialization.
        listOf(
            "\"id\"",
            "\"kind\"",
            "\"startedAt\"",
            "\"endedAt\"",
            "\"bookmarkedAt\"",
            "\"selectionInstructionsShown\"",
        ).forEach { key ->
            assertTrue(
                encoded.contains(key),
                "Expected serialized Session to contain wire key $key, got: $encoded",
            )
        }
    }
}
