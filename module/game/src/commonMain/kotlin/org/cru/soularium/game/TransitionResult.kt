package org.cru.soularium.game

import org.cru.soularium.model.game.SessionState

data class TransitionResult(
    val next: SessionState,
    val effects: List<Effect> = emptyList(),
    val error: GameError? = null,
)
