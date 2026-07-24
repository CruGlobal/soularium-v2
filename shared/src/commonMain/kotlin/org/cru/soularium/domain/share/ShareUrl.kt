package org.cru.soularium.domain.share

import org.cru.soularium.model.CardPick
import org.cru.soularium.model.Conversation

fun shareUrlFor(conversation: Conversation, picks: List<CardPick>): String {
    val finals = picks.filter { it.isFinal }
    val orderedCardIds =
        (1..5).flatMap { q ->
            finals.filter { it.questionNumber == q }
                .sortedBy { it.pickOrder }
                .map { it.cardId }
        }
    val name = urlEncode(conversation.contact.name)
    return "https://mysoularium.com/my-life-in-pictures/?images=${orderedCardIds.joinToString(",")}&person=$name"
}

private fun urlEncode(s: String): String = buildString {
    for (c in s) {
        when {
            c.isLetterOrDigit() || c in "-_.~" -> append(c)
            else -> {
                val bytes = c.toString().encodeToByteArray()
                for (b in bytes) {
                    append('%')
                    val byte = b.toInt() and 0xff
                    append((byte shr 4).toString(16).uppercase())
                    append((byte and 0x0f).toString(16).uppercase())
                }
            }
        }
    }
}
