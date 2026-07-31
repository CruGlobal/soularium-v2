package org.cru.soularium.game

import kotlinx.coroutines.flow.StateFlow
import org.cru.soularium.model.CardPick
import org.cru.soularium.model.Session

/**
 * Drives a single conversation session's state machine: [dispatch] applies an event and queues
 * its resulting [Effect]s for asynchronous FIFO execution, while [state] exposes the current
 * [GameState] for the UI to render.
 */
interface GameEngine {
    val state: StateFlow<GameState>

    suspend fun start()

    /** Applies [event] to the current state synchronously; its resulting effects are enqueued for FIFO execution. */
    fun dispatch(event: SessionEvent)

    /**
     * Loads each participant's persisted picks for the summary screen. Runs after already-queued
     * effects, so it observes every pick dispatched before the call; returns an empty list
     * (reporting the failure) when loading fails.
     */
    suspend fun loadSummaries(): List<ParticipantSummary>

    /** Runs after already-queued effects; completes even if the underlying host write fails. */
    suspend fun bookmark()

    /** Runs after already-queued effects; completes even if the underlying host write fails. */
    suspend fun discard()

    /** Stops accepting new work; already-queued effects still drain before the engine's scope is cancelled. */
    fun close()

    /** One participant's persisted picks, as loaded by [loadSummaries]. */
    data class ParticipantSummary(val participantIndex: Int, val name: String, val picks: List<CardPick>)

    /** Graph-injected creation of a [GameEngine] for a given session. */
    interface Factory {
        fun create(sessionId: Session.Id, kind: Session.Kind): GameEngine
    }
}
