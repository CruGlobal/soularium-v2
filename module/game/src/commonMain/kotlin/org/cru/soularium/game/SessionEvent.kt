package org.cru.soularium.game

import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Session

sealed interface SessionEvent {
    data class StartSession(val kind: Session.Kind) : SessionEvent

    data class AddParticipant(val name: String) : SessionEvent

    data class RemoveParticipant(val index: Int) : SessionEvent

    data object ConfirmParticipants : SessionEvent

    data object BeginSelection : SessionEvent

    data object DismissInstructions : SessionEvent

    data object ConfirmSelection : SessionEvent

    data class ToggleCard(val cardId: Int) : SessionEvent

    data object ConfirmFinal : SessionEvent

    data object EndDiscussion : SessionEvent

    data class CollectContact(val participantIndex: Int, val info: ContactInfo) : SessionEvent

    data class SkipContact(val participantIndex: Int) : SessionEvent

    data object Conclude : SessionEvent
}
