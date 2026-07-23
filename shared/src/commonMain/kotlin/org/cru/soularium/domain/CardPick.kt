package org.cru.soularium.domain

import kotlinx.serialization.Serializable
import org.cru.soularium.model.CardPickId
import org.cru.soularium.model.Conversation

@Serializable
data class CardPick(
    val id: CardPickId,
    val conversationId: Conversation.Id,
    val questionNumber: Int,
    val cardId: Int,
    val pickOrder: Int,
    val isFinal: Boolean,
)
