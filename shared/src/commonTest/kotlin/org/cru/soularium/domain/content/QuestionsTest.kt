package org.cru.soularium.domain.content

import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionsTest {
    @Test
    fun `All five questions are defined`() {
        assertEquals(5, Questions.all.size)
        assertEquals(listOf(1, 2, 3, 4, 5), Questions.all.map { it.number })
    }

    @Test
    fun `Q1 and Q2 require 3 picks`() {
        listOf(1, 2).forEach { n ->
            assertEquals(3, Questions.byNumber(n).requiredImageCount)
        }
    }

    @Test
    fun `Q3 Q4 Q5 require 1 pick`() {
        listOf(3, 4, 5).forEach { n ->
            assertEquals(1, Questions.byNumber(n).requiredImageCount)
        }
    }

    @Test
    fun `Final pick count across all questions is 9`() {
        assertEquals(9, Questions.all.sumOf { it.requiredImageCount })
    }
}
