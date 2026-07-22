package org.cru.soularium.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class CardPickId(val value: String) {
    companion object {
        fun random() = CardPickId(Uuid.random().toString())
    }
}
