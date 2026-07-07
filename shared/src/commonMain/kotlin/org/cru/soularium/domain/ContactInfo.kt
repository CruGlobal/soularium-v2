package org.cru.soularium.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Optional follow-up contact captured for a conversation participant. Persisted
 * as part of the session-state JSON snapshot (see [SessionState] persistence in
 * [org.cru.soularium.data.repository.SessionRepositoryImpl]), so every field
 * pins its wire name via [SerialName] — renaming a Kotlin property must not
 * silently change the stored key and orphan existing sessions.
 */
@Serializable
data class ContactInfo(
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("notes") val notes: String? = null,
)
