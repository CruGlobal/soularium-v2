package org.cru.soularium.model

import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: Id = Id.random(),
    val kind: Kind = Kind.SOLO,
    val startedAt: Instant = Clock.System.now(),
    val endedAt: Instant? = null,
    val bookmarkedAt: Instant? = null,
    val selectionInstructionsShown: Boolean = false,
) {
    @Serializable
    @JvmInline
    value class Id(val value: String) {
        companion object {
            fun random() = Id(Uuid.random().toString())
        }
    }

    @Serializable
    enum class Kind { SOLO, GROUP }
}
