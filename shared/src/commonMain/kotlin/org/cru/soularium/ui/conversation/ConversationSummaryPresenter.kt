package org.cru.soularium.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.domain.DomainError
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.Sharer
import org.cru.soularium.domain.share.shareUrlFor
import org.cru.soularium.model.Session
import org.cru.soularium.ui.nav.ConversationSummaryScreen

/**
 * Read-only summary of a completed session, opened from PastConversations.
 * Collects the participant mosaic (final picks per question, per participant)
 * reactively from the repository and offers Share + Back — no state-machine
 * involvement.
 */
@AssistedInject
class ConversationSummaryPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ConversationSummaryScreen,
    private val sessionRepository: SessionRepository,
    private val sharer: Sharer,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
) : Presenter<ConversationSummaryPresenter.UiState> {

    data class UiState(
        val participants: List<ParticipantSummary>,
        val isLoading: Boolean,
        val error: DomainError?,
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data class Share(val participantIndex: Int) : UiEvent
        data object Back : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        val summaryState by remember(screen.sessionId) { summaryStateFlow(screen.sessionId) }
            .collectAsState(initial = SummaryState.Loading)

        return UiState(
            participants = (summaryState as? SummaryState.Loaded)?.participants.orEmpty(),
            isLoading = summaryState is SummaryState.Loading,
            error = (summaryState as? SummaryState.Failed)?.error,
        ) { event ->
            when (event) {
                is UiEvent.Share -> shareParticipant(event.participantIndex, scope)
                UiEvent.Back -> navigator.pop()
            }
        }
    }

    // Compose observeConversations with each conversation's observePicks. The
    // outer switch triggers whenever participants are added/removed; combine
    // rebuilds the summary list whenever any pick set changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun summaryStateFlow(sessionId: Session.Id): Flow<SummaryState> =
        sessionRepository.observeConversations(sessionId)
            .flatMapLatest { conversations ->
                if (conversations.isEmpty()) {
                    flowOf(emptyList<ParticipantSummary>())
                } else {
                    combine(
                        conversations.map { conversation ->
                            sessionRepository.observePicks(conversation.id).map { picks ->
                                ParticipantSummary(
                                    participantIndex = conversation.displayOrder,
                                    name = conversation.contact.name,
                                    cardIds = picks
                                        .filter { it.isFinal }
                                        .sortedWith(compareBy({ it.questionNumber }, { it.pickOrder }))
                                        .map { it.cardId },
                                )
                            }
                        },
                    ) { it.toList() }
                }
            }
            .map<List<ParticipantSummary>, SummaryState> { SummaryState.Loaded(it) }
            .catch { throwable ->
                crashReporter.recordNonFatal(throwable, "observeSummaries")
                emit(SummaryState.Failed(DomainError.PersistenceFailed))
            }

    private sealed interface SummaryState {
        data object Loading : SummaryState
        data class Loaded(val participants: List<ParticipantSummary>) : SummaryState
        data class Failed(val error: DomainError) : SummaryState
    }

    private fun shareParticipant(participantIndex: Int, scope: CoroutineScope) {
        scope.launch {
            runCatching {
                val conversation =
                    sessionRepository.loadConversations(screen.sessionId)
                        .firstOrNull { it.displayOrder == participantIndex }
                        ?: return@launch
                val picks = sessionRepository.loadPicks(conversation.id)
                val url = shareUrlFor(conversation, picks)
                sharer.share(text = url)
                analytics.event("share_initiated", mapOf("channel" to "other"))
            }.onFailure { crashReporter.recordNonFatal(it, "shareSummary") }
        }
    }

    @CircuitInject(ConversationSummaryScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator, screen: ConversationSummaryScreen): ConversationSummaryPresenter
    }
}
