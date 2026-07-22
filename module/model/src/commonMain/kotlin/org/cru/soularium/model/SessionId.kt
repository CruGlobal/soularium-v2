package org.cru.soularium.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class SessionId(val value: String) {
    companion object {
        fun random() = SessionId(Uuid.random().toString())
    }
}
