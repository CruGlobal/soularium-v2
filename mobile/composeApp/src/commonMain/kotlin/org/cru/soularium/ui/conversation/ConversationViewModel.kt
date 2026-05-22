package org.cru.soularium.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
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

data class ConversationUiContext(
    val participantNames: List<String> = emptyList(),
    val draftPicks: List<Int> = emptyList(),
    val roundFinals: List<Int> = emptyList(),
    val instructionsShown: Boolean = false,
)

class ConversationViewModel(
    private val sessionId: SessionId,
    private val sessionRepository: SessionRepository,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val sharer: Sharer,
) : ViewModel() {
    private val _state = MutableStateFlow<SessionState>(SessionState.NotStarted)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _ui = MutableStateFlow(ConversationUiContext())
    val ui: StateFlow<ConversationUiContext> = _ui.asStateFlow()

    private val _summaries = MutableStateFlow<List<ParticipantSummary>>(emptyList())
    val summaries: StateFlow<List<ParticipantSummary>> = _summaries.asStateFlow()

    private val initialLoad: Job =
        viewModelScope.launch {
            runCatching {
                val loaded = sessionRepository.loadState(sessionId) ?: return@runCatching
                _state.value = loaded
                // Rehydrate participant names so a resumed (e.g. bookmarked)
                // group session can still advance turns — the transition
                // function reads participant count from ConversationUiContext.
                val names =
                    sessionRepository.loadConversations(sessionId)
                        .sortedBy { it.displayOrder }
                        .map { it.contact.name }
                if (names.isNotEmpty()) {
                    _ui.update { ui -> ui.copy(participantNames = names) }
                }
            }.onFailure { crashReporter.recordNonFatal(it, "loadState on init") }
        }

    /**
     * Bootstraps a brand-new conversation: if (after the initial load settles)
     * the session has no persisted state, the session row is created and the
     * state machine is started. A no-op for a resumed/bookmarked session.
     */
    fun ensureStarted(kind: SessionKind) {
        viewModelScope.launch {
            initialLoad.join()
            if (_state.value != SessionState.NotStarted) return@launch
            // The session row can already exist while _state is still
            // NotStarted — e.g. its persisted snapshot failed to decode during
            // initialLoad. createSession upserts, so recreating it here would
            // clobber the real row and orphan its conversations/picks. Only
            // bootstrap a genuinely new session; on any load error, bail
            // rather than risk overwriting existing data.
            val existing =
                runCatching { sessionRepository.loadSession(sessionId) }
                    .getOrElse {
                        crashReporter.recordNonFatal(it, "loadSession in ensureStarted")
                        return@launch
                    }
            if (existing != null) return@launch
            runCatching {
                sessionRepository.createSession(
                    session = newSession(sessionId, kind),
                    initialState = SessionState.NotStarted,
                )
            }.onFailure { crashReporter.recordNonFatal(it, "createSession") }
            dispatch(SessionEvent.StartSession(kind))
        }
    }

    fun dispatch(event: SessionEvent) {
        // PickCard/UnpickCard mutate draft state without invoking the pure transition.
        when (event) {
            is SessionEvent.PickCard -> {
                _ui.update { it.copy(draftPicks = it.draftPicks + event.cardId) }
                return
            }
            is SessionEvent.UnpickCard -> {
                _ui.update { it.copy(draftPicks = it.draftPicks - event.cardId) }
                return
            }
            else -> Unit
        }

        val previousState = _state.value
        val ctx =
            SessionContext(
                participantNames = _ui.value.participantNames,
                currentDraftPicks = _ui.value.draftPicks,
                currentRoundFinalPicks = _ui.value.roundFinals,
                showInstructionsForThisSession = !_ui.value.instructionsShown,
            )
        val result = transition(previousState, event, ctx)

        if (result.error != null) {
            analytics.event(
                name = "transition_error",
                params = mapOf("error" to (result.error?.let { it::class.simpleName } ?: "unknown")),
            )
            return
        }

        if (event is SessionEvent.DismissInstructions) {
            _ui.update { it.copy(instructionsShown = true) }
        }

        _state.value = result.next
        resetDraftIfNeeded(previous = previousState, next = result.next)
        if (result.next == SessionState.Summary) loadSummaries()

        viewModelScope.launch {
            runCatching { applyEffects(result.effects) }
                .onFailure { crashReporter.recordNonFatal(it, "applyEffects after $event") }
        }
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
    ) {
        val prevQ = previous as? SessionState.InQuestion
        val nextQ = next as? SessionState.InQuestion
        when {
            nextQ?.activity == QuestionActivity.ShowingPrompt -> {
                _ui.update { it.copy(draftPicks = emptyList(), roundFinals = emptyList()) }
            }
            prevQ?.activity == QuestionActivity.SelectingRound1 &&
                nextQ?.activity == QuestionActivity.SelectingRound2 -> {
                _ui.update { it.copy(roundFinals = it.draftPicks, draftPicks = emptyList()) }
            }
            else -> Unit
        }
    }

    /** Loads each participant's final 9 picks for the Summary screen. */
    fun loadSummaries() {
        viewModelScope.launch {
            runCatching {
                sessionRepository.loadConversations(sessionId).map { conversation ->
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
            }.onSuccess { _summaries.value = it }
                .onFailure { crashReporter.recordNonFatal(it, "loadSummaries") }
        }
    }

    /** Builds the share URL for one participant and hands it to the platform sharer. */
    fun shareSummary(participantIndex: Int) {
        viewModelScope.launch {
            runCatching {
                val conversation =
                    sessionRepository.loadConversations(sessionId)
                        .firstOrNull { it.displayOrder == participantIndex }
                        ?: return@launch
                val picks = sessionRepository.loadPicks(conversation.id)
                val url = shareUrlFor(conversation, picks)
                sharer.share(text = url)
                analytics.event("share_initiated", mapOf("channel" to "other"))
            }.onFailure { crashReporter.recordNonFatal(it, "shareSummary") }
        }
    }

    /**
     * Bookmarks the session, then invokes [onComplete] once the write has
     * settled. [onComplete] is called from inside the persisting coroutine so
     * the bookmark is durable before the caller navigates away (which clears
     * this ViewModel and cancels [viewModelScope]).
     */
    fun bookmarkAndExit(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { sessionRepository.setBookmarked(sessionId, true) }
                .onFailure { crashReporter.recordNonFatal(it, "bookmarkAndExit") }
            analytics.event("conversation_bookmarked", emptyMap())
            onComplete()
        }
    }

    private suspend fun applyEffects(effects: List<Effect>) {
        for (effect in effects) {
            when (effect) {
                is Effect.PersistState ->
                    sessionRepository.persistState(sessionId, effect.state)
                is Effect.PersistParticipants -> {
                    _ui.update { it.copy(participantNames = effect.names) }
                    sessionRepository.upsertParticipants(sessionId, effect.names)
                }
                is Effect.PersistPicks -> {
                    val conversations = sessionRepository.loadConversations(sessionId)
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
                    val conversations = sessionRepository.loadConversations(sessionId)
                    val convId =
                        conversations.firstOrNull { it.displayOrder == effect.participantIndex }?.id
                            ?: continue
                    sessionRepository.upsertContact(convId, effect.info)
                }
                is Effect.PersistBookmark ->
                    sessionRepository.setBookmarked(sessionId, effect.bookmark)
                is Effect.LogAnalytics ->
                    analytics.event(effect.event, effect.params)
            }
        }
    }
}
