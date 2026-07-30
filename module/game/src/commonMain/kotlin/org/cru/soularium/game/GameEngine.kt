package org.cru.soularium.game

import kotlinx.coroutines.flow.StateFlow
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

/**
 * Drives a single conversation session's state machine: [dispatch] applies an event and queues
 * its resulting [Effect]s for asynchronous execution against the [Host], while
 * [state] exposes the current [GameState] for the UI to render.
 */
interface GameEngine {
    val state: StateFlow<GameState>

    suspend fun start()

    /** Applies [event] to the current state synchronously; its resulting effects are enqueued for FIFO execution. */
    fun dispatch(event: SessionEvent)

    /** Suspends until every effect enqueued so far has finished executing against the [Host]. */
    suspend fun awaitIdle()

    /** Runs after already-queued effects; completes even if the underlying host write fails. */
    suspend fun bookmark()

    /** Runs after already-queued effects; completes even if the underlying host write fails. */
    suspend fun discard()

    /** Stops accepting new work; already-queued effects still drain before the engine's scope is cancelled. */
    fun close()

    /** Graph-injected creation of a [GameEngine] for a given session. */
    interface Factory {
        fun create(sessionId: Session.Id, kind: Session.Kind, initialState: GameState = GameState()): GameEngine
    }

    /**
     * Everything a running [GameEngine] may do to the outside world — rehydration reads,
     * effect and session-lifecycle writes, and telemetry; implemented over
     * [SessionRepository][org.cru.soularium.db.repository.SessionRepository] and the
     * analytics ports.
     */
    interface Host {
        suspend fun findSessionState(id: Session.Id): SessionState?

        suspend fun loadParticipantNames(id: Session.Id): List<String>

        suspend fun sessionExists(id: Session.Id): Boolean

        suspend fun createSession(session: Session, initialState: SessionState)

        suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean)

        suspend fun deleteSession(id: Session.Id)

        suspend fun execute(id: Session.Id, effect: Effect)

        fun reportNonFatal(throwable: Throwable, context: String)
    }
}
