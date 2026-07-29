package org.cru.soularium.data.game

import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.game.Effect
import org.cru.soularium.game.GameSessionStore
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

/**
 * [GameSessionStore] backed by the real [SessionRepository]/analytics/crash-reporting ports.
 *
 * [Effect] carries no session id of its own, so this adapter is bound to one [sessionId] for the
 * whole lifetime of the [org.cru.soularium.game.GameEngine] it backs — a fresh instance is built
 * per engine rather than shared across sessions.
 */
class GameSessionStoreImpl(
    private val sessionId: Session.Id,
    private val sessionRepository: SessionRepository,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : GameSessionStore {
    override suspend fun findSessionState(id: Session.Id): SessionState? = sessionRepository.findSessionState(id)

    override suspend fun loadParticipantNames(id: Session.Id): List<String> =
        sessionRepository.loadConversations(id).sortedBy { it.displayOrder }.map { it.contact.name }

    override suspend fun sessionExists(id: Session.Id): Boolean = sessionRepository.findSession(id) != null

    override suspend fun createSession(session: Session, initialState: SessionState) {
        sessionRepository.createSession(session = session, initialState = initialState)
    }

    override suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean) =
        sessionRepository.setBookmarked(id, bookmarked)

    override suspend fun deleteSession(id: Session.Id) = sessionRepository.deleteSession(id)

    override suspend fun execute(effect: Effect) {
        when (effect) {
            is Effect.PersistState ->
                sessionRepository.persistState(sessionId, effect.state)

            is Effect.PersistParticipants ->
                sessionRepository.upsertParticipants(sessionId, effect.names)

            is Effect.PersistPicks -> {
                val convId =
                    sessionRepository.loadConversations(sessionId)
                        .firstOrNull { it.displayOrder == effect.participantIndex }
                        ?.id
                if (convId != null) {
                    sessionRepository.upsertPicks(
                        conversationId = convId,
                        questionNumber = effect.questionNumber,
                        cardIds = effect.cardIds,
                        isFinal = effect.isFinal,
                    )
                }
            }

            is Effect.PersistContact -> {
                val convId =
                    sessionRepository.loadConversations(sessionId)
                        .firstOrNull { it.displayOrder == effect.participantIndex }
                        ?.id
                if (convId != null) {
                    sessionRepository.upsertContact(convId, effect.info)
                }
            }

            is Effect.LogAnalytics ->
                analytics.event(effect.event, effect.params)
        }
    }

    override fun reportNonFatal(throwable: Throwable, context: String) =
        crashReporter.recordNonFatal(throwable, context)
}
