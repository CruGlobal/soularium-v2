package org.cru.soularium.game

import co.touchlab.kermit.Logger
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

private val logger = Logger.withTag("GameEngine")

@AssistedInject
internal class GameEngineImpl(
    private val host: Host,
    @Assisted private val sessionId: Session.Id,
    @Assisted private val kind: Session.Kind,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    initialState: GameState = GameState(),
) : GameEngine {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val queue = Channel<QueuedOp>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<GameState> = _state.asStateFlow()

    init {
        scope.launch {
            withContext(NonCancellable) {
                for (queued in queue) {
                    runCatching { queued.op() }
                        .onFailure { logger.e(it) { queued.context() } }
                }
            }
        }.invokeOnCompletion { scope.cancel() }
    }

    override fun dispatch(event: SessionEvent) {
        val result = step(event, _state.value)
        val effects: List<Effect>
        if (result.error == null) {
            _state.value = result.next
            effects = result.effects
        } else {
            effects =
                listOf(
                    Effect.LogAnalytics(
                        "transition_error",
                        mapOf("error" to (result.error::class.simpleName ?: "unknown")),
                    ),
                )
        }
        if (effects.isEmpty()) return
        val context = { "applyEffects after $event" }
        effects.forEach { effect -> enqueue(context) { host.execute(sessionId, effect) } }
    }

    override suspend fun start() {
        val loadResult =
            runCatching { host.findSessionState(sessionId) }
                .onFailure { logger.e(it) { "findSessionState on start" } }
        val loaded = loadResult.getOrNull()
        if (loaded != null) {
            _state.update { it.copy(session = snapBackToPromptIfMidQuestion(loaded)) }
            runCatching { host.loadParticipantNames(sessionId) }
                .onSuccess { names -> if (names.isNotEmpty()) _state.update { it.copy(participantNames = names) } }
                .onFailure { logger.e(it) { "loadParticipantNames on start" } }
            runCatching { host.loadSelectionInstructionsShown(sessionId) }
                .onSuccess { shown -> if (shown) _state.update { it.copy(instructionsShown = true) } }
                .onFailure { logger.e(it) { "loadSelectionInstructionsShown on start" } }
        }
        if (_state.value.session == SessionState.NotStarted) {
            val exists =
                runCatching { host.sessionExists(sessionId) }
                    .getOrElse {
                        logger.e(it) { "sessionExists on start" }
                        false
                    }
            if (!exists || loadResult.isFailure) {
                runCatching { host.createSession(Session(id = sessionId, kind = kind), SessionState.NotStarted) }
                    .onFailure { logger.e(it) { "createSession on start" } }
            }
            dispatch(SessionEvent.StartSession(kind))
        }
    }

    override suspend fun loadSummaries(): List<GameEngine.ParticipantSummary> {
        var summaries = emptyList<GameEngine.ParticipantSummary>()
        awaitQueued("loadSummaries") { summaries = host.loadSummaries(sessionId) }
        return summaries
    }

    /** Suspends until every effect enqueued so far has finished executing against the host. */
    suspend fun awaitIdle() = awaitQueued("awaitIdle") {}

    override suspend fun bookmark() = awaitQueued("bookmarkAndExit") {
        runCatching { host.setBookmarked(sessionId, true) }
            .onFailure { logger.e(it) { "bookmarkAndExit" } }
        host.execute(sessionId, Effect.LogAnalytics("conversation_bookmarked", emptyMap()))
    }

    override suspend fun discard() = awaitQueued("discardAndExit") { host.deleteSession(sessionId) }

    override fun close() {
        queue.close() // worker drains what is already queued, then the scope dies
    }

    private fun enqueue(context: () -> String, op: suspend () -> Unit) {
        queue.trySend(QueuedOp(context, op))
    }

    /**
     * Enqueues [op] behind every effect queued so far and suspends until it has run. The caller
     * always resumes — failures thrown by [op] are reported by the worker loop, not rethrown.
     */
    private suspend fun awaitQueued(context: String, op: suspend () -> Unit) {
        val done = CompletableDeferred<Unit>()
        enqueue({ context }) {
            try {
                op()
            } finally {
                done.complete(Unit)
            }
        }
        done.await()
    }

    private class QueuedOp(val context: () -> String, val op: suspend () -> Unit)

    private fun step(event: SessionEvent, state: GameState): StepResult {
        val result = when (val session = state.session) {
            SessionState.NotStarted -> transitionNotStarted(event, state)
            SessionState.AddingParticipants -> transitionAddingParticipants(event, state)
            is SessionState.InQuestion -> transitionInQuestion(session, event, state)
            SessionState.Summary -> transitionSummary(event, state)
            is SessionState.CollectingContact -> transitionCollectingContact(session, event, state)
            SessionState.Concluded -> invalid(state, "Concluded", event)
        }
        if (result.error != null) return result
        // A turn always begins with an empty draft-pick tray.
        val next = result.next
        val startsNewTurn = (next.session as? SessionState.InQuestion)?.activity == QuestionState.ShowingPrompt
        return if (startsNewTurn && next.draftPicks.isNotEmpty()) {
            result.copy(next = next.copy(draftPicks = emptyList()))
        } else {
            result
        }
    }

    private fun transitionNotStarted(event: SessionEvent, state: GameState): StepResult = when (event) {
        is SessionEvent.StartSession ->
            StepResult(
                next = state.copy(session = SessionState.AddingParticipants),
                effects =
                listOf(
                    Effect.PersistState(SessionState.AddingParticipants),
                    Effect.LogAnalytics(
                        event = "session_started",
                        params = mapOf("kind" to event.kind.name.lowercase()),
                    ),
                ),
            )
        else -> invalid(state, "NotStarted", event)
    }

    private fun transitionAddingParticipants(event: SessionEvent, state: GameState): StepResult = when (event) {
        is SessionEvent.AddParticipant -> persistParticipants(state, state.participantNames + event.name)
        is SessionEvent.RemoveParticipant ->
            persistParticipants(
                state,
                state.participantNames.toMutableList().also {
                    if (event.index in it.indices) it.removeAt(event.index)
                },
            )
        SessionEvent.ConfirmParticipants -> {
            if (state.participantNames.isEmpty()) {
                StepResult(
                    next = state,
                    error = GameError.InvalidStateTransition("AddingParticipants", "ConfirmParticipants(empty)"),
                )
            } else {
                val next = SessionState.InQuestion(1, 0, QuestionState.ShowingPrompt)
                StepResult(
                    next = state.copy(session = next),
                    effects = listOf(Effect.PersistState(next)),
                )
            }
        }
        else -> invalid(state, "AddingParticipants", event)
    }

    private fun persistParticipants(state: GameState, names: List<String>) = StepResult(
        next = state.copy(participantNames = names),
        effects = listOf(Effect.PersistParticipants(names)),
    )

    private fun transitionInQuestion(
        session: SessionState.InQuestion,
        event: SessionEvent,
        state: GameState,
    ): StepResult {
        val question = Question.forNumber(session.questionNumber)
        return when (event) {
            SessionEvent.BeginSelection -> {
                if (session.activity != QuestionState.ShowingPrompt && session.activity != QuestionState.Finalizing) {
                    return invalid(state, session.toString(), event)
                }
                val targetActivity =
                    if (!state.instructionsShown &&
                        session.activity == QuestionState.ShowingPrompt
                    ) {
                        QuestionState.ShowingInstructions
                    } else {
                        QuestionState.Selecting
                    }
                val next = session.copy(activity = targetActivity)
                StepResult(next = state.copy(session = next), effects = listOf(Effect.PersistState(next)))
            }

            SessionEvent.DismissInstructions -> {
                if (session.activity != QuestionState.ShowingInstructions) {
                    return invalid(state, session.toString(), event)
                }
                val next = session.copy(activity = QuestionState.Selecting)
                StepResult(
                    next = state.copy(session = next, instructionsShown = true),
                    effects = listOf(Effect.PersistState(next), Effect.PersistInstructionsShown),
                )
            }

            SessionEvent.ConfirmSelection -> {
                if (session.activity != QuestionState.Selecting) {
                    return invalid(state, session.toString(), event)
                }
                if (state.draftPicks.size != question.requiredImageCount) {
                    return StepResult(
                        next = state,
                        error =
                        GameError.InvalidSelectionCount(
                            question.requiredImageCount,
                            state.draftPicks.size,
                        ),
                    )
                }
                val next = session.copy(activity = QuestionState.Finalizing)
                StepResult(
                    next = state.copy(session = next),
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
                if (session.activity != QuestionState.Finalizing) {
                    return invalid(state, session.toString(), event)
                }
                if (state.draftPicks.size != question.requiredImageCount) {
                    return StepResult(
                        next = state,
                        error = GameError.InvalidSelectionCount(question.requiredImageCount, state.draftPicks.size),
                    )
                }
                val next = session.copy(activity = QuestionState.Discussing)
                StepResult(
                    next = state.copy(session = next),
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
                if (session.activity != QuestionState.Discussing) {
                    return invalid(state, session.toString(), event)
                }
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
                StepResult(next = state.copy(session = next), effects = listOf(Effect.PersistState(next)))
            }

            is SessionEvent.ToggleCard ->
                if (session.activity == QuestionState.Selecting) {
                    val toggled =
                        if (event.cardId in state.draftPicks) {
                            state.draftPicks - event.cardId
                        } else {
                            state.draftPicks + event.cardId
                        }
                    StepResult(next = state.copy(draftPicks = toggled))
                } else {
                    invalid(state, session.toString(), event)
                }

            else -> invalid(state, session.toString(), event)
        }
    }

    private fun transitionSummary(event: SessionEvent, state: GameState): StepResult = when (event) {
        is SessionEvent.CollectContact -> {
            val next = SessionState.CollectingContact(event.participantIndex)
            StepResult(
                next = state.copy(session = next),
                effects =
                listOf(
                    Effect.PersistState(next),
                    Effect.PersistContact(event.participantIndex, event.info),
                ),
            )
        }
        SessionEvent.SkipContact ->
            StepResult(
                next = state.copy(session = SessionState.Concluded),
                effects = listOf(Effect.PersistState(SessionState.Concluded)),
            )
        SessionEvent.Conclude -> concludeResult(state)
        else -> invalid(state, "Summary", event)
    }

    private fun transitionCollectingContact(
        session: SessionState.CollectingContact,
        event: SessionEvent,
        state: GameState,
    ): StepResult {
        val nextIndex = session.participantIndex + 1
        val advanced =
            if (nextIndex >= state.participantNames.size) {
                SessionState.Concluded
            } else {
                SessionState.CollectingContact(nextIndex)
            }
        return when (event) {
            is SessionEvent.CollectContact ->
                StepResult(
                    next = state.copy(session = advanced),
                    effects =
                    listOf(
                        Effect.PersistState(advanced),
                        Effect.PersistContact(event.participantIndex, event.info),
                    ),
                )
            SessionEvent.SkipContact ->
                StepResult(
                    next = state.copy(session = advanced),
                    effects = listOf(Effect.PersistState(advanced)),
                )
            SessionEvent.Conclude -> concludeResult(state)
            else -> invalid(state, session.toString(), event)
        }
    }

    private fun concludeResult(state: GameState) = StepResult(
        next = state.copy(session = SessionState.Concluded),
        effects =
        listOf(
            Effect.PersistState(SessionState.Concluded),
            Effect.LogAnalytics(event = "session_completed", params = emptyMap()),
        ),
    )

    private fun invalid(state: GameState, from: String, event: SessionEvent) = StepResult(
        next = state,
        error = GameError.InvalidStateTransition(from, event::class.simpleName ?: "?"),
    )

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

    /**
     * A pure transition's outcome: the full [next] engine state, the [effects] to run against
     * the host, or the [error] that leaves state unchanged.
     */
    private data class StepResult(
        val next: GameState,
        val effects: List<Effect> = emptyList(),
        val error: GameError? = null,
    )

    @AssistedFactory
    @ContributesBinding(AppScope::class)
    interface Factory : GameEngine.Factory {
        override fun create(sessionId: Session.Id, kind: Session.Kind): GameEngineImpl
    }

    /**
     * Everything a running engine may do to the outside world — rehydration reads,
     * effect and session-lifecycle writes, and telemetry; implemented over
     * [SessionRepository][org.cru.soularium.db.repository.SessionRepository] and the
     * analytics ports.
     */
    interface Host {
        suspend fun findSessionState(id: Session.Id): SessionState?

        suspend fun loadSelectionInstructionsShown(id: Session.Id): Boolean

        suspend fun loadParticipantNames(id: Session.Id): List<String>

        suspend fun loadSummaries(id: Session.Id): List<GameEngine.ParticipantSummary>

        suspend fun sessionExists(id: Session.Id): Boolean

        suspend fun createSession(session: Session, initialState: SessionState)

        suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean)

        suspend fun deleteSession(id: Session.Id)

        suspend fun execute(id: Session.Id, effect: Effect)
    }
}
