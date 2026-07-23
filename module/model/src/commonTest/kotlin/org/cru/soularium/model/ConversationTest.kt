package org.cru.soularium.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConversationTest {
    @Test
    fun `Id - random - produces a unique id each call`() {
        assertNotEquals(Conversation.Id.random(), Conversation.Id.random())
    }

    @Test
    fun `Id - Serialization - encodes as a bare JSON string`() {
        assertEquals("\"c-1\"", Json.encodeToString(Conversation.Id("c-1")))
    }

    @Test
    fun `Serialization - round-trips through JSON`() {
        val original = Conversation(
            id = Conversation.Id("c-1"),
            sessionId = Session.Id("s-1"),
            displayOrder = 2,
            contact = ContactInfo(name = "Ada", email = "ada@example.com"),
        )
        assertEquals(original, Json.decodeFromString<Conversation>(Json.encodeToString(original)))
    }
}
