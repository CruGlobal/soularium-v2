package org.cru.soularium.game

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cru.soularium.game.content.Question
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState
import org.cru.soularium.model.game.SessionState.InQuestion.QuestionState

@AssistedInject
internal class GameEngineImpl(
    private val host: GameEngine.Host,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    @Assisted private val sessionId: Session.Id,
    @Assisted private val kind: Session.Kind,
    @Assisted initialState: GameState,
) : GameEngine {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val queue = Channel<QueuedOp>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        scope.launch {
            for (queued in queue) {
                withContext(NonCancellable) {
                    runCatching { queued.op() }
                        .onFailure { host.reportNonFatal(it, queued.context) }
                }
            }
        }.invokeOnCompletion { scope.cancel() }
    }

    override fun dispatch(event: SessionEvent) {
        val current = _state.value
        val result = step(current.session, event, current)
        val context = "applyEffects after $event"
        if (result.error != null) {
            enqueue(context) {
                host.execute(
                    sessionId,
                    Effect.LogAnalytics(
                        "transition_error",
                        mapOf(
                            "error" to (result.error::class.simpleName ?: "unknown")
                        )
                    ),
                )
            }
            return
        }
        _state.value = evolve(current, event, result)
        result.effects.forEach { effect -> enqueue(context) { host.execute(sessionId, effect) } }
    }

    override suspend fun start() {
        var loadFailed = false
        val loaded =
            runCatching { host.findSessionState(sessionId) }
                .onFailure {
                    loadFailed = true
                    host.reportNonFatal(it, "findSessionState on start")
                }
                .getOrNull()
        if (loaded != null) {
            _state.update { it.copy(session = snapBackToPromptIfMidQuestion(loaded)) }
            runCatching { host.loadParticipantNames(sessionId) }
                .onSuccess { names -> if (names.isNotEmpty()) _state.update { it.copy(participantNames = names) } }
                .onFailure { host.reportNonFatal(it, "loadParticipantNames on start") }
        }
        if (_state.value.session == SessionState.NotStarted) {
            val exists =
                runCatching { host.sessionExists(sessionId) }
                    .getOrElse {
                        host.reportNonFatal(it, "sessionExists on start")
                        false
                    }
            if (!exists || loadFailed) {
                runCatching { host.createSession(Session(id = sessionId, kind = kind), SessionState.NotStarted) }
                    .onFailure { host.reportNonFatal(it, "createSession on start") }
            }
            dispatch(SessionEvent.StartSession(kind))
        }
    }

    override suspend fun awaitIdle() {
        val done = CompletableDeferred<Unit>()
        enqueue("awaitIdle") { done.complete(Unit) }
        done.await()
    }

    override suspend fun bookmark() {
        val done = CompletableDeferred<Unit>()
        enqueue("bookmarkAndExit") {
            try {
                runCatching { host.setBookmarked(sessionId, true) }
                    .onFailure { host.reportNonFatal(it, "bookmarkAndExit") }
                host.execute(sessionId, Effect.LogAnalytics("conversation_bookmarked", emptyMap()))
            } finally {
                done.complete(Unit)
            }
        }
        done.await()
    }

    override suspend fun discard() {
        val done = CompletableDeferred<Unit>()
        enqueue("discardAndExit") {
            try {
                host.deleteSession(sessionId)
            } finally {
                done.complete(Unit)
            }
        }
        done.await()
    }

    override fun close() {
        queue.close() // worker drains what is already queued, then the scope dies
    }

    private fun enqueue(context: String, op: suspend () -> Unit) {
        queue.trySend(QueuedOp(context, op))
    }

    private class QueuedOp(val context: String, val op: suspend () -> Unit)

    private fun evolve(current: GameState, event: SessionEvent, result: StepResult): GameState {
        var next = current.copy(session = result.next)
        if (event is SessionEvent.ToggleCard) {
            next =
                next.copy(
                    draftPicks =
                    if (event.cardId in next.draftPicks) {
                        next.draftPicks - event.cardId
                    } else {
                        next.draftPicks + event.cardId
                    },
                )
        }
        if (event is SessionEvent.DismissInstructions) next = next.copy(instructionsShown = true)
        for (effect in result.effects) {
            if (effect is Effect.PersistParticipants) next = next.copy(participantNames = effect.names)
        }
        val enteringPromptForNewTurn =
            (result.next as? SessionState.InQuestion)?.activity == QuestionState.ShowingPrompt
        if (enteringPromptForNewTurn) next = next.copy(draftPicks = emptyList())
        return next
    }

    private fun step(session: SessionState, event: SessionEvent, state: GameState): StepResult = when (session) {
        SessionState.NotStarted -> transitionNotStarted(event)
        SessionState.AddingParticipants -> transitionAddingParticipants(event, state)
        is SessionState.InQuestion -> transitionInQuestion(session, event, state)
        SessionState.Summary -> transitionSummary(event)
        is SessionState.CollectingContact -> transitionCollectingContact(session, event, state)
        SessionState.Concluded ->
            StepResult(
                next = SessionState.Concluded,
                error = GameError.InvalidStateTransition("Concluded", event::class.simpleName ?: "?"),
            )
    }

    private fun transitionNotStarted(event: SessionEvent): StepResult = when (event) {
        is SessionEvent.StartSession ->
            StepResult(
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
            StepResult(
                next = SessionState.NotStarted,
                error = GameError.InvalidStateTransition("NotStarted", event::class.simpleName ?: "?"),
            )
    }

    private fun transitionAddingParticipants(event: SessionEvent, state: GameState): StepResult = when (event) {
        is SessionEvent.AddParticipant -> {
            val names = state.participantNames + event.name
            StepResult(
                next = SessionState.AddingParticipants,
                effects = listOf(Effect.PersistParticipants(names)),
            )
        }
        is SessionEvent.RemoveParticipant -> {
            val names =
                state.participantNames.toMutableList().also {
                    if (event.index in it.indices) it.removeAt(event.index)
                }
            StepResult(
                next = SessionState.AddingParticipants,
                effects = listOf(Effect.PersistParticipants(names)),
            )
        }
        SessionEvent.ConfirmParticipants -> {
            if (state.participantNames.isEmpty()) {
                StepResult(
                    next = SessionState.AddingParticipants,
                    error = GameError.InvalidStateTransition("AddingParticipants", "ConfirmParticipants(empty)"),
                )
            } else {
                val next = SessionState.InQuestion(1, 0, QuestionState.ShowingPrompt)
                StepResult(
                    next = next,
                    effects = listOf(Effect.PersistState(next)),
                )
            }
        }
        else ->
            StepResult(
                next = SessionState.AddingParticipants,
                error = GameError.InvalidStateTransition("AddingParticipants", event::class.simpleName ?: "?"),
            )
    }

    private fun transitionInQuestion(
        session: SessionState.InQuestion,
        event: SessionEvent,
        state: GameState,
    ): StepResult {
        val question = Question.forNumber(session.questionNumber)
        return when (event) {
            SessionEvent.BeginSelection -> {
                val targetActivity =
                    if (!state.instructionsShown &&
                        session.activity == QuestionState.ShowingPrompt
                    ) {
                        QuestionState.ShowingInstructions
                    } else {
                        QuestionState.Selecting
                    }
                val next = session.copy(activity = targetActivity)
                StepResult(next = next, effects = listOf(Effect.PersistState(next)))
            }

            SessionEvent.DismissInstructions -> {
                val next = session.copy(activity = QuestionState.Selecting)
                StepResult(next = next, effects = listOf(Effect.PersistState(next)))
            }

            SessionEvent.ConfirmSelection -> {
                if (session.activity != QuestionState.Selecting) {
                    return StepResult(
                        next = session,
                        error = GameError.InvalidStateTransition(session.toString(), event::class.simpleName ?: "?"),
                    )
                }
                if (state.draftPicks.size != question.requiredImageCount) {
                    return StepResult(
                        next = session,
                        error =
                        GameError.InvalidSelectionCount(
                            question.requiredImageCount,
                            state.draftPicks.size,
                        ),
                    )
                }
                val next = session.copy(activity = QuestionState.Finalizing)
                StepResult(
                    next = next,
                    effects =
                    listOf(
                        Effect.PersistState(next),
                        Effect.PersistPicks(
                            questionNumber = session.questionNumber,
                            participantIndex = session.activeParticipantIndex,
                            cardIds = state.draftPicks,
                            isFinal = true,
                        ),
                    ),
                )
            }

            SessionEvent.ConfirmFinal -> {
                if (state.draftPicks.size != question.requiredImageCount) {
                    return StepResult(
                        next = session,
                        error = GameError.InvalidSelectionCount(question.requiredImageCount, state.draftPicks.size),
                    )
                }
                val next = session.copy(activity = QuestionState.Discussing)
                StepResult(
                    next = next,
                    effects =
                    listOf(
                        Effect.PersistState(next),
                        Effect.PersistPicks(
                            questionNumber = session.questionNumber,
                            participantIndex = session.activeParticipantIndex,
                            cardIds = state.draftPicks,
                            isFinal = true,
                        ),
                        Effect.LogAnalytics(
                            event = "question_completed",
                            params =
                            mapOf(
                                "question_number" to session.questionNumber,
                                "participant_index" to session.activeParticipantIndex,
                                "picks_count" to question.requiredImageCount,
                            ),
                        ),
                    ),
                )
            }

            SessionEvent.EndDiscussion -> {
                val isLastParticipant = session.activeParticipantIndex >= state.participantNames.size - 1
                val next =
                    when {
                        !isLastParticipant ->
                            session.copy(
                                activeParticipantIndex = session.activeParticipantIndex + 1,
                                activity = QuestionState.ShowingPrompt,
                            )
                        session.questionNumber < Question.entries.size ->
                            SessionState.InQuestion(
                                questionNumber = session.questionNumber + 1,
                                activeParticipantIndex = 0,
                                activity = QuestionState.ShowingPrompt,
                            )
                        else -> SessionState.Summary
                    }
                StepResult(next = next, effects = listOf(Effect.PersistState(next)))
            }

            is SessionEvent.ToggleCard ->
                if (session.activity == QuestionState.Selecting) {
                    StepResult(next = session)
                } else {
                    StepResult(
                        next = session,
                        error = GameError.InvalidStateTransition(session.toString(), event::class.simpleName ?: "?"),
                    )
                }

            else ->
                StepResult(
                    next = session,
                    error = GameError.InvalidStateTransition(session.toString(), event::class.simpleName ?: "?"),
                )
        }
    }

    private fun transitionSummary(event: SessionEvent): StepResult = when (event) {
        is SessionEvent.CollectContact -> {
            val next = SessionState.CollectingContact(event.participantIndex)
            StepResult(
                next = next,
                effects =
                listOf(
                    Effect.PersistState(next),
                    Effect.PersistContact(event.participantIndex, event.info),
                ),
            )
        }
        SessionEvent.SkipContact ->
            StepResult(
                next = SessionState.Concluded,
                effects = listOf(Effect.PersistState(SessionState.Concluded)),
            )
        SessionEvent.Conclude ->
            StepResult(
                next = SessionState.Concluded,
                effects =
                listOf(
                    Effect.PersistState(SessionState.Concluded),
                    Effect.LogAnalytics(event = "session_completed", params = emptyMap()),
                ),
            )
        else ->
            StepResult(
                next = SessionState.Summary,
                error = GameError.InvalidStateTransition("Summary", event::class.simpleName ?: "?"),
            )
    }

    private fun transitionCollectingContact(
        session: SessionState.CollectingContact,
        event: SessionEvent,
        state: GameState,
    ): StepResult = when (event) {
        is SessionEvent.CollectContact -> {
            val nextIndex = session.participantIndex + 1
            val next =
                if (nextIndex >= state.participantNames.size) {
                    SessionState.Concluded
                } else {
                    SessionState.CollectingContact(nextIndex)
                }
            StepResult(
                next = next,
                effects =
                listOf(
                    Effect.PersistState(next),
                    Effect.PersistContact(event.participantIndex, event.info),
                ),
            )
        }
        SessionEvent.SkipContact -> {
            val nextIndex = session.participantIndex + 1
            if (nextIndex >= state.participantNames.size) {
                StepResult(
                    next = SessionState.Concluded,
                    effects = listOf(Effect.PersistState(SessionState.Concluded)),
                )
            } else {
                val next = SessionState.CollectingContact(nextIndex)
                StepResult(next = next, effects = listOf(Effect.PersistState(next)))
            }
        }
        SessionEvent.Conclude ->
            StepResult(
                next = SessionState.Concluded,
                effects =
                listOf(
                    Effect.PersistState(SessionState.Concluded),
                    Effect.LogAnalytics(event = "session_completed", params = emptyMap()),
                ),
            )
        else ->
            StepResult(
                next = session,
                error = GameError.InvalidStateTransition(session.toString(), event::class.simpleName ?: "?"),
            )
    }

    /**
     * A session bookmarked mid-question persists an in-progress activity (Selecting, Finalizing,
     * Discussing), but the volatile draft picks behind it are not persisted. Snap back to the
     * question prompt on resume so the user restarts that question cleanly instead of landing on
     * an empty selection.
     */
    private fun snapBackToPromptIfMidQuestion(state: SessionState): SessionState =
        if (state is SessionState.InQuestion && state.activity != QuestionState.ShowingPrompt) {
            state.copy(activity = QuestionState.ShowingPrompt)
        } else {
            state
        }

    private data class StepResult(
        val next: SessionState,
        val effects: List<Effect> = emptyList(),
        val error: GameError? = null,
    )

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    interface Factory : GameEngine.Factory {
        override fun create(sessionId: Session.Id, kind: Session.Kind, initialState: GameState): GameEngineImpl
    }
}
