package org.cru.soularium.domain

import kotlinx.serialization.Serializable

@Serializable
data class CardPick(
    val id: CardPickId,
    val conversationId: ConversationId,
    val questionNumber: Int,
    val cardId: Int,
    val pickOrder: Int,
    val isFinal: Boolean,
)
