package org.cru.soularium.domain.ports

import kotlinx.coroutines.flow.Flow
import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.Conversation
import org.cru.soularium.domain.Session
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.model.ConversationId
import org.cru.soularium.model.SessionId

interface SessionRepository {
    suspend fun createSession(session: Session, initialState: SessionState): SessionId

    suspend fun loadSession(id: SessionId): Session?

    suspend fun loadState(id: SessionId): SessionState?

    suspend fun persistState(id: SessionId, state: SessionState)

    suspend fun setBookmarked(id: SessionId, bookmarked: Boolean)

    suspend fun setEnded(id: SessionId)

    suspend fun upsertParticipants(sessionId: SessionId, names: List<String>): List<ConversationId>

    suspend fun upsertContact(conversationId: ConversationId, info: ContactInfo)

    suspend fun upsertPicks(conversationId: ConversationId, questionNumber: Int, cardIds: List<Int>, isFinal: Boolean)

    suspend fun loadPicks(conversationId: ConversationId): List<CardPick>

    fun observeCompletedSessions(): Flow<List<Session>>

    fun observeBookmarkedSessions(): Flow<List<Session>>

    suspend fun deleteSession(id: SessionId)

    suspend fun loadConversations(sessionId: SessionId): List<Conversation>
}
