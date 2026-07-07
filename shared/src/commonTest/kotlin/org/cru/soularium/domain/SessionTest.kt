package org.cru.soularium.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Session participates in the persistence layer (Room entities and session-state
 * JSON snapshots), so its wire format must stay stable across Kotlin renames.
 * The round-trip tests assert the encoder/decoder are symmetrical; the wire-key
 * test locks the JSON key names so a Kotlin field rename can't silently change
 * the persisted format.
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

        // These are the exact keys embedded in persisted sessions. Renaming a
        // Kotlin field on Session without updating the auto-derived wire key
        // (or without an explicit @SerialName override) would fail this
        // assertion before it shipped and orphaned stored sessions.
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
