package org.cru.soularium.domain.ports

import kotlinx.coroutines.flow.Flow
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.model.CardPick
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session

interface SessionRepository {
    suspend fun createSession(session: Session, initialState: SessionState): Session.Id

    suspend fun loadSession(id: Session.Id): Session?

    suspend fun loadState(id: Session.Id): SessionState?

    suspend fun persistState(id: Session.Id, state: SessionState)

    suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean)

    suspend fun setEnded(id: Session.Id)

    suspend fun upsertParticipants(sessionId: Session.Id, names: List<String>): List<Conversation.Id>

    suspend fun upsertContact(conversationId: Conversation.Id, info: ContactInfo)

    suspend fun upsertPicks(conversationId: Conversation.Id, questionNumber: Int, cardIds: List<Int>, isFinal: Boolean)

    suspend fun loadPicks(conversationId: Conversation.Id): List<CardPick>

    fun observeCompletedSessions(): Flow<List<Session>>

    fun observeBookmarkedSessions(): Flow<List<Session>>

    suspend fun deleteSession(id: Session.Id)

    suspend fun loadConversations(sessionId: Session.Id): List<Conversation>
}
