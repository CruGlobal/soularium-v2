package org.cru.soularium.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.domain.newSession
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.ports.Sharer
import org.cru.soularium.domain.session.Effect
import org.cru.soularium.domain.session.QuestionActivity
import org.cru.soularium.domain.session.SessionContext
import org.cru.soularium.domain.session.SessionEvent
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.domain.session.transition
import org.cru.soularium.domain.share.shareUrlFor
import org.cru.soularium.ui.nav.ConversationScreen

private const val TOTAL_QUESTIONS = 5

/**
 * Volatile per-conversation UI context that doesn't belong on [SessionState]:
 * participant names (loaded into memory), draft / pending picks while the user
 * is selecting cards, and a one-shot flag that the per-session instruction
 * panel has been seen.
 */
data class ConversationUiContext(
    val participantNames: List<String> = emptyList(),
    val draftPicks: List<Int> = emptyList(),
    val roundFinals: List<Int> = emptyList(),
    val instructionsShown: Boolean = false,
)

@AssistedInject
class ConversationPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ConversationScreen,
    private val sessionRepository: SessionRepository,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val sharer: Sharer,
) : Presenter<ConversationPresenter.UiState> {

    data class UiState(
        val sessionState: SessionState,
        val ui: ConversationUiContext,
        val summaries: List<ParticipantSummary>,
        val showExitDialog: Boolean,
        val subState: SubState,
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    /**
     * Render-ready projection of the session for the layout. Each variant carries
     * the [UiState] of exactly one sub-Layout, with its callbacks already wired
     * to the Presenter's event sink — the Layout only has to dispatch on type.
     */
    sealed interface SubState {
        /** Shown while the session is bootstrapping (NotStarted) or winding down (Concluded). */
        data object Loading : SubState
        data class AddingParticipants(val state: AddParticipantsUiState) : SubState
        data class QuestionPrompt(val state: QuestionPromptUiState) : SubState
        data class Instructions(val state: InstructionPanelUiState) : SubState
        data class Selecting(val state: SelectionUiState) : SubState
        data class Finalizing(val state: FinalizingUiState) : SubState
        data class Discussing(val state: DiscussingUiState) : SubState
        data class Summary(val state: SummaryUiState) : SubState
        data class CollectingContact(val state: ContactCollectionUiState) : SubState
    }

    sealed interface UiEvent : CircuitUiEvent {
        /** Domain session event from a subscreen (begin selection, pick card, etc.). */
        data class Dispatch(val event: SessionEvent) : UiEvent

        /** Platform back / explicit exit affordance — open the bookmark/discard dialog. */
        data object RequestExit : UiEvent
        data object DismissExitDialog : UiEvent
        data object BookmarkAndExit : UiEvent
        data object DiscardAndExit : UiEvent

        /** Share / contact actions raised from the Summary subscreen. */
        data class Share(val participantIndex: Int) : UiEvent
        data class CollectContact(val participantIndex: Int, val info: ContactInfo) : UiEvent
        data object SkipContact : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        // The repository serializer: keeps effect application from one event
        // from interleaving with another's effects on the shared DAOs.
        val repoMutex = remember { Mutex() }

        var sessionState by remember { mutableStateOf<SessionState>(SessionState.NotStarted) }
        var ui by remember { mutableStateOf(ConversationUiContext()) }
        var summaries by remember { mutableStateOf(emptyList<ParticipantSummary>()) }
        var showExitDialog by remember { mutableStateOf(false) }
        var bootstrapped by remember { mutableStateOf(false) }

        // Initial load + ensureStarted in one effect: load any persisted state,
        // rehydrate participant names, and bootstrap if this is a brand-new
        // session id.
        LaunchedEffect(screen.sessionId) {
            runCatching {
                val loaded = sessionRepository.loadState(screen.sessionId)
                if (loaded != null) {
                    sessionState = snapBackToPromptIfMidQuestion(loaded)
                    val names =
                        sessionRepository.loadConversations(screen.sessionId)
                            .sortedBy { it.displayOrder }
                            .map { it.contact.name }
                    if (names.isNotEmpty()) {
                        ui = ui.copy(participantNames = names)
                    }
                }
            }.onFailure { crashReporter.recordNonFatal(it, "loadState on init") }

            if (!bootstrapped) {
                bootstrapped = true
                if (sessionState == SessionState.NotStarted) {
                    val existing =
                        runCatching { sessionRepository.loadSession(screen.sessionId) }
                            .getOrElse {
                                crashReporter.recordNonFatal(it, "loadSession in ensureStarted")
                                null
                            }
                    if (existing == null) {
                        runCatching {
                            sessionRepository.createSession(
                                session = newSession(screen.sessionId, screen.kind),
                                initialState = SessionState.NotStarted,
                            )
                        }.onFailure { crashReporter.recordNonFatal(it, "createSession") }
                        val (newState, newUi) = applyDispatch(
                            event = SessionEvent.StartSession(screen.kind),
                            previousState = sessionState,
                            ui = ui,
                            scope = scope,
                            repoMutex = repoMutex,
                        )
                        sessionState = newState
                        ui = newUi
                    }
                }
            }
        }

        // If we land on Summary (either fresh or via load), populate summaries.
        LaunchedEffect(sessionState) {
            if (sessionState == SessionState.Summary) {
                runCatching {
                    repoMutex.withLock { loadSummaries() }
                }.onSuccess { summaries = it }
                    .onFailure { crashReporter.recordNonFatal(it, "loadSummaries") }
            }
            if (sessionState == SessionState.Concluded) {
                navigator.pop()
            }
        }

        val dispatchEvent: (SessionEvent) -> Unit = { domainEvent ->
            // Captured by the sub-Layout callbacks below; goes back through the
            // same Dispatch path as inline `state.eventSink(...)` calls.
            val (newState, newUi) = applyDispatch(
                event = domainEvent,
                previousState = sessionState,
                ui = ui,
                scope = scope,
                repoMutex = repoMutex,
            )
            sessionState = newState
            ui = newUi
        }

        val subState = projectSubState(
            sessionState = sessionState,
            ui = ui,
            summaries = summaries,
            dispatch = dispatchEvent,
            onShare = { index -> shareSummary(index, scope, repoMutex) },
            onCollectContact = { index, info ->
                val (newState, newUi) = applyDispatch(
                    event = SessionEvent.CollectContact(index, info),
                    previousState = sessionState,
                    ui = ui,
                    scope = scope,
                    repoMutex = repoMutex,
                )
                sessionState = newState
                ui = newUi
            },
            onSkipContact = {
                val (newState, newUi) = applyDispatch(
                    event = SessionEvent.SkipContact,
                    previousState = sessionState,
                    ui = ui,
                    scope = scope,
                    repoMutex = repoMutex,
                )
                sessionState = newState
                ui = newUi
            },
        )

        return UiState(
            sessionState = sessionState,
            ui = ui,
            summaries = summaries,
            showExitDialog = showExitDialog,
            subState = subState,
        ) { event ->
            when (event) {
                is UiEvent.Dispatch -> {
                    val (newState, newUi) = applyDispatch(
                        event = event.event,
                        previousState = sessionState,
                        ui = ui,
                        scope = scope,
                        repoMutex = repoMutex,
                    )
                    sessionState = newState
                    ui = newUi
                }
                UiEvent.RequestExit -> if (sessionState != SessionState.Concluded) {
                    showExitDialog = true
                }
                UiEvent.DismissExitDialog -> showExitDialog = false
                UiEvent.BookmarkAndExit -> {
                    showExitDialog = false
                    scope.launch {
                        runCatching {
                            repoMutex.withLock {
                                sessionRepository.setBookmarked(screen.sessionId, true)
                            }
                        }.onFailure { crashReporter.recordNonFatal(it, "bookmarkAndExit") }
                        analytics.event("conversation_bookmarked", emptyMap())
                        navigator.pop()
                    }
                }
                UiEvent.DiscardAndExit -> {
                    showExitDialog = false
                    scope.launch {
                        runCatching {
                            repoMutex.withLock {
                                sessionRepository.deleteSession(screen.sessionId)
                            }
                        }.onFailure { crashReporter.recordNonFatal(it, "discardAndExit") }
                        navigator.pop()
                    }
                }
                is UiEvent.Share -> shareSummary(event.participantIndex, scope, repoMutex)
                is UiEvent.CollectContact -> {
                    val (newState, newUi) = applyDispatch(
                        event = SessionEvent.CollectContact(event.participantIndex, event.info),
                        previousState = sessionState,
                        ui = ui,
                        scope = scope,
                        repoMutex = repoMutex,
                    )
                    sessionState = newState
                    ui = newUi
                }
                UiEvent.SkipContact -> {
                    val (newState, newUi) = applyDispatch(
                        event = SessionEvent.SkipContact,
                        previousState = sessionState,
                        ui = ui,
                        scope = scope,
                        repoMutex = repoMutex,
                    )
                    sessionState = newState
                    ui = newUi
                }
            }
        }
    }

    /**
     * Runs the pure transition function for [event], applies its effects
     * asynchronously, and returns the new (state, ui) pair to assign. PickCard
     * and UnpickCard mutate only the draft, bypassing the transition.
     */
    private fun applyDispatch(
        event: SessionEvent,
        previousState: SessionState,
        ui: ConversationUiContext,
        scope: CoroutineScope,
        repoMutex: Mutex,
    ): Pair<SessionState, ConversationUiContext> {
        when (event) {
            is SessionEvent.PickCard -> {
                return previousState to ui.copy(draftPicks = ui.draftPicks + event.cardId)
            }
            is SessionEvent.UnpickCard -> {
                return previousState to ui.copy(draftPicks = ui.draftPicks - event.cardId)
            }
            else -> Unit
        }

        val ctx =
            SessionContext(
                participantNames = ui.participantNames,
                currentDraftPicks = ui.draftPicks,
                currentRoundFinalPicks = ui.roundFinals,
                showInstructionsForThisSession = !ui.instructionsShown,
            )
        val result = transition(previousState, event, ctx)

        if (result.error != null) {
            analytics.event(
                name = "transition_error",
                params = mapOf("error" to (result.error?.let { it::class.simpleName } ?: "unknown")),
            )
            return previousState to ui
        }

        var nextUi = ui
        if (event is SessionEvent.DismissInstructions) {
            nextUi = nextUi.copy(instructionsShown = true)
        }
        // Apply effect-driven ui changes synchronously so callers see the new
        // participant list in the same emission as the state transition. Repo
        // persistence still happens asynchronously below.
        for (effect in result.effects) {
            if (effect is Effect.PersistParticipants) {
                nextUi = nextUi.copy(participantNames = effect.names)
            }
        }
        nextUi = resetDraftIfNeeded(previous = previousState, next = result.next, ui = nextUi)

        scope.launch {
            runCatching { repoMutex.withLock { applyEffects(result.effects) } }
                .onFailure { crashReporter.recordNonFatal(it, "applyEffects after $event") }
        }
        return result.next to nextUi
    }

    /**
     * Draft picks are kept all the way through the Finalizing and Discussing
     * activities so those screens can display them; they are cleared only when
     * a fresh turn begins (a new ShowingPrompt). Round 1 picks are shifted into
     * [ConversationUiContext.roundFinals] when narrowing to round 2.
     */
    private fun resetDraftIfNeeded(
        previous: SessionState,
        next: SessionState,
        ui: ConversationUiContext,
    ): ConversationUiContext {
        val prevQ = previous as? SessionState.InQuestion
        val nextQ = next as? SessionState.InQuestion
        return when {
            nextQ?.activity == QuestionActivity.ShowingPrompt ->
                ui.copy(draftPicks = emptyList(), roundFinals = emptyList())
            prevQ?.activity == QuestionActivity.SelectingRound1 &&
                nextQ?.activity == QuestionActivity.SelectingRound2 ->
                ui.copy(roundFinals = ui.draftPicks, draftPicks = emptyList())
            else -> ui
        }
    }

    /** Loads each participant's final 9 picks for the Summary screen. */
    private suspend fun loadSummaries(): List<ParticipantSummary> = sessionRepository.loadConversations(screen.sessionId).map { conversation ->
        val cardIds =
            sessionRepository.loadPicks(conversation.id)
                .filter { it.isFinal }
                .sortedWith(compareBy({ it.questionNumber }, { it.pickOrder }))
                .map { it.cardId }
        ParticipantSummary(
            participantIndex = conversation.displayOrder,
            name = conversation.contact.name,
            cardIds = cardIds,
        )
    }

    private suspend fun applyEffects(effects: List<Effect>) {
        for (effect in effects) {
            when (effect) {
                is Effect.PersistState ->
                    sessionRepository.persistState(screen.sessionId, effect.state)
                is Effect.PersistParticipants -> {
                    sessionRepository.upsertParticipants(screen.sessionId, effect.names)
                }
                is Effect.PersistPicks -> {
                    val conversations = sessionRepository.loadConversations(screen.sessionId)
                    val convId =
                        conversations.firstOrNull { it.displayOrder == effect.participantIndex }?.id
                            ?: continue
                    sessionRepository.upsertPicks(
                        conversationId = convId,
                        questionNumber = effect.questionNumber,
                        cardIds = effect.cardIds,
                        isFinal = effect.isFinal,
                    )
                }
                is Effect.PersistContact -> {
                    val conversations = sessionRepository.loadConversations(screen.sessionId)
                    val convId =
                        conversations.firstOrNull { it.displayOrder == effect.participantIndex }?.id
                            ?: continue
                    sessionRepository.upsertContact(convId, effect.info)
                }
                is Effect.PersistBookmark ->
                    sessionRepository.setBookmarked(screen.sessionId, effect.bookmark)
                is Effect.LogAnalytics ->
                    analytics.event(effect.event, effect.params)
            }
        }
    }

    /**
     * Looks up the participant's conversation and final picks, builds the share
     * URL, and hands it to the platform sharer. Errors are reported as
     * non-fatals and swallowed — share is best-effort.
     */
    private fun shareSummary(
        participantIndex: Int,
        scope: CoroutineScope,
        repoMutex: Mutex,
    ) {
        scope.launch {
            runCatching {
                val url =
                    repoMutex.withLock {
                        val conversation =
                            sessionRepository.loadConversations(screen.sessionId)
                                .firstOrNull { it.displayOrder == participantIndex }
                                ?: return@withLock null
                        val picks = sessionRepository.loadPicks(conversation.id)
                        shareUrlFor(conversation, picks)
                    } ?: return@launch
                sharer.share(text = url)
                analytics.event("share_initiated", mapOf("channel" to "other"))
            }.onFailure { crashReporter.recordNonFatal(it, "shareSummary") }
        }
    }

    @CircuitInject(ConversationScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator, screen: ConversationScreen): ConversationPresenter
    }
}

