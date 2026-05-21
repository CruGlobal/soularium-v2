package org.cru.soularium.domain

import kotlinx.serialization.Serializable
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.jvm.JvmInline
import kotlin.random.Random

@Serializable
@JvmInline
value class SessionId(val value: String) {
    companion object {
        fun random(): SessionId = SessionId(generateUuid())
        fun fromString(s: String): SessionId = SessionId(s)
    }
}

@Serializable
@JvmInline
value class ConversationId(val value: String) {
    companion object {
        fun random(): ConversationId = ConversationId(generateUuid())
        fun fromString(s: String): ConversationId = ConversationId(s)
    }
}

@Serializable
@JvmInline
value class CardPickId(val value: String) {
    companion object {
        fun random(): CardPickId = CardPickId(generateUuid())
    }
}

private fun generateUuid(): String {
    val bytes = ByteArray(16)
    Random.nextBytes(bytes)
    bytes[6] = (bytes[6] and 0x0f) or 0x40
    bytes[8] = (bytes[8] and 0x3f) or 0x80.toByte()
    val hex = bytes.joinToString("") { ((it.toInt() and 0xff) + 0x100).toString(16).substring(1) }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
}
