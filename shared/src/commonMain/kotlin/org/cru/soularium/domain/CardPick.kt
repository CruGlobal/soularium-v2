package org.cru.soularium.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single card selected during a conversation, either as a draft or a
 * confirmed pick. Persisted via `CardPickEntity` Room columns today; the
 * [SerialName] annotations lock each field's JSON wire key so the format
 * stays stable if this type is ever serialized to JSON (share links, sync,
 * export).
 */
@Serializable
data class CardPick(
    @SerialName("id") val id: CardPickId,
    @SerialName("conversationId") val conversationId: ConversationId,
    @SerialName("questionNumber") val questionNumber: Int,
    @SerialName("cardId") val cardId: Int,
    @SerialName("pickOrder") val pickOrder: Int,
    @SerialName("isFinal") val isFinal: Boolean,
)
