package org.cru.soularium.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CardPickTest {
    @Test
    fun `Id - random - produces a unique id each call`() {
        assertNotEquals(CardPick.Id.random(), CardPick.Id.random())
    }

    @Test
    fun `Id - Serialization - encodes as a bare JSON string`() {
        assertEquals("\"cp-1\"", Json.encodeToString(CardPick.Id("cp-1")))
    }

    @Test
    fun `Serialization - round-trips through JSON`() {
        val original = CardPick(
            id = CardPick.Id("cp-1"),
            conversationId = Conversation.Id("c-1"),
            questionNumber = 3,
            cardId = 42,
            pickOrder = 1,
            isFinal = true,
        )
        assertEquals(original, Json.decodeFromString<CardPick>(Json.encodeToString(original)))
    }
}
