package org.cru.soularium.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cru.soularium.data.db.CardPickDao
import org.cru.soularium.data.db.ConversationDao
import org.cru.soularium.data.db.SessionDao
import org.cru.soularium.data.db.entities.CardPickEntity
import org.cru.soularium.data.db.entities.ConversationEntity
import org.cru.soularium.data.db.entities.SessionEntity
import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.CardPickId
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.Conversation
import org.cru.soularium.domain.ConversationId
import org.cru.soularium.domain.Session
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.session.SessionState

class SessionRepositoryImpl(
    private val sessionDao: SessionDao,
    private val conversationDao: ConversationDao,
    private val cardPickDao: CardPickDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionRepository {
    override suspend fun createSession(
        session: Session,
        initialState: SessionState,
    ): SessionId {
        sessionDao.upsert(session.toEntity(initialState))
        return session.id
    }

    override suspend fun loadSession(id: SessionId): Session? = sessionDao.byId(id.value)?.toDomain()

    override suspend fun loadState(id: SessionId): SessionState? =
        sessionDao.byId(id.value)?.let { json.decodeFromString<SessionState>(it.stateSnapshotJson) }

    override suspend fun persistState(
        id: SessionId,
        state: SessionState,
    ) {
        val current = sessionDao.byId(id.value) ?: return
        // Reaching Concluded ends the session, so it surfaces under
        // Past Conversations → Completed (which filters on ended_at).
        val endedAt =
            if (state == SessionState.Concluded) {
                current.endedAt ?: Clock.System.now().toEpochMilliseconds()
            } else {
                current.endedAt
            }
        sessionDao.upsert(
            current.copy(
                stateSnapshotJson = json.encodeToString(state),
                endedAt = endedAt,
            ),
        )
    }

    override suspend fun setBookmarked(
        id: SessionId,
        bookmarked: Boolean,
    ) {
        val current = sessionDao.byId(id.value) ?: return
        val bookmarkedAt = if (bookmarked) Clock.System.now().toEpochMilliseconds() else null
        sessionDao.upsert(current.copy(bookmarkedAt = bookmarkedAt))
    }

    override suspend fun setEnded(id: SessionId) {
        val current = sessionDao.byId(id.value) ?: return
        sessionDao.upsert(current.copy(endedAt = Clock.System.now().toEpochMilliseconds()))
    }

    override suspend fun upsertParticipants(
        sessionId: SessionId,
        names: List<String>,
    ): List<ConversationId> {
        val existing = conversationDao.forSession(sessionId.value)
        val keptIds = mutableListOf<ConversationId>()
        names.forEachIndexed { index, name ->
            val match = existing.find { it.displayOrder == index }
            val id = match?.id ?: ConversationId.random().value
            keptIds += ConversationId(id)
            conversationDao.upsert(
                ConversationEntity(
                    id = id,
                    sessionId = sessionId.value,
                    displayOrder = index,
                    name = name,
                    surname = match?.surname,
                    email = match?.email,
                    phone = match?.phone,
                    notes = match?.notes,
                ),
            )
        }
        // Prune conversation rows left over from a now-shorter participant list,
        // so removed participants don't linger as orphaned rows (with their
        // cascaded card_picks) in loadConversations / loadSummaries.
        conversationDao.deleteFrom(sessionId.value, names.size)
        return keptIds
    }

    override suspend fun upsertContact(
        conversationId: ConversationId,
        info: ContactInfo,
    ) {
        val current = conversationDao.byId(conversationId.value) ?: return
        conversationDao.upsert(
            current.copy(
                name = info.name,
                surname = info.surname,
                email = info.email,
                phone = info.phone,
                notes = info.notes,
            ),
        )
    }

    override suspend fun upsertPicks(
        conversationId: ConversationId,
        questionNumber: Int,
        cardIds: List<Int>,
        isFinal: Boolean,
    ) {
        cardPickDao.deleteForRound(conversationId.value, questionNumber, isFinal)
        val entities =
            cardIds.mapIndexed { i, cid ->
                CardPickEntity(
                    id = CardPickId.random().value,
                    conversationId = conversationId.value,
                    questionNumber = questionNumber,
                    cardId = cid,
                    pickOrder = i,
                    isFinal = isFinal,
                )
            }
        cardPickDao.upsertAll(entities)
    }

    override suspend fun loadPicks(conversationId: ConversationId): List<CardPick> =
        cardPickDao.forConversation(conversationId.value).map { it.toDomain() }

    override fun observeCompletedSessions(): Flow<List<Session>> = sessionDao.observeCompleted().map { list -> list.map { it.toDomain() } }

    override fun observeBookmarkedSessions(): Flow<List<Session>> =
        sessionDao.observeBookmarked().map { list -> list.map { it.toDomain() } }

    override suspend fun deleteSession(id: SessionId) {
        sessionDao.delete(id.value)
    }

    override suspend fun loadConversations(sessionId: SessionId): List<Conversation> =
        conversationDao.forSession(sessionId.value).map { it.toDomain() }

    private fun Session.toEntity(state: SessionState) =
        SessionEntity(
            id = id.value,
            kind = kind.name,
            startedAt = startedAt.toEpochMilliseconds(),
            endedAt = endedAt?.toEpochMilliseconds(),
            bookmarkedAt = bookmarkedAt?.toEpochMilliseconds(),
            stateSnapshotJson = json.encodeToString(state),
            selectionInstructionsShown = selectionInstructionsShown,
        )

    private fun SessionEntity.toDomain() =
        Session(
            id = SessionId(id),
            // Tolerate an unrecognised persisted kind (corruption / a value
            // written by a newer build) rather than throwing and taking down
            // the whole Past Conversations list.
            kind = SessionKind.entries.firstOrNull { it.name == kind } ?: SessionKind.SOLO,
            startedAt = Instant.fromEpochMilliseconds(startedAt),
            endedAt = endedAt?.let(Instant::fromEpochMilliseconds),
            bookmarkedAt = bookmarkedAt?.let(Instant::fromEpochMilliseconds),
            selectionInstructionsShown = selectionInstructionsShown,
        )

    private fun ConversationEntity.toDomain() =
        Conversation(
            id = ConversationId(id),
            sessionId = SessionId(sessionId),
            displayOrder = displayOrder,
            contact = ContactInfo(name, surname, email, phone, notes),
        )

    private fun CardPickEntity.toDomain() =
        CardPick(
            id = CardPickId(id),
            conversationId = ConversationId(conversationId),
            questionNumber = questionNumber,
            cardId = cardId,
            pickOrder = pickOrder,
            isFinal = isFinal,
        )
}
