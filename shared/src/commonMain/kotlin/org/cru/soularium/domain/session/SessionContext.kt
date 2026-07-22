package org.cru.soularium.domain.session

data class SessionContext(
    val participantNames: List<String>,
    val currentDraftPicks: List<Int>,
    val showInstructionsForThisSession: Boolean,
)
