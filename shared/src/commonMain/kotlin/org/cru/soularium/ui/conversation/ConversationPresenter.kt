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
internal data class ConversationUiContext(
    val participantNames: List<String> = emptyList(),
    val draftPicks: List<Int> = emptyList(),
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

    /**
     * One subtype per page the conversation flow can render. Each subtype carries
     * exactly the props the matching screen needs, so [ConversationLayout]'s only
     * branching is the `when` over this sealed hierarchy.
     */
    sealed interface UiState : CircuitUiState {
        val showExitDialog: Boolean
        val eventSink: (UiEvent) -> Unit

        /** Transient placeholder shown while bootstrapping or popping. */
        data class Loading(override val showExitDialog: Boolean, override val eventSink: (UiEvent) -> Unit) : UiState

        data class AddingParticipants(
            val participantNames: List<String>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class QuestionPrompt(
            val questionNumber: Int,
            val totalQuestions: Int,
            val participantName: String,
            val isGroup: Boolean,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class Instructions(override val showExitDialog: Boolean, override val eventSink: (UiEvent) -> Unit) :
            UiState

        data class Selection(
            val questionNumber: Int,
            val selectedCardIds: List<Int>,
            val isConfirmEnabled: Boolean,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class Finalizing(
            val questionNumber: Int,
            val cardIds: List<Int>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class Discussing(
            val questionNumber: Int,
            val participantName: String,
            val cardIds: List<Int>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class Summary(
            val participants: List<ParticipantSummary>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class CollectingContact(
            val participantName: String,
            val participantIndex: Int,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState
    }

    /**
     * Sealed event hierarchy. Top-level entries are emittable from any page
     * (the back/exit-dialog affordances). Page-specific events are grouped into
     * nested sealed interfaces named after their owning [UiState] subtype, so
     * each subscreen has its own narrow vocabulary of events.
     */
    sealed interface UiEvent : CircuitUiEvent {
        /** Platform back / explicit exit affordance — open the bookmark/discard dialog. */
        data object RequestExit : UiEvent
        data object DismissExitDialog : UiEvent
        data object BookmarkAndExit : UiEvent
        data object DiscardAndExit : UiEvent

        sealed interface AddingParticipants : UiEvent {
            data class AddParticipant(val name: String) : AddingParticipants
            data class RemoveParticipant(val index: Int) : AddingParticipants
            data object Confirm : AddingParticipants
        }

        sealed interface QuestionPrompt : UiEvent {
            data object BeginSelection : QuestionPrompt
        }

        sealed interface Instructions : UiEvent {
            data object Dismiss : Instructions
        }

        sealed interface Selection : UiEvent {
            /** Tap a card — the presenter decides pick vs. unpick. */
            data class ToggleCard(val cardId: Int) : Selection
            data object Confirm : Selection
        }

        sealed interface Finalizing : UiEvent {
            data object Confirm : Finalizing

            /** Re-open the selection round with the current picks intact. */
            data object ChangeSelection : Finalizing
        }

        sealed interface Discussing : UiEvent {
            data object Done : Discussing
        }

        sealed interface Summary : UiEvent {
            data class Share(val participantIndex: Int) : Summary

            /** Start collecting this participant's contact info. */
            data class StartCollectingContact(val participantIndex: Int) : Summary
            data object Done : Summary
        }

        sealed interface CollectingContact : UiEvent {
            data class Save(val info: ContactInfo) : CollectingContact
            data object Skip : CollectingContact
        }
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

        fun dispatch(sessionEvent: SessionEvent) {
            val (newState, newUi) = applyDispatch(
                event = sessionEvent,
                previousState = sessionState,
                ui = ui,
                scope = scope,
                repoMutex = repoMutex,
            )
            sessionState = newState
            ui = newUi
        }

        val eventSink: (UiEvent) -> Unit = { event ->
            when (event) {
                // Page-specific events
                is UiEvent.AddingParticipants.AddParticipant ->
                    dispatch(SessionEvent.AddParticipant(event.name))
                is UiEvent.AddingParticipants.RemoveParticipant ->
                    dispatch(SessionEvent.RemoveParticipant(event.index))
                UiEvent.AddingParticipants.Confirm ->
                    dispatch(SessionEvent.ConfirmParticipants)

                UiEvent.QuestionPrompt.BeginSelection ->
                    dispatch(SessionEvent.BeginSelection)

                UiEvent.Instructions.Dismiss ->
                    dispatch(SessionEvent.DismissInstructions)

                is UiEvent.Selection.ToggleCard -> {
                    // Draft picks are volatile UI state — toggle directly without
                    // round-tripping through the pure transition function.
                    ui = if (event.cardId in ui.draftPicks) {
                        ui.copy(draftPicks = ui.draftPicks - event.cardId)
                    } else {
                        ui.copy(draftPicks = ui.draftPicks + event.cardId)
                    }
                }
                UiEvent.Selection.Confirm ->
                    dispatch(SessionEvent.ConfirmSelection)

                UiEvent.Finalizing.Confirm ->
                    dispatch(SessionEvent.ConfirmFinal)
                UiEvent.Finalizing.ChangeSelection ->
                    dispatch(SessionEvent.BeginSelection)

                UiEvent.Discussing.Done ->
                    dispatch(SessionEvent.EndDiscussion)

                is UiEvent.Summary.Share -> shareParticipant(event.participantIndex, scope, repoMutex)
                is UiEvent.Summary.StartCollectingContact -> {
                    val name = ui.participantNames.getOrElse(event.participantIndex) { "" }
                    dispatch(SessionEvent.CollectContact(event.participantIndex, ContactInfo(name)))
                }
                UiEvent.Summary.Done ->
                    dispatch(SessionEvent.Conclude)

                is UiEvent.CollectingContact.Save -> {
                    val current = sessionState as? SessionState.CollectingContact
                    if (current != null) {
                        dispatch(SessionEvent.CollectContact(current.participantIndex, event.info))
                    }
                }
                UiEvent.CollectingContact.Skip ->
                    dispatch(SessionEvent.SkipContact)

                // Global events
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
            }
        }

        return buildUiState(sessionState, ui, summaries, showExitDialog, eventSink)
    }

    /**
     * Projects the presenter's internal state onto the page-specific [UiState]
     * subtype. All the branching that used to live in the Layout (question
     * lookup, participant name resolution, round numbering, selection-count
     * validity) is resolved here.
     */
    private fun buildUiState(
        sessionState: SessionState,
        ui: ConversationUiContext,
        summaries: List<ParticipantSummary>,
        showExitDialog: Boolean,
        eventSink: (UiEvent) -> Unit,
    ): UiState = when (sessionState) {
        SessionState.NotStarted, SessionState.Concluded ->
            UiState.Loading(showExitDialog, eventSink)

        SessionState.AddingParticipants ->
            UiState.AddingParticipants(ui.participantNames, showExitDialog, eventSink)

        is SessionState.InQuestion -> {
            val question = Questions.byNumber(sessionState.questionNumber)
            val participantName =
                ui.participantNames.getOrElse(sessionState.activeParticipantIndex) { "" }
            when (sessionState.activity) {
                QuestionActivity.ShowingPrompt ->
                    UiState.QuestionPrompt(
                        questionNumber = sessionState.questionNumber,
                        totalQuestions = TOTAL_QUESTIONS,
                        participantName = participantName,
                        isGroup = ui.participantNames.size > 1,
                        showExitDialog = showExitDialog,
                        eventSink = eventSink,
                    )

                QuestionActivity.ShowingInstructions ->
                    UiState.Instructions(showExitDialog, eventSink)

                QuestionActivity.Selecting ->
                    UiState.Selection(
                        questionNumber = sessionState.questionNumber,
                        selectedCardIds = ui.draftPicks,
                        isConfirmEnabled = ui.draftPicks.size == question.requiredImageCount,
                        showExitDialog = showExitDialog,
                        eventSink = eventSink,
                    )

                QuestionActivity.Finalizing ->
                    UiState.Finalizing(
                        questionNumber = sessionState.questionNumber,
                        cardIds = ui.draftPicks,
                        showExitDialog = showExitDialog,
                        eventSink = eventSink,
                    )

                QuestionActivity.Discussing ->
                    UiState.Discussing(
                        questionNumber = sessionState.questionNumber,
                        participantName = participantName,
                        cardIds = ui.draftPicks,
                        showExitDialog = showExitDialog,
                        eventSink = eventSink,
                    )
            }
        }

        SessionState.Summary ->
            UiState.Summary(summaries, showExitDialog, eventSink)

        is SessionState.CollectingContact ->
            UiState.CollectingContact(
                participantName = ui.participantNames.getOrElse(sessionState.participantIndex) { "" },
                participantIndex = sessionState.participantIndex,
                showExitDialog = showExitDialog,
                eventSink = eventSink,
            )
    }

    /**
     * Runs the pure transition function for [event] and applies its effects
     * asynchronously. Returns the new (state, ui) pair to assign.
     */
    private fun applyDispatch(
        event: SessionEvent,
        previousState: SessionState,
        ui: ConversationUiContext,
        scope: CoroutineScope,
        repoMutex: Mutex,
    ): Pair<SessionState, ConversationUiContext> {
        val ctx =
            SessionContext(
                participantNames = ui.participantNames,
                currentDraftPicks = ui.draftPicks,
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
        nextUi = resetDraftIfNeeded(next = result.next, ui = nextUi)

        scope.launch {
            runCatching { repoMutex.withLock { applyEffects(result.effects) } }
                .onFailure { crashReporter.recordNonFatal(it, "applyEffects after $event") }
        }
        return result.next to nextUi
    }

    private fun shareParticipant(participantIndex: Int, scope: CoroutineScope, repoMutex: Mutex) {
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

    /**
     * Draft picks are kept all the way through the Finalizing and Discussing
     * activities so those screens can display them; they are cleared only when
     * a fresh turn begins (a new ShowingPrompt).
     */
    private fun resetDraftIfNeeded(next: SessionState, ui: ConversationUiContext): ConversationUiContext {
        val nextQ = next as? SessionState.InQuestion
        return if (nextQ?.activity == QuestionActivity.ShowingPrompt) {
            ui.copy(draftPicks = emptyList())
        } else {
            ui
        }
    }

    /** Loads each participant's final 9 picks for the Summary screen. */
    private suspend fun loadSummaries(): List<ParticipantSummary> =
        sessionRepository.loadConversations(screen.sessionId).map { conversation ->
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
                is Effect.LogAnalytics ->
                    analytics.event(effect.event, effect.params)
            }
        }
    }

    @CircuitInject(ConversationScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator, screen: ConversationScreen): ConversationPresenter
    }
}

/**
 * A session bookmarked mid-question persists an in-progress activity
 * (Selecting, Finalizing, Discussing), but the volatile draft picks
 * behind it are not persisted. Snap back to the question prompt on resume so
 * the user restarts that question cleanly instead of landing on an empty
 * selection.
 */
private fun snapBackToPromptIfMidQuestion(state: SessionState): SessionState =
    if (state is SessionState.InQuestion && state.activity != QuestionActivity.ShowingPrompt) {
        state.copy(activity = QuestionActivity.ShowingPrompt)
    } else {
        state
    }
