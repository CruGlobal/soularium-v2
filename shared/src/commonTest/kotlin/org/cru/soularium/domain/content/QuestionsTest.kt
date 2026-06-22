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
    fun `Q1 and Q2 require 3 final picks across 2 rounds`() {
        listOf(1, 2).forEach { n ->
            val q = Questions.byNumber(n)
            assertEquals(3, q.requiredImageCount)
            assertEquals(2, q.selectionRounds)
        }
    }

    @Test
    fun `Q3 Q4 Q5 require 1 pick in 1 round`() {
        listOf(3, 4, 5).forEach { n ->
            val q = Questions.byNumber(n)
            assertEquals(1, q.requiredImageCount)
            assertEquals(1, q.selectionRounds)
        }
    }

    @Test
    fun `Final pick count across all questions is 9`() {
        assertEquals(9, Questions.all.sumOf { it.requiredImageCount })
    }
}
