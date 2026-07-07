package org.cru.soularium.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * CardPick records a single card selection during a conversation and is
 * persisted per participant. The round-trip test asserts the encoder/decoder
 * are symmetrical; the wire-key test locks the JSON key names so a Kotlin
 * field rename can't silently change the stored format.
 */
class CardPickTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `final CardPick roundtrips through JSON`() {
        val original = CardPick(
            id = CardPickId("11111111-1111-4111-8111-111111111111"),
            conversationId = ConversationId.fromString("22222222-2222-4222-8222-222222222222"),
            questionNumber = 3,
            cardId = 42,
            pickOrder = 1,
            isFinal = true,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CardPick>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `draft CardPick roundtrips through JSON`() {
        val original = CardPick(
            id = CardPickId("33333333-3333-4333-8333-333333333333"),
            conversationId = ConversationId.fromString("44444444-4444-4444-8444-444444444444"),
            questionNumber = 1,
            cardId = 7,
            pickOrder = 0,
            isFinal = false,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CardPick>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `JSON wire keys match the persistence contract`() {
        val encoded = json.encodeToString(
            CardPick(
                id = CardPickId("55555555-5555-4555-8555-555555555555"),
                conversationId = ConversationId.fromString("66666666-6666-4666-8666-666666666666"),
                questionNumber = 2,
                cardId = 15,
                pickOrder = 0,
                isFinal = true,
            ),
        )

        // Renaming a Kotlin field on CardPick without an explicit @SerialName
        // override would change the auto-derived wire key and fail this
        // assertion, preventing silent orphaning of stored data.
        listOf(
            "\"id\"",
            "\"conversationId\"",
            "\"questionNumber\"",
            "\"cardId\"",
            "\"pickOrder\"",
            "\"isFinal\"",
        ).forEach { key ->
            assertTrue(
                encoded.contains(key),
                "Expected serialized CardPick to contain wire key $key, got: $encoded",
            )
        }
    }
}
