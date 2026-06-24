package org.cru.soularium.domain.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The persisted snapshot of where a conversation is in its flow.
 *
 * Instances are serialized to JSON and stored in the `sessions` table
 * (`state_snapshot_json`). The `@SerialName` on every variant pins the
 * serialized type discriminator to a stable string so that renaming or moving
 * these Kotlin types never breaks an already-persisted (e.g. bookmarked)
 * session. Do not change an existing `@SerialName` value.
 */
@Serializable
sealed interface SessionState {
    @Serializable
    @SerialName("not_started")
    data object NotStarted : SessionState

    @Serializable
    @SerialName("adding_participants")
    data object AddingParticipants : SessionState

    @Serializable
    @SerialName("in_question")
    data class InQuestion(val questionNumber: Int, val activeParticipantIndex: Int, val activity: QuestionActivity) :
        SessionState

    @Serializable
    @SerialName("summary")
    data object Summary : SessionState

    @Serializable
    @SerialName("collecting_contact")
    data class CollectingContact(val participantIndex: Int) : SessionState

    @Serializable
    @SerialName("concluded")
    data object Concluded : SessionState
}

@Serializable
enum class QuestionActivity {
    @SerialName("showing_prompt")
    ShowingPrompt,

    @SerialName("showing_instructions")
    ShowingInstructions,

    @SerialName("selecting_round_1")
    SelectingRound1,

    @SerialName("selecting_round_2")
    SelectingRound2,

    @SerialName("finalizing")
    Finalizing,

    @SerialName("discussing")
    Discussing,
}
