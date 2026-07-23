package org.cru.soularium.domain.session

import org.cru.soularium.model.ContactInfo

sealed interface Effect {
    data class PersistState(val state: SessionState) : Effect

    data class PersistParticipants(val names: List<String>) : Effect

    data class PersistPicks(
        val questionNumber: Int,
        val participantIndex: Int,
        val cardIds: List<Int>,
        val isFinal: Boolean,
    ) : Effect

    data class PersistContact(val participantIndex: Int, val info: ContactInfo) : Effect

    data class LogAnalytics(val event: String, val params: Map<String, Any>) : Effect
}
