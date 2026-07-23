package org.cru.soularium.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(val id: Id, val sessionId: Session.Id, val displayOrder: Int, val contact: ContactInfo,) {
    @Serializable
    @JvmInline
    value class Id(val value: String) {
        companion object {
            fun random() = Id(Uuid.random().toString())
        }
    }
}
