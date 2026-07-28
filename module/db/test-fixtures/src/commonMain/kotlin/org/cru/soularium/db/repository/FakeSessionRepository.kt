package org.cru.soularium.db.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.cru.soularium.model.CardPick
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

/**
 * In-memory [SessionRepository] test fixture that fully persists sessions,
 * state, conversations, and picks. Data is held in [MutableStateFlow]s, so the
 * returned flows re-emit on every change, like the Room-backed repository's
 * observable queries. Completed/bookmarked status is tracked with id sets
 * rather than by mutating the [Session] timestamp fields. Mutating calls are
 * recorded for assertions, and error paths can be exercised via the
 * fault-injection properties.
 */
class FakeSessionRepository : SessionRepository {
    private val sessions = MutableStateFlow(emptyMap<Session.Id, Session>())
    private val states = mutableMapOf<Session.Id, SessionState>()
    private val conversations = MutableStateFlow(emptyMap<Session.Id, List<Conversation>>())
    private val picks = MutableStateFlow(emptyMap<Conversation.Id, List<CardPick>>())
    private val completedIds = MutableStateFlow(emptySet<Session.Id>())
    private val bookmarkedIds = MutableStateFlow(emptySet<Session.Id>())

    // Fault injection
    var findSessionStateError: Throwable? = null
    var observeConversationsError: Throwable? = null
    var observeConversationsOverride: Flow<List<Conversation>>? = null

    // Recorded interactions
    val persistedStates = mutableListOf<Pair<Session.Id, SessionState>>()
    val deletedSessions = mutableListOf<Session.Id>()
    var lastUpsertedParticipants: List<String>? = null

    fun seedSession(session: Session, state: SessionState? = null) {
        sessions.update { it + (session.id to session) }
        state?.let { states[session.id] = it }
    }

    fun seedState(id: Session.Id, state: SessionState) {
        states[id] = state
    }

    fun seedConversations(sessionId: Session.Id, seed: List<Conversation>) {
        conversations.update { it + (sessionId to seed) }
    }

    fun seedPicks(conversationId: Conversation.Id, seed: List<CardPick>) {
        picks.update { it + (conversationId to seed) }
    }

    fun bookmarkedSnapshot(): List<Session> =
        bookmarkedIds.value.filterNot { it in completedIds.value }.mapNotNull { sessions.value[it] }

    override suspend fun createSession(session: Session, initialState: SessionState): Session.Id {
        sessions.update { it + (session.id to session) }
        states[session.id] = initialState
        return session.id
    }

    override suspend fun findSession(id: Session.Id): Session? = sessions.value[id]
    override fun findSessionFlow(id: Session.Id): Flow<Session?> = sessions.map { it[id] }

    override suspend fun findSessionState(id: Session.Id): SessionState? {
        findSessionStateError?.let { throw it }
        return states[id]
    }

    override suspend fun persistState(id: Session.Id, state: SessionState) {
        persistedStates += id to state
        states[id] = state
        if (state == SessionState.Concluded) {
            completedIds.update { it + id }
            bookmarkedIds.update { it - id }
        }
    }

    override suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean) {
        bookmarkedIds.update { if (bookmarked) it + id else it - id }
    }

    override suspend fun setEnded(id: Session.Id) {
        completedIds.update { it + id }
    }

    override suspend fun upsertParticipants(sessionId: Session.Id, names: List<String>): List<Conversation.Id> {
        lastUpsertedParticipants = names
        val existing = conversations.value[sessionId].orEmpty()
        val list = names.mapIndexed { idx, name ->
            Conversation(
                id = existing.getOrNull(idx)?.id ?: Conversation.Id.random(),
                sessionId = sessionId,
                displayOrder = idx,
                contact = ContactInfo(name),
            )
        }
        conversations.update { it + (sessionId to list) }
        return list.map { it.id }
    }

    override suspend fun upsertContact(conversationId: Conversation.Id, info: ContactInfo) {
        conversations.update { all ->
            all.mapValues { (_, list) ->
                list.map { if (it.id == conversationId) it.copy(contact = info) else it }
            }
        }
    }

    override suspend fun upsertPicks(
        conversationId: Conversation.Id,
        questionNumber: Int,
        cardIds: List<Int>,
        isFinal: Boolean,
    ) {
        picks.update { all ->
            val bucket = all[conversationId].orEmpty().filterNot { it.questionNumber == questionNumber } +
                cardIds.mapIndexed { order, cardId ->
                    CardPick(
                        id = CardPick.Id.random(),
                        conversationId = conversationId,
                        questionNumber = questionNumber,
                        cardId = cardId,
                        pickOrder = order,
                        isFinal = isFinal,
                    )
                }
            all + (conversationId to bucket)
        }
    }

    override suspend fun loadPicks(conversationId: Conversation.Id): List<CardPick> =
        picks.value[conversationId].orEmpty()

    override fun observeCompletedSessions(): Flow<List<Session>> =
        combine(sessions, completedIds) { sessions, completed -> completed.mapNotNull { sessions[it] } }

    override fun observeBookmarkedSessions(): Flow<List<Session>> =
        combine(sessions, bookmarkedIds, completedIds) { sessions, bookmarked, completed ->
            bookmarked.filterNot { it in completed }.mapNotNull { sessions[it] }
        }

    override suspend fun deleteSession(id: Session.Id) {
        deletedSessions += id
        sessions.update { it - id }
        states.remove(id)
        completedIds.update { it - id }
        bookmarkedIds.update { it - id }
        val removed = conversations.value[id].orEmpty().map { it.id }
        conversations.update { it - id }
        picks.update { it - removed }
    }

    override suspend fun loadConversations(sessionId: Session.Id): List<Conversation> =
        conversations.value[sessionId].orEmpty()

    override fun observeConversations(sessionId: Session.Id): Flow<List<Conversation>> {
        observeConversationsOverride?.let { return it }
        observeConversationsError?.let { error -> return flow { throw error } }
        return conversations.map { it[sessionId].orEmpty() }
    }

    override fun observePicks(conversationId: Conversation.Id): Flow<List<CardPick>> =
        picks.map { it[conversationId].orEmpty() }
}
