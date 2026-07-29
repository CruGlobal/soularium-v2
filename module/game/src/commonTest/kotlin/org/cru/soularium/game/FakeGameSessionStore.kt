package org.cru.soularium.game

import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

internal class FakeGameSessionStore : GameSessionStore {
    var persistedState: SessionState? = null
    var participantNames: List<String> = emptyList()
    var sessionExists = false
    var findSessionStateError: Throwable? = null
    var executeError: Throwable? = null
    var setBookmarkedError: Throwable? = null
    var deleteSessionError: Throwable? = null

    val executed = mutableListOf<Effect>()
    val created = mutableListOf<Pair<Session, SessionState>>()
    val bookmarked = mutableListOf<Boolean>()
    var deleted = 0
    val nonFatals = mutableListOf<String>()

    override suspend fun findSessionState(id: Session.Id): SessionState? {
        findSessionStateError?.let { throw it }
        return persistedState
    }

    override suspend fun loadParticipantNames(id: Session.Id): List<String> = participantNames

    override suspend fun sessionExists(id: Session.Id): Boolean = sessionExists

    override suspend fun createSession(session: Session, initialState: SessionState) {
        created += session to initialState
        sessionExists = true
    }

    override suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean) {
        setBookmarkedError?.let { throw it }
        this.bookmarked += bookmarked
    }

    override suspend fun deleteSession(id: Session.Id) {
        deleteSessionError?.let { throw it }
        deleted++
    }

    override suspend fun execute(id: Session.Id, effect: Effect) {
        executeError?.let { throw it }
        executed += effect
        if (effect is Effect.PersistState) persistedState = effect.state
    }

    override fun reportNonFatal(throwable: Throwable, context: String) {
        nonFatals += context
    }
}
