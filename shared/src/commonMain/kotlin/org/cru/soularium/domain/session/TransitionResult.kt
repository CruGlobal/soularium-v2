package org.cru.soularium.domain.session

import org.cru.soularium.domain.DomainError
import org.cru.soularium.model.game.SessionState

data class TransitionResult(
    val next: SessionState,
    val effects: List<Effect> = emptyList(),
    val error: DomainError? = null,
)
