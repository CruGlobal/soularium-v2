package org.cru.soularium.domain

import kotlinx.serialization.Serializable
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.ConversationId
import org.cru.soularium.model.Session

@Serializable
data class Conversation(
    val id: ConversationId,
    val sessionId: Session.Id,
    val displayOrder: Int,
    val contact: ContactInfo,
)
