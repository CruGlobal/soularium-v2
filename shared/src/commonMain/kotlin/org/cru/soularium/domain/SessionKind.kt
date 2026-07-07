package org.cru.soularium.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Whether a session is being played by one participant or a group. Each
 * variant pins its wire value via [SerialName] so renaming a Kotlin constant
 * cannot silently change the stored representation and orphan existing
 * sessions.
 */
@Serializable
enum class SessionKind {
    @SerialName("SOLO")
    SOLO,

    @SerialName("GROUP")
    GROUP,
}
