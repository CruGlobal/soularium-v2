package org.cru.soularium.domain

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: ConversationId,
    val sessionId: SessionId,
    val displayOrder: Int,
    val contact: ContactInfo,
)
