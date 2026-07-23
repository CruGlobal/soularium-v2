package org.cru.soularium.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Whether a session is being played by one participant or a group. Stored
 * on the `SessionEntity.kind` column today; the [SerialName] annotations
 * lock each variant's JSON wire value so the format stays stable if this
 * type is ever serialized to JSON (share links, sync, export).
 */
@Serializable
enum class SessionKind {
    @SerialName("SOLO")
    SOLO,

    @SerialName("GROUP")
    GROUP,
}
