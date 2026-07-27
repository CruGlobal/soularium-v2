package org.cru.soularium.game

data class SessionContext(
    val participantNames: List<String>,
    val currentDraftPicks: List<Int>,
    val showInstructionsForThisSession: Boolean,
)
