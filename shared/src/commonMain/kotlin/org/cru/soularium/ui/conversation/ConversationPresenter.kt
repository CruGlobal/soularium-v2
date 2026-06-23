package org.cru.soularium.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cru.soularium.domain.ContactInfo
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

class ConversationPresenter(
    private val navigator: Navigator,
    private val screen: ConversationScreen,
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
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

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

        return UiState(
            sessionState = sessionState,
            ui = ui,
            summaries = summaries,
            showExitDialog = showExitDialog,
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
                is UiEvent.Share -> {
                    scope.launch {
                        runCatching {
                            val url =
                                repoMutex.withLock {
                                    val conversation =
                                        sessionRepository.loadConversations(screen.sessionId)
                                            .firstOrNull { it.displayOrder == event.participantIndex }
                                            ?: return@withLock null
                                    val picks = sessionRepository.loadPicks(conversation.id)
                                    shareUrlFor(conversation, picks)
                                } ?: return@launch
                            sharer.share(text = url)
                            analytics.event("share_initiated", mapOf("channel" to "other"))
                        }.onFailure { crashReporter.recordNonFatal(it, "shareSummary") }
                    }
                }
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
