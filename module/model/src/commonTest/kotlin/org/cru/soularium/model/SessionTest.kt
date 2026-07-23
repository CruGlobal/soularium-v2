package org.cru.soularium.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionTest {
    @Test
    fun `Id - random - produces a unique id each call`() {
        assertNotEquals(Session.Id.random(), Session.Id.random())
    }

    @Test
    fun `Id - Serialization - encodes as a bare JSON string`() {
        assertEquals("\"s-1\"", Json.encodeToString(Session.Id("s-1")))
    }

    @Test
    fun `Serialization - round-trips through JSON`() {
        val original = Session(
            id = Session.Id("s-1"),
            kind = Session.Kind.GROUP,
            startedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
            endedAt = Instant.fromEpochMilliseconds(1_700_000_100_000),
            bookmarkedAt = Instant.fromEpochMilliseconds(1_700_000_050_000),
            selectionInstructionsShown = true,
        )
        assertEquals(original, Json.decodeFromString<Session>(Json.encodeToString(original)))
    }
}
