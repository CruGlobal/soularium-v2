package org.cru.soularium.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentRepositoryImplTest {
    @Test
    fun `provides the five Soularium questions`() {
        assertEquals(5, ContentRepositoryImpl().questions().size)
    }

    @Test
    fun `provides fifty cards`() {
        assertEquals(50, ContentRepositoryImpl().cards().size)
    }
}
