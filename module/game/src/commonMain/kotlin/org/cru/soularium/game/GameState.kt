package org.cru.soularium.game

import org.cru.soularium.model.game.SessionState

/** The [GameEngine]'s full state: the persisted [session] plus the volatile context around it. */
data class GameState(
    val session: SessionState = SessionState.NotStarted,
    val participantNames: List<String> = emptyList(),
    val draftPicks: List<Int> = emptyList(),
    val instructionsShown: Boolean = false,
)
