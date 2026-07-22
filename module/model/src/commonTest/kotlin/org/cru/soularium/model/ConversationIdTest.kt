package org.cru.soularium.model

import kotlin.test.Test
import kotlin.test.assertNotEquals

class ConversationIdTest {
    @Test
    fun `random - produces a unique id each call`() {
        assertNotEquals(ConversationId.random(), ConversationId.random())
    }
}
