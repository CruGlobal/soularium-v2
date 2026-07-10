package org.cru.soularium.ui.content

import kotlin.test.Test
import kotlin.test.assertEquals

class CardAssetTest {
    @Test
    fun `card ids are unique`() {
        val ids = CardAsset.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "CardAsset ids must be unique")
    }

    @Test
    fun `fromId resolves every card asset by its id`() {
        CardAsset.entries.forEach { card ->
            assertEquals(card, CardAsset.fromId(card.id))
        }
    }
}
