package org.cru.soularium.game.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuestionTest {
    @Test
    fun `five questions numbered 1 through 5 in entry order`() {
        assertEquals(5, Question.entries.size)
        assertEquals(listOf(1, 2, 3, 4, 5), Question.entries.map { it.number })
    }

    @Test
    fun `CURRENT_LIFE and DESIRED_LIFE require 3 picks`() {
        listOf(Question.CURRENT_LIFE, Question.DESIRED_LIFE).forEach {
            assertEquals(3, it.requiredImageCount)
        }
    }

    @Test
    fun `GOD and SPIRITUAL_EXPERIENCE and DESIRED_SPIRITUAL_LIFE require 1 pick`() {
        listOf(Question.GOD, Question.SPIRITUAL_EXPERIENCE, Question.DESIRED_SPIRITUAL_LIFE).forEach {
            assertEquals(1, it.requiredImageCount)
        }
    }

    @Test
    fun `final pick count across all questions is 9`() {
        assertEquals(9, Question.entries.sumOf { it.requiredImageCount })
    }

    @Test
    fun `forNumber round-trips every entry`() {
        Question.entries.forEach { assertEquals(it, Question.forNumber(it.number)) }
    }

    @Test
    fun `forNumber throws for out-of-range numbers`() {
        assertFailsWith<NoSuchElementException> { Question.forNumber(0) }
        assertFailsWith<NoSuchElementException> { Question.forNumber(6) }
    }
}