/**
 * Maps the canonical session state + UI context onto a render-ready
 * [ConversationPresenter.SubState]. All callbacks needed by sub-Layouts are
 * wired here so the Layout itself is a pure dispatch table.
 *
 * NotStarted and Concluded both project to [ConversationPresenter.SubState.Loading]
 * — the first is transient while bootstrap runs, the second while the
 * navigator pops back.
 */
private fun projectSubState(
    sessionState: SessionState,
    ui: ConversationUiContext,
    summaries: List<ParticipantSummary>,
    dispatch: (SessionEvent) -> Unit,
    onShare: (Int) -> Unit,
    onCollectContact: (Int, ContactInfo) -> Unit,
    onSkipContact: () -> Unit,
): ConversationPresenter.SubState = when (sessionState) {
    SessionState.NotStarted, SessionState.Concluded -> ConversationPresenter.SubState.Loading

    SessionState.AddingParticipants -> ConversationPresenter.SubState.AddingParticipants(
        AddParticipantsUiState(
            participantNames = ui.participantNames,
            onAddParticipant = { dispatch(SessionEvent.AddParticipant(it)) },
            onRemoveParticipant = { dispatch(SessionEvent.RemoveParticipant(it)) },
            onConfirm = { dispatch(SessionEvent.ConfirmParticipants) },
        ),
    )

    is SessionState.InQuestion -> {
        val question = Questions.byNumber(sessionState.questionNumber)
        val participantName =
            ui.participantNames.getOrElse(sessionState.activeParticipantIndex) { "" }
        when (sessionState.activity) {
            QuestionActivity.ShowingPrompt -> ConversationPresenter.SubState.QuestionPrompt(
                QuestionPromptUiState(
                    questionNumber = sessionState.questionNumber,
                    totalQuestions = TOTAL_QUESTIONS,
                    participantName = participantName,
                    isGroup = ui.participantNames.size > 1,
                    onBegin = { dispatch(SessionEvent.BeginSelection) },
                ),
            )

            QuestionActivity.ShowingInstructions -> ConversationPresenter.SubState.Instructions(
                InstructionPanelUiState(
                    onDismiss = { dispatch(SessionEvent.DismissInstructions) },
                ),
            )

            QuestionActivity.SelectingRound1,
            QuestionActivity.SelectingRound2,
            -> ConversationPresenter.SubState.Selecting(
                SelectionUiState(
                    questionNumber = sessionState.questionNumber,
                    round = if (sessionState.activity == QuestionActivity.SelectingRound2) 2 else 1,
                    selectedCardIds = ui.draftPicks,
                    isConfirmEnabled = isSelectionValid(
                        question = question,
                        activity = sessionState.activity,
                        count = ui.draftPicks.size,
                    ),
                    onToggleCard = { cardId ->
                        if (cardId in ui.draftPicks) {
                            dispatch(SessionEvent.UnpickCard(cardId))
                        } else {
                            dispatch(SessionEvent.PickCard(cardId))
                        }
                    },
                    onConfirm = { dispatch(SessionEvent.ConfirmSelection) },
                ),
            )

            QuestionActivity.Finalizing -> ConversationPresenter.SubState.Finalizing(
                FinalizingUiState(
                    questionNumber = sessionState.questionNumber,
                    cardIds = ui.draftPicks,
                    onConfirm = { dispatch(SessionEvent.ConfirmFinal) },
                    onChangeSelection = { dispatch(SessionEvent.BeginSelection) },
                ),
            )

            QuestionActivity.Discussing -> ConversationPresenter.SubState.Discussing(
                DiscussingUiState(
                    questionNumber = sessionState.questionNumber,
                    participantName = participantName,
                    cardIds = ui.draftPicks,
                    onDone = { dispatch(SessionEvent.EndDiscussion) },
                ),
            )
        }
    }

    SessionState.Summary -> ConversationPresenter.SubState.Summary(
        SummaryUiState(
            participants = summaries,
            onShare = onShare,
            onAddContact = { index ->
                val name = ui.participantNames.getOrElse(index) { "" }
                onCollectContact(index, ContactInfo(name))
            },
            onDone = { dispatch(SessionEvent.Conclude) },
        ),
    )

    is SessionState.CollectingContact -> ConversationPresenter.SubState.CollectingContact(
        ContactCollectionUiState(
            participantName = ui.participantNames.getOrElse(sessionState.participantIndex) { "" },
            onSave = { onCollectContact(sessionState.participantIndex, it) },
            onSkip = onSkipContact,
        ),
    )
}

/**
 * Mirrors the count rules enforced by the transition function: round 1 of a
 * two-round question needs a wide set, every other round needs exactly the
 * required count.
 */
private fun isSelectionValid(
    question: org.cru.soularium.domain.content.Question,
    activity: QuestionActivity,
    count: Int,
): Boolean = when (activity) {
    QuestionActivity.SelectingRound1 ->
        if (question.selectionRounds == 2) {
            count >= question.requiredImageCount + 1
        } else {
            count == question.requiredImageCount
        }
    QuestionActivity.SelectingRound2 -> count == question.requiredImageCount
    else -> false
}

/**
 * A session bookmarked mid-question persists an in-progress activity
 * (SelectingRound1/2, Finalizing, Discussing), but the volatile draft picks
 * behind it are not persisted. Snap back to the question prompt on resume so
 * the user restarts that question cleanly instead of landing on an empty
 * selection.
 */
private fun snapBackToPromptIfMidQuestion(state: SessionState): SessionState = if (state is SessionState.InQuestion && state.activity != QuestionActivity.ShowingPrompt) {
    state.copy(activity = QuestionActivity.ShowingPrompt)
} else {
    state
}
