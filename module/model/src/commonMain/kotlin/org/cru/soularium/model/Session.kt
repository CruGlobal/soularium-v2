package org.cru.soularium.model

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: SessionId = SessionId.random(),
    val kind: Kind = Kind.SOLO,
    val startedAt: Instant = Clock.System.now(),
    val endedAt: Instant? = null,
    val bookmarkedAt: Instant? = null,
    val selectionInstructionsShown: Boolean = false,
) {
    @Serializable
    enum class Kind { SOLO, GROUP }
}
