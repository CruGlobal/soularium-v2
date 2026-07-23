package org.cru.soularium.domain.session

import org.cru.soularium.domain.DomainError
import org.cru.soularium.domain.content.Questions

fun transition(state: SessionState, event: SessionEvent, ctx: SessionContext): TransitionResult = when (state) {
    SessionState.NotStarted -> transitionNotStarted(event)
    SessionState.AddingParticipants -> transitionAddingParticipants(event, ctx)
    is SessionState.InQuestion -> transitionInQuestion(state, event, ctx)
    SessionState.Summary -> transitionSummary(event)
    is SessionState.CollectingContact -> transitionCollectingContact(state, event, ctx)
    SessionState.Concluded ->
        TransitionResult(
            next = SessionState.Concluded,
            error = DomainError.InvalidStateTransition("Concluded", event::class.simpleName ?: "?"),
        )
}

private fun transitionNotStarted(event: SessionEvent): TransitionResult = when (event) {
    is SessionEvent.StartSession ->
        TransitionResult(
            next = SessionState.AddingParticipants,
            effects =
            listOf(
                Effect.PersistState(SessionState.AddingParticipants),
                Effect.LogAnalytics(
                    event = "session_started",
                    params = mapOf("kind" to event.kind.name.lowercase()),
                ),
            ),
        )
    else ->
        TransitionResult(
            next = SessionState.NotStarted,
            error = DomainError.InvalidStateTransition("NotStarted", event::class.simpleName ?: "?"),
        )
}

private fun transitionAddingParticipants(event: SessionEvent, ctx: SessionContext): TransitionResult = when (event) {
    is SessionEvent.AddParticipant -> {
        val names = ctx.participantNames + event.name
        TransitionResult(
            next = SessionState.AddingParticipants,
            effects = listOf(Effect.PersistParticipants(names)),
        )
    }
    is SessionEvent.RemoveParticipant -> {
        val names =
            ctx.participantNames.toMutableList().also {
                if (event.index in it.indices) it.removeAt(event.index)
            }
        TransitionResult(
            next = SessionState.AddingParticipants,
            effects = listOf(Effect.PersistParticipants(names)),
        )
    }
    SessionEvent.ConfirmParticipants -> {
        if (ctx.participantNames.isEmpty()) {
            TransitionResult(
                next = SessionState.AddingParticipants,
                error = DomainError.InvalidStateTransition("AddingParticipants", "ConfirmParticipants(empty)"),
            )
        } else {
            val next = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
            TransitionResult(
                next = next,
                effects = listOf(Effect.PersistState(next)),
            )
        }
    }
    else ->
        TransitionResult(
            next = SessionState.AddingParticipants,
            error = DomainError.InvalidStateTransition("AddingParticipants", event::class.simpleName ?: "?"),
        )
}

