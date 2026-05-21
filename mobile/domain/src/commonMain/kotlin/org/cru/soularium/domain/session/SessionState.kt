package org.cru.soularium.domain.session

import kotlinx.serialization.Serializable

@Serializable
sealed interface SessionState {
    @Serializable
    data object NotStarted : SessionState

    @Serializable
    data object AddingParticipants : SessionState

    @Serializable
    data class InQuestion(
        val questionNumber: Int,
        val activeParticipantIndex: Int,
        val activity: QuestionActivity,
    ) : SessionState

    @Serializable
    data object Summary : SessionState

    @Serializable
    data class CollectingContact(val participantIndex: Int) : SessionState

    @Serializable
    data object Concluded : SessionState
}

@Serializable
enum class QuestionActivity {
    ShowingPrompt,
    ShowingInstructions,
    SelectingRound1,
    SelectingRound2,
    Finalizing,
    Discussing,
}
