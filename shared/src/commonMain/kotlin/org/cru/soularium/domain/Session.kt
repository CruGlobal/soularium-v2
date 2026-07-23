package org.cru.soularium.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Session record. Persisted via `SessionEntity` Room columns today; the
 * [SerialName] annotations lock each field's JSON wire key so the format
 * stays stable if this type is ever serialized to JSON (share links, sync,
 * export).
 */
@Serializable
data class Session(
    @SerialName("id") val id: SessionId,
    @SerialName("kind") val kind: SessionKind,
    @SerialName("startedAt") val startedAt: Instant,
    @SerialName("endedAt") val endedAt: Instant? = null,
    @SerialName("bookmarkedAt") val bookmarkedAt: Instant? = null,
    @SerialName("selectionInstructionsShown") val selectionInstructionsShown: Boolean = false,
)

/** Creates a fresh session that starts now. */
fun newSession(id: SessionId, kind: SessionKind): Session =
    Session(id = id, kind = kind, startedAt = Clock.System.now())
