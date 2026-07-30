package org.cru.soularium.game

import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

/**
 * The persistence/analytics port a [GameEngine] drives; implemented in `:module:game` over
 * [SessionRepository] and the analytics ports.
 */
interface GameSessionStore {
    suspend fun findSessionState(id: Session.Id): SessionState?

    suspend fun loadParticipantNames(id: Session.Id): List<String>

    suspend fun sessionExists(id: Session.Id): Boolean

    suspend fun createSession(session: Session, initialState: SessionState)

    suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean)

    suspend fun deleteSession(id: Session.Id)

    suspend fun execute(id: Session.Id, effect: Effect)

    fun reportNonFatal(throwable: Throwable, context: String)
}
