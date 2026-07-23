package org.cru.soularium.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SessionKind is stored on the `SessionEntity.kind` column today. These
 * tests exercise the JSON encoder/decoder and lock the wire values for
 * each variant so the format stays stable if this type is ever serialized
 * to JSON (share links, sync, export).
 */
class SessionKindTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `SessionKind SOLO roundtrips through JSON`() {
        val encoded = json.encodeToString(SessionKind.SOLO)
        val decoded = json.decodeFromString<SessionKind>(encoded)

        assertEquals(SessionKind.SOLO, decoded)
    }

    @Test
    fun `SessionKind GROUP roundtrips through JSON`() {
        val encoded = json.encodeToString(SessionKind.GROUP)
        val decoded = json.decodeFromString<SessionKind>(encoded)

        assertEquals(SessionKind.GROUP, decoded)
    }

    @Test
    fun `SessionKind JSON wire values match the persistence contract`() {
        // Enum variants serialize as JSON string primitives whose value is
        // the Kotlin constant name (unless overridden with @SerialName).
        // Locking these values means a Kotlin rename can't silently change
        // the wire format for any future JSON serialization.
        assertEquals("\"SOLO\"", json.encodeToString(SessionKind.SOLO))
        assertEquals("\"GROUP\"", json.encodeToString(SessionKind.GROUP))
    }

    @Test
    fun `all SessionKind variants round-trip`() {
        // Guard against a new variant being added later without a matching
        // wire-value test — if this enumeration count assertion fires, add a
        // round-trip case and a wire-value case for the new variant above.
        assertEquals(2, SessionKind.entries.size)
        SessionKind.entries.forEach { kind ->
            val encoded = json.encodeToString(kind)
            val decoded = json.decodeFromString<SessionKind>(encoded)
            assertTrue(kind == decoded, "SessionKind.$kind failed to round-trip: encoded=$encoded, decoded=$decoded")
        }
    }
}
