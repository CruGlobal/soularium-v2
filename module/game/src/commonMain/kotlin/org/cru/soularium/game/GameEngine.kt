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
    fun dispatch(event: SessionEvent)
    suspend fun awaitIdle()
    suspend fun bookmark()
    suspend fun discard()
    fun close()

    fun interface Factory {
        fun create(sessionId: Session.Id, kind: Session.Kind): GameEngine
    }
}

fun GameEngine(
    sessionId: Session.Id,
    kind: Session.Kind,
    store: GameSessionStore,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): GameEngine = GameEngineImpl(sessionId, kind, store, dispatcher, GameState())
