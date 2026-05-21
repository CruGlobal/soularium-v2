package org.cru.soularium.domain.session

import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.SessionKind

sealed interface SessionEvent {
    data class StartSession(val kind: SessionKind) : SessionEvent

    data class AddParticipant(val name: String) : SessionEvent

    data class RemoveParticipant(val index: Int) : SessionEvent

    data object ConfirmParticipants : SessionEvent

    data object BeginSelection : SessionEvent

    data object DismissInstructions : SessionEvent

    data class PickCard(val cardId: Int) : SessionEvent

    data class UnpickCard(val cardId: Int) : SessionEvent

    data object ConfirmSelection : SessionEvent

    data object ConfirmFinal : SessionEvent

    data object EndDiscussion : SessionEvent

    data class CollectContact(val participantIndex: Int, val info: ContactInfo) : SessionEvent

    data object SkipContact : SessionEvent

    data object Conclude : SessionEvent

    data object Bookmark : SessionEvent
}
