package org.cru.soularium.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.session.Effect
import org.cru.soularium.domain.session.QuestionActivity
import org.cru.soularium.domain.session.SessionContext
import org.cru.soularium.domain.session.SessionEvent
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.domain.session.transition

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
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.NotStarted)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _ui = MutableStateFlow(ConversationUiContext())
    val ui: StateFlow<ConversationUiContext> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { sessionRepository.loadState(sessionId) }
                .onFailure { crashReporter.recordNonFatal(it, "loadState on init") }
                .getOrNull()
                ?.let { _state.value = it }
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
        val ctx = SessionContext(
            participantNames = _ui.value.participantNames,
            currentDraftPicks = _ui.value.draftPicks,
            currentRoundFinalPicks = _ui.value.roundFinals,
            showInstructionsForThisSession = !_ui.value.instructionsShown,
        )
        val result = transition(previousState, event, ctx)

        if (result.error != null) {
            analytics.event(
                name = "transition_error",
                params = mapOf("error" to (result.error?.toString() ?: "unknown")),
            )
            return
        }

        if (event is SessionEvent.DismissInstructions) {
            _ui.update { it.copy(instructionsShown = true) }
        }

        _state.value = result.next
        resetDraftIfNeeded(previous = previousState, next = result.next)

        viewModelScope.launch {
            runCatching { applyEffects(result.effects) }
                .onFailure { crashReporter.recordNonFatal(it, "applyEffects after $event") }
        }
    }

    private fun resetDraftIfNeeded(previous: SessionState, next: SessionState) {
        val prevQ = previous as? SessionState.InQuestion
        val nextQ = next as? SessionState.InQuestion
        when {
            prevQ != null && nextQ != null && prevQ.questionNumber != nextQ.questionNumber -> {
                _ui.update { it.copy(draftPicks = emptyList(), roundFinals = emptyList()) }
            }
            prevQ?.activity == QuestionActivity.SelectingRound1 &&
                nextQ?.activity == QuestionActivity.SelectingRound2 -> {
                _ui.update { it.copy(roundFinals = it.draftPicks, draftPicks = emptyList()) }
            }
            prevQ != null && nextQ?.activity == QuestionActivity.Discussing -> {
                _ui.update { it.copy(draftPicks = emptyList(), roundFinals = emptyList()) }
            }
            else -> Unit
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
                    val convId = conversations.getOrNull(effect.participantIndex)?.id ?: continue
                    sessionRepository.upsertPicks(
                        conversationId = convId,
                        questionNumber = effect.questionNumber,
                        cardIds = effect.cardIds,
                        isFinal = effect.isFinal,
                    )
                }
                is Effect.PersistContact -> {
                    val conversations = sessionRepository.loadConversations(sessionId)
                    val convId = conversations.getOrNull(effect.participantIndex)?.id ?: continue
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
