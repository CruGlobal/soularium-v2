package org.cru.soularium.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class CardPick(
    val id: Id,
    val conversationId: Conversation.Id,
    val questionNumber: Int,
    val cardId: Int,
    val pickOrder: Int,
    val isFinal: Boolean,
) {
    @Serializable
    @JvmInline
    value class Id(val value: String) {
        companion object {
            fun random() = Id(Uuid.random().toString())
        }
    }
}
