package org.cru.soularium.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single participant's slot within a session. Persisted via
 * `ConversationEntity` Room columns today; the [SerialName] annotations
 * lock each field's JSON wire key so the format stays stable if this type
 * is ever serialized to JSON (share links, sync, export).
 */
@Serializable
data class Conversation(
    @SerialName("id") val id: ConversationId,
    @SerialName("sessionId") val sessionId: SessionId,
    @SerialName("displayOrder") val displayOrder: Int,
    @SerialName("contact") val contact: ContactInfo,
)
