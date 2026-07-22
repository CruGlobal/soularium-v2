package org.cru.soularium.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ConversationId(val value: String) {
    companion object {
        fun random() = ConversationId(Uuid.random().toString())
    }
}