private fun transitionInQuestion(
    state: SessionState.InQuestion,
    event: SessionEvent,
    ctx: SessionContext,
): TransitionResult {
    val question = Questions.byNumber(state.questionNumber)
    return when (event) {
        SessionEvent.BeginSelection -> {
            val targetActivity =
                if (ctx.showInstructionsForThisSession &&
                    state.activity == QuestionActivity.ShowingPrompt
                ) {
                    QuestionActivity.ShowingInstructions
                } else {
                    QuestionActivity.Selecting
                }
            val next = state.copy(activity = targetActivity)
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }

        SessionEvent.DismissInstructions -> {
            val next = state.copy(activity = QuestionActivity.Selecting)
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }

        SessionEvent.ConfirmSelection -> {
            if (state.activity != QuestionActivity.Selecting) {
                return TransitionResult(
                    next = state,
                    error = DomainError.InvalidStateTransition(state.toString(), event::class.simpleName ?: "?"),
                )
            }
            if (ctx.currentDraftPicks.size != question.requiredImageCount) {
                return TransitionResult(
                    next = state,
                    error = DomainError.InvalidSelectionCount(
                        question.requiredImageCount,
                        ctx.currentDraftPicks.size,
                    ),
                )
            }
            val next = state.copy(activity = QuestionActivity.Finalizing)
            TransitionResult(
                next = next,
                effects =
                listOf(
                    Effect.PersistState(next),
                    Effect.PersistPicks(
                        questionNumber = state.questionNumber,
                        participantIndex = state.activeParticipantIndex,
                        cardIds = ctx.currentDraftPicks,
                        isFinal = true,
                    ),
                ),
            )
        }

        SessionEvent.ConfirmFinal -> {
            if (ctx.currentDraftPicks.size != question.requiredImageCount) {
                return TransitionResult(
                    next = state,
                    error = DomainError.InvalidSelectionCount(question.requiredImageCount, ctx.currentDraftPicks.size),
                )
            }
            val next = state.copy(activity = QuestionActivity.Discussing)
            TransitionResult(
                next = next,
                effects =
                listOf(
                    Effect.PersistState(next),
                    Effect.PersistPicks(
                        questionNumber = state.questionNumber,
                        participantIndex = state.activeParticipantIndex,
                        cardIds = ctx.currentDraftPicks,
                        isFinal = true,
                    ),
                    Effect.LogAnalytics(
                        event = "question_completed",
                        params =
                        mapOf(
                            "question_number" to state.questionNumber,
                            "participant_index" to state.activeParticipantIndex,
                            "picks_count" to question.requiredImageCount,
                        ),
                    ),
                ),
            )
        }

        SessionEvent.EndDiscussion -> {
            val isLastParticipant = state.activeParticipantIndex >= ctx.participantNames.size - 1
            val next =
                when {
                    !isLastParticipant ->
                        state.copy(
                            activeParticipantIndex = state.activeParticipantIndex + 1,
                            activity = QuestionActivity.ShowingPrompt,
                        )
                    state.questionNumber < 5 ->
                        SessionState.InQuestion(
                            questionNumber = state.questionNumber + 1,
                            activeParticipantIndex = 0,
                            activity = QuestionActivity.ShowingPrompt,
                        )
                    else -> SessionState.Summary
                }
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }

        else ->
            TransitionResult(
                next = state,
                error = DomainError.InvalidStateTransition(state.toString(), event::class.simpleName ?: "?"),
            )
    }
}

private fun transitionSummary(event: SessionEvent): TransitionResult = when (event) {
    is SessionEvent.CollectContact -> {
        val next = SessionState.CollectingContact(event.participantIndex)
        TransitionResult(
            next = next,
            effects =
            listOf(
                Effect.PersistState(next),
                Effect.PersistContact(event.participantIndex, event.info),
            ),
        )
    }
    SessionEvent.SkipContact ->
        TransitionResult(
            next = SessionState.Concluded,
            effects = listOf(Effect.PersistState(SessionState.Concluded)),
        )
    SessionEvent.Conclude ->
        TransitionResult(
            next = SessionState.Concluded,
            effects =
            listOf(
                Effect.PersistState(SessionState.Concluded),
                Effect.LogAnalytics(event = "session_completed", params = emptyMap()),
            ),
        )
    else ->
        TransitionResult(
            next = SessionState.Summary,
            error = DomainError.InvalidStateTransition("Summary", event::class.simpleName ?: "?"),
        )
}

private fun transitionCollectingContact(
    state: SessionState.CollectingContact,
    event: SessionEvent,
    ctx: SessionContext,
): TransitionResult = when (event) {
    is SessionEvent.CollectContact -> {
        // Persist this participant's contact, then advance — to the next
        // participant's contact form, or Concluded after the last one.
        val nextIndex = state.participantIndex + 1
        val next =
            if (nextIndex >= ctx.participantNames.size) {
                SessionState.Concluded
            } else {
                SessionState.CollectingContact(nextIndex)
            }
        TransitionResult(
            next = next,
            effects =
            listOf(
                Effect.PersistState(next),
                Effect.PersistContact(event.participantIndex, event.info),
            ),
        )
    }
    SessionEvent.SkipContact -> {
        val nextIndex = state.participantIndex + 1
        if (nextIndex >= ctx.participantNames.size) {
            TransitionResult(
                next = SessionState.Concluded,
                effects = listOf(Effect.PersistState(SessionState.Concluded)),
            )
        } else {
            val next = SessionState.CollectingContact(nextIndex)
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }
    }
    SessionEvent.Conclude ->
        TransitionResult(
            next = SessionState.Concluded,
            effects =
            listOf(
                Effect.PersistState(SessionState.Concluded),
                Effect.LogAnalytics(event = "session_completed", params = emptyMap()),
            ),
        )
    else ->
        TransitionResult(
            next = state,
            error = DomainError.InvalidStateTransition(state.toString(), event::class.simpleName ?: "?"),
        )
}
