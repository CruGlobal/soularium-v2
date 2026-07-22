package org.cru.soularium.model

import kotlin.test.Test
import kotlin.test.assertNotEquals

class CardPickIdTest {
    @Test
    fun `random - produces a unique id each call`() {
        assertNotEquals(CardPickId.random(), CardPickId.random())
    }
}
