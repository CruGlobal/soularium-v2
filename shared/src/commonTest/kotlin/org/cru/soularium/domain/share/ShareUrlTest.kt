package org.cru.soularium.domain.share

import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.CardPickId
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.Conversation
import org.cru.soularium.domain.ConversationId
import org.cru.soularium.domain.SessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShareUrlTest {
    private val conv =
        Conversation(
            id = ConversationId("c-1"),
            sessionId = SessionId("s-1"),
            displayOrder = 0,
            contact = ContactInfo(name = "John"),
        )

    private fun pick(q: Int, card: Int, order: Int, isFinal: Boolean = true) = CardPick(
        id = CardPickId("p-$q-$card"),
        conversationId = ConversationId("c-1"),
        questionNumber = q,
        cardId = card,
        pickOrder = order,
        isFinal = isFinal,
    )

    @Test
    fun `9 final picks ordered Q1 to Q5`() {
        val picks =
            listOf(
                pick(1, 5, 0), pick(1, 12, 1), pick(1, 33, 2),
                pick(2, 7, 0), pick(2, 18, 1), pick(2, 41, 2),
                pick(3, 22, 0),
                pick(4, 9, 0),
                pick(5, 50, 0),
            )
        val url = shareUrlFor(conv, picks)
        assertEquals(
            "https://mysoularium.com/my-life-in-pictures/?images=5,12,33,7,18,41,22,9,50&person=John",
            url,
        )
    }

    @Test
    fun `Names with spaces are URL-encoded`() {
        val picks =
            (1..5).flatMap { q ->
                val count = if (q <= 2) 3 else 1
                (1..count).map { pick(q, q * 10 + it, it - 1) }
            }
        val withSpace = conv.copy(contact = ContactInfo(name = "Mary Jane"))
        val url = shareUrlFor(withSpace, picks)
        assertEquals("Mary%20Jane", url.substringAfter("person="))
    }

    @Test
    fun `Non-final picks are excluded`() {
        val picks =
            listOf(
                pick(1, 5, 0), pick(1, 12, 1), pick(1, 33, 2),
                pick(2, 7, 0), pick(2, 18, 1), pick(2, 41, 2),
                pick(3, 22, 0),
                pick(4, 9, 0),
                pick(5, 50, 0),
                pick(1, 99, 99, isFinal = false),
            )
        val url = shareUrlFor(conv, picks)
        assertTrue("99" !in url, "non-final card 99 should not appear in $url")
    }

    @Test
    fun `pickOrder governs ordering within a question`() {
        val picks =
            listOf(
                pick(1, 33, 2), pick(1, 5, 0), pick(1, 12, 1),
                pick(2, 41, 2), pick(2, 7, 0), pick(2, 18, 1),
                pick(3, 22, 0),
                pick(4, 9, 0),
                pick(5, 50, 0),
            )
        val url = shareUrlFor(conv, picks)
        assertEquals(
            "https://mysoularium.com/my-life-in-pictures/?images=5,12,33,7,18,41,22,9,50&person=John",
            url,
        )
    }
}
