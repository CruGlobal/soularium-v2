package org.cru.soularium.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single participant's slot within a session. Every field pins its wire
 * name via [SerialName] so a Kotlin property rename cannot silently change
 * the stored key and orphan existing conversations.
 */
@Serializable
data class Conversation(
    @SerialName("id") val id: ConversationId,
    @SerialName("sessionId") val sessionId: SessionId,
    @SerialName("displayOrder") val displayOrder: Int,
    @SerialName("contact") val contact: ContactInfo,
)
