package org.cru.soularium.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Optional follow-up contact captured for a conversation participant.
 * Persisted alongside the enclosing conversation via `ConversationEntity`
 * Room columns today; the [SerialName] annotations lock each field's JSON
 * wire key so the format stays stable if this type is ever serialized to
 * JSON (share links, sync, export).
 */
@Serializable
data class ContactInfo(
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("notes") val notes: String? = null,
)
