package org.cru.soularium.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single card selected during a conversation, either as a draft or a
 * confirmed pick. Every field pins its wire name via [SerialName] so a
 * Kotlin property rename cannot silently change the stored key and orphan
 * existing pick records.
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
