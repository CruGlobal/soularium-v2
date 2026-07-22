package org.cru.soularium.model

import kotlin.test.Test
import kotlin.test.assertNotEquals

class SessionIdTest {
    @Test
    fun `random - produces a unique id each call`() {
        assertNotEquals(SessionId.random(), SessionId.random())
    }
}
