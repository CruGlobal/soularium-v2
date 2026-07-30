package org.cru.soularium.game

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.analytics.AnalyticsTracker
import org.cru.soularium.analytics.CrashReporter
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

/** [GameEngine.Host] backed by the real [SessionRepository]/analytics/crash-reporting ports. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class GameEngineHostImpl(
    private val sessionRepository: SessionRepository,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : GameEngine.Host {
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

    override suspend fun execute(id: Session.Id, effect: Effect) {
        when (effect) {
            is Effect.PersistState ->
                sessionRepository.persistState(id, effect.state)

            is Effect.PersistParticipants ->
                sessionRepository.upsertParticipants(id, effect.names)

            is Effect.PersistPicks -> {
                val convId =
                    sessionRepository.loadConversations(id)
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
                    sessionRepository.loadConversations(id)
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
