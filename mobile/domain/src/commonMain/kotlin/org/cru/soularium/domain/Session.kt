package org.cru.soularium.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: SessionId,
    val kind: SessionKind,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val bookmarkedAt: Instant? = null,
    val selectionInstructionsShown: Boolean = false,
)
