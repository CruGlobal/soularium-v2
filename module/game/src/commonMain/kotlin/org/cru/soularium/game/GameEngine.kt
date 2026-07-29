package org.cru.soularium.game

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import org.cru.soularium.model.Session

/**
 * Drives a single conversation session's state machine: [dispatch] applies an event and queues
 * its resulting [Effect]s for asynchronous execution against the [GameSessionStore], while
 * [state] exposes the current [GameState] for the UI to render.
 */
interface GameEngine {
    val state: StateFlow<GameState>

    suspend fun start()

    /** Applies [event] to the current state synchronously; its resulting effects are enqueued for FIFO execution. */
    fun dispatch(event: SessionEvent)

    /** Suspends until every effect enqueued so far has finished executing against the [GameSessionStore]. */
    suspend fun awaitIdle()

    /** Runs after already-queued effects; completes even if the underlying store write fails. */
    suspend fun bookmark()

    /** Runs after already-queued effects; completes even if the underlying store write fails. */
    suspend fun discard()

    /** Stops accepting new work; already-queued effects still drain before the engine's scope is cancelled. */
    fun close()

    /** Graph-injected creation of a [GameEngine] for a given session. */
    fun interface Factory {
        fun create(sessionId: Session.Id, kind: Session.Kind): GameEngine
    }
}

/** Direct construction of a [GameEngine], e.g. for tests; production code goes through [GameEngine.Factory]. */
fun GameEngine(
    sessionId: Session.Id,
    kind: Session.Kind,
    store: GameSessionStore,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): GameEngine = GameEngineImpl(sessionId, kind, store, dispatcher, GameState())
