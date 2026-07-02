package org.cru.soularium.game

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.cru.soularium.analytics.AnalyticsTracker
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

/** [GameEngineImpl.Host] backed by the real [SessionRepository]/analytics ports. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class GameEngineHostImpl(
    private val sessionRepository: SessionRepository,
    private val analytics: AnalyticsTracker,
) : GameEngineImpl.Host {
    /**
     * The last-read displayOrder → conversation-id mapping, so per-pick and per-contact effects
     * don't reload the conversation list on every write. Refreshed by every conversation read and
     * every [Effect.PersistParticipants]; a lookup for a different session falls back to a query.
     */
    private var cachedConversationIds: Pair<Session.Id, Map<Int, Conversation.Id>>? = null

    override suspend fun findSessionState(id: Session.Id): SessionState? = sessionRepository.findSessionState(id)

    override suspend fun loadParticipantNames(id: Session.Id): List<String> =
        loadConversations(id).sortedBy { it.displayOrder }.map { it.contact.name }

    override suspend fun loadSummaries(id: Session.Id): List<GameEngine.ParticipantSummary> = coroutineScope {
        loadConversations(id)
            .map { conversation ->
                async {
                    GameEngine.ParticipantSummary(
                        participantIndex = conversation.displayOrder,
                        name = conversation.contact.name,
                        picks = sessionRepository.loadPicks(conversation.id),
                    )
                }
            }
            .awaitAll()
    }

    override suspend fun sessionExists(id: Session.Id): Boolean = sessionRepository.findSession(id) != null

    override suspend fun createSession(session: Session, initialState: SessionState) {
        sessionRepository.createSession(session = session, initialState = initialState)
    }

    override suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean) =
        sessionRepository.setBookmarked(id, bookmarked)

    override suspend fun deleteSession(id: Session.Id) = sessionRepository.deleteSession(id)

    override suspend fun execute(id: Session.Id, effect: Effect) {
        when (effect) {
            is Effect.PersistState ->
                sessionRepository.persistState(id, effect.state)

            is Effect.PersistParticipants -> {
                val ids = sessionRepository.upsertParticipants(id, effect.names)
                cachedConversationIds = id to ids.withIndex().associate { (index, convId) -> index to convId }
            }

            is Effect.PersistPicks ->
                sessionRepository.upsertPicks(
                    conversationId = conversationId(id, effect.participantIndex),
                    questionNumber = effect.questionNumber,
                    cardIds = effect.cardIds,
                    isFinal = effect.isFinal,
                )

            is Effect.PersistContact ->
                sessionRepository.upsertContact(conversationId(id, effect.participantIndex), effect.info)

            is Effect.LogAnalytics ->
                analytics.event(effect.event, effect.params)
        }
    }

    private suspend fun loadConversations(id: Session.Id): List<Conversation> = sessionRepository.loadConversations(id)
        .also { cachedConversationIds = id to it.associate { c -> c.displayOrder to c.id } }

    private suspend fun conversationId(id: Session.Id, participantIndex: Int): Conversation.Id {
        val ids =
            cachedConversationIds?.takeIf { it.first == id }?.second
                ?: loadConversations(id).associate { it.displayOrder to it.id }
        // A missing row is an invariant violation; throwing lets the engine's worker report it.
        return requireNotNull(ids[participantIndex]) { "no conversation with displayOrder $participantIndex" }
    }
}
