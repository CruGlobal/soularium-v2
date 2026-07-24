package org.cru.soularium.db.room.repository

import androidx.room.Dao
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.db.room.SoulariumDatabase
import org.cru.soularium.db.room.entities.CardPickEntity
import org.cru.soularium.db.room.entities.ConversationEntity
import org.cru.soularium.db.room.entities.SessionEntity
import org.cru.soularium.model.CardPick
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

@Dao
internal abstract class SessionRoomRepository(private val db: SoulariumDatabase) : SessionRepository {
    private val sessionDao get() = db.sessionDao
    private val conversationDao get() = db.conversationDao
    private val cardPickDao get() = db.cardPickDao
    private val json: Json = Json { ignoreUnknownKeys = true }

    override suspend fun createSession(session: Session, initialState: SessionState): Session.Id {
        sessionDao.upsert(session.toEntity(initialState))
        return session.id
    }

    override suspend fun loadSession(id: Session.Id): Session? = sessionDao.byId(id.value)?.toDomain()

    override suspend fun loadState(id: Session.Id): SessionState? = sessionDao.byId(id.value)?.let {
        json.decodeFromString<SessionState>(it.stateSnapshotJson)
    }

    override suspend fun persistState(id: Session.Id, state: SessionState) {
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

    override suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean) {
        val current = sessionDao.byId(id.value) ?: return
        val bookmarkedAt = if (bookmarked) Clock.System.now().toEpochMilliseconds() else null
        sessionDao.upsert(current.copy(bookmarkedAt = bookmarkedAt))
    }

    override suspend fun setEnded(id: Session.Id) {
        val current = sessionDao.byId(id.value) ?: return
        sessionDao.upsert(current.copy(endedAt = Clock.System.now().toEpochMilliseconds()))
    }

    override suspend fun upsertParticipants(sessionId: Session.Id, names: List<String>): List<Conversation.Id> {
        val existing = conversationDao.forSession(sessionId.value)
        val keptIds = mutableListOf<Conversation.Id>()
        names.forEachIndexed { index, name ->
            val match = existing.find { it.displayOrder == index }
            val id = match?.id ?: Conversation.Id.random().value
            keptIds += Conversation.Id(id)
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

    override suspend fun upsertContact(conversationId: Conversation.Id, info: ContactInfo) {
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
        conversationId: Conversation.Id,
        questionNumber: Int,
        cardIds: List<Int>,
        isFinal: Boolean,
    ) {
        val entities =
            cardIds.mapIndexed { i, cid ->
                CardPickEntity(
                    id = CardPick.Id.random().value,
                    conversationId = conversationId.value,
                    questionNumber = questionNumber,
                    cardId = cid,
                    pickOrder = i,
                    isFinal = isFinal,
                )
            }
        // Atomic delete-then-insert so a cancelled write can't drop the round.
        cardPickDao.replaceRound(conversationId.value, questionNumber, isFinal, entities)
    }

    override suspend fun loadPicks(conversationId: Conversation.Id): List<CardPick> =
        cardPickDao.forConversation(conversationId.value).map {
            it.toDomain()
        }

    override fun observeCompletedSessions(): Flow<List<Session>> = sessionDao.observeCompleted().map { list ->
        list.map { it.toDomain() }
    }

    override fun observeBookmarkedSessions(): Flow<List<Session>> = sessionDao.observeBookmarked().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun deleteSession(id: Session.Id) {
        sessionDao.delete(id.value)
    }

    override suspend fun loadConversations(sessionId: Session.Id): List<Conversation> =
        conversationDao.forSession(sessionId.value).map {
            it.toDomain()
        }

    private fun Session.toEntity(state: SessionState) = SessionEntity(
        id = id.value,
        kind = kind.name,
        startedAt = startedAt.toEpochMilliseconds(),
        endedAt = endedAt?.toEpochMilliseconds(),
        bookmarkedAt = bookmarkedAt?.toEpochMilliseconds(),
        stateSnapshotJson = json.encodeToString(state),
        selectionInstructionsShown = selectionInstructionsShown,
    )

    private fun SessionEntity.toDomain() = Session(
        id = Session.Id(id),
        // Tolerate an unrecognised persisted kind (corruption / a value
        // written by a newer build) rather than throwing and taking down
        // the whole Past Conversations list.
        kind = Session.Kind.entries.firstOrNull { it.name == kind } ?: Session.Kind.SOLO,
        startedAt = Instant.fromEpochMilliseconds(startedAt),
        endedAt = endedAt?.let(Instant::fromEpochMilliseconds),
        bookmarkedAt = bookmarkedAt?.let(Instant::fromEpochMilliseconds),
        selectionInstructionsShown = selectionInstructionsShown,
    )

    private fun ConversationEntity.toDomain() = Conversation(
        id = Conversation.Id(id),
        sessionId = Session.Id(sessionId),
        displayOrder = displayOrder,
        contact = ContactInfo(name, surname, email, phone, notes),
    )

    private fun CardPickEntity.toDomain() = CardPick(
        id = CardPick.Id(id),
        conversationId = Conversation.Id(conversationId),
        questionNumber = questionNumber,
        cardId = cardId,
        pickOrder = pickOrder,
        isFinal = isFinal,
    )
}
