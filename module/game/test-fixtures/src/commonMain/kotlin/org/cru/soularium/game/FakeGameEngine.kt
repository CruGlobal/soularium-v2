package org.cru.soularium.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}
