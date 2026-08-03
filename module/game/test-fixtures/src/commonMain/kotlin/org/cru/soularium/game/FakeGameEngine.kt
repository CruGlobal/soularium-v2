package org.cru.soularium.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cru.soularium.model.Session

/**
 * Scripted [GameEngine] for isolated presenter tests: set [stateFlow] to drive
 * the UI, script [summaries], and assert on the recorded [dispatched] events and
 * lifecycle counters.
 */
class FakeGameEngine(initialState: GameState = GameState()) : GameEngine {
    val stateFlow = MutableStateFlow(initialState)
    override val state: StateFlow<GameState> = stateFlow.asStateFlow()

    val dispatched = mutableListOf<SessionEvent>()
    var summaries: List<GameEngine.ParticipantSummary> = emptyList()
    var startCount = 0
    var loadSummariesCount = 0
    var bookmarkCount = 0
    var discardCount = 0
    var closeCount = 0

    override suspend fun start() {
        startCount++
    }
    override fun dispatch(event: SessionEvent) {
        dispatched += event
    }
    override suspend fun loadSummaries(): List<GameEngine.ParticipantSummary> {
        loadSummariesCount++
        return summaries
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

    /** [GameEngine.Factory] that always returns [engine]. */
    class Factory(val engine: FakeGameEngine = FakeGameEngine()) : GameEngine.Factory {
        override fun create(sessionId: Session.Id, kind: Session.Kind): GameEngine = engine
    }
}
