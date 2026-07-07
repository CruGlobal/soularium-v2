package org.cru.soularium.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Persisted session record. Every field pins its wire name via [SerialName]
 * so a Kotlin property rename cannot silently change the stored key and
 * orphan existing sessions on users' phones.
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
