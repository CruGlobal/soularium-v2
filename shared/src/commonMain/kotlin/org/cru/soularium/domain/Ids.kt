package org.cru.soularium.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class SessionId(val value: String) {
    companion object {
        fun random(): SessionId = SessionId(Uuid.random().toString())

        fun fromString(s: String): SessionId = SessionId(s)
    }
}

@Serializable
@JvmInline
value class ConversationId(val value: String) {
    companion object {
        fun random(): ConversationId = ConversationId(Uuid.random().toString())

        fun fromString(s: String): ConversationId = ConversationId(s)
    }
}

@Serializable
@JvmInline
value class CardPickId(val value: String) {
    companion object {
        fun random(): CardPickId = CardPickId(Uuid.random().toString())
    }
}
