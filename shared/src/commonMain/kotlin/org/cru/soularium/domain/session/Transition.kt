package org.cru.soularium.domain.session

import org.cru.soularium.domain.DomainError
import org.cru.soularium.domain.content.Questions

fun transition(
    state: SessionState,
    event: SessionEvent,
    ctx: SessionContext,
): TransitionResult = when (state) {
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

private fun transitionAddingParticipants(
    event: SessionEvent,
    ctx: SessionContext,
): TransitionResult = when (event) {
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
                when {
                    ctx.showInstructionsForThisSession &&
                        state.activity == QuestionActivity.ShowingPrompt ->
                        QuestionActivity.ShowingInstructions
                    // "Change Selection" from Finalizing returns to the
                    // narrowing round for a two-round question (so the user
                    // re-picks the final set with their picks intact), or to
                    // round 1 for a one-round question.
                    state.activity == QuestionActivity.Finalizing &&
                        question.selectionRounds == 2 ->
                        QuestionActivity.SelectingRound2
                    else -> QuestionActivity.SelectingRound1
                }
            val next = state.copy(activity = targetActivity)
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }

        SessionEvent.DismissInstructions -> {
            val next = state.copy(activity = QuestionActivity.SelectingRound1)
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }

        is SessionEvent.PickCard, is SessionEvent.UnpickCard ->
            // selection draft state is owned by the UI; the machine treats these as no-ops
            TransitionResult(next = state)

        SessionEvent.ConfirmSelection -> {
            val targetActivity =
                when (state.activity) {
                    QuestionActivity.SelectingRound1 -> {
                        val needed = if (question.selectionRounds == 2) question.requiredImageCount + 1 else question.requiredImageCount
                        if (ctx.currentDraftPicks.size < needed) {
                            return TransitionResult(
                                next = state,
                                error = DomainError.InvalidSelectionCount(needed, ctx.currentDraftPicks.size),
                            )
                        }
                        if (question.selectionRounds == 2) QuestionActivity.SelectingRound2 else QuestionActivity.Finalizing
                    }
                    QuestionActivity.SelectingRound2 -> {
                        if (ctx.currentDraftPicks.size != question.requiredImageCount) {
                            return TransitionResult(
                                next = state,
                                error = DomainError.InvalidSelectionCount(question.requiredImageCount, ctx.currentDraftPicks.size),
                            )
                        }
                        QuestionActivity.Finalizing
                    }
                    else -> return TransitionResult(
                        next = state,
                        error = DomainError.InvalidStateTransition(state.toString(), event::class.simpleName ?: "?"),
                    )
                }
            val next = state.copy(activity = targetActivity)
            TransitionResult(
                next = next,
                effects =
                listOf(
                    Effect.PersistState(next),
                    Effect.PersistPicks(
                        questionNumber = state.questionNumber,
                        participantIndex = state.activeParticipantIndex,
                        cardIds = ctx.currentDraftPicks,
                        isFinal = targetActivity == QuestionActivity.Finalizing,
                    ),
                ),
            )
        }

        SessionEvent.ConfirmFinal -> {
            val source = ctx.currentDraftPicks.ifEmpty { ctx.currentRoundFinalPicks }
            if (source.size != question.requiredImageCount) {
                return TransitionResult(
                    next = state,
                    error = DomainError.InvalidSelectionCount(question.requiredImageCount, source.size),
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
                        cardIds = source,
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

        SessionEvent.Bookmark ->
            TransitionResult(
                next = state,
                effects = listOf(Effect.PersistBookmark(true)),
            )

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
    SessionEvent.Bookmark ->
        TransitionResult(
            next = SessionState.Summary,
            effects = listOf(Effect.PersistBookmark(true)),
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
    SessionEvent.Bookmark ->
        TransitionResult(
            next = state,
            effects = listOf(Effect.PersistBookmark(true)),
        )
    else ->
        TransitionResult(
            next = state,
            error = DomainError.InvalidStateTransition(state.toString(), event::class.simpleName ?: "?"),
        )
}
