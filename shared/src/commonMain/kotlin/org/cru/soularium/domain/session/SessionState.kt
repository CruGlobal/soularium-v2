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

    // Kotlin variant was renamed from SelectingRound1 after the two-round flow was
    // collapsed into one; the @SerialName is preserved so persisted sessions still
    // deserialize.
    @SerialName("selecting_round_1")
    Selecting,

    @SerialName("finalizing")
    Finalizing,

    @SerialName("discussing")
    Discussing,
}
