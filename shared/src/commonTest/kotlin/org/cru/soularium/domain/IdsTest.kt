package org.cru.soularium.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IdsTest {
    @Test
    fun `SessionId roundtrips through string`() {
        val id = SessionId.random()
        assertEquals(id, SessionId.fromString(id.value))
    }

    @Test
    fun `Two random SessionIds differ`() {
        assertNotEquals(SessionId.random(), SessionId.random())
    }

    @Test
    fun `Generated id has uuid v4 shape`() {
        val id = SessionId.random().value
        val regex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        assertTrue(regex.matches(id), "expected uuid-v4 shape, got $id")
    }
}
