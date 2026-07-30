package org.cru.soularium.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cru.soularium.model.Session

/**
 * Scripted [GameEngine] for isolated presenter tests: set [stateFlow] to drive
 * the UI, and assert on the recorded [dispatched] events and lifecycle counters.
 */
class FakeGameEngine(initialState: GameState = GameState()) : GameEngine {
    val stateFlow = MutableStateFlow(initialState)
    override val state: StateFlow<GameState> = stateFlow.asStateFlow()

    val dispatched = mutableListOf<SessionEvent>()
    var startCount = 0
    var awaitIdleCount = 0
    var bookmarkCount = 0
    var discardCount = 0
    var closeCount = 0

    override suspend fun start() {
        startCount++
    }
    override fun dispatch(event: SessionEvent) {
        dispatched += event
    }
    override suspend fun awaitIdle() {
        awaitIdleCount++
    }
    override suspend fun bookmark() {
        bookmarkCount++
    }
    override suspend fun discard() {
        discardCount++
    }
    override fun close() {
        closeCount++
    }

    /**
     * [GameEngine.Factory] that always returns [engine], recording each [create] call
     * so tests can assert on the requested session id, kind, and initial state.
     */
    class Factory(val engine: FakeGameEngine = FakeGameEngine()) : GameEngine.Factory {
        val createCalls = mutableListOf<CreateCall>()

        override fun create(sessionId: Session.Id, kind: Session.Kind, initialState: GameState): GameEngine {
            createCalls += CreateCall(sessionId, kind, initialState)
            return engine
        }

        data class CreateCall(val sessionId: Session.Id, val kind: Session.Kind, val initialState: GameState)
    }
}
