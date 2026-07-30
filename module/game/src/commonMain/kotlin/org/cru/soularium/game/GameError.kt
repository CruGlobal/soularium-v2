package org.cru.soularium.game

sealed interface GameError {
    data class InvalidStateTransition(val from: String, val event: String) : GameError

    data class InvalidSelectionCount(val expected: Int, val got: Int) : GameError
}
