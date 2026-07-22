package org.cru.soularium.domain

import kotlinx.serialization.Serializable
import org.cru.soularium.model.ConversationId
import org.cru.soularium.model.SessionId

@Serializable
data class Conversation(
    val id: ConversationId,
    val sessionId: SessionId,
    val displayOrder: Int,
    val contact: ContactInfo,
)
