package org.cru.soularium.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.model.Session
import org.cru.soularium.ui.nav.ConversationSummaryScreen

private val logger = Logger.withTag("ConversationSummaryPresenter")

/**
 * Read-only summary of a completed session, opened from PastConversations.
 * Collects each participant's picks broken down per question reactively from
 * the repository — no state-machine involvement. Back returns to Past
 * Conversations.
 */
@AssistedInject
class ConversationSummaryPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ConversationSummaryScreen,
    private val sessionRepository: SessionRepository,
) : Presenter<ConversationSummaryPresenter.UiState> {

    data class UiState(
        val participants: List<ParticipantSummary>,
        val isLoading: Boolean,
        val loadFailed: Boolean,
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val summaryState by remember(screen.sessionId) { summaryStateFlow(screen.sessionId) }
            .collectAsState(initial = SummaryState.Loading)

        return UiState(
            participants = (summaryState as? SummaryState.Loaded)?.participants.orEmpty(),
            isLoading = summaryState is SummaryState.Loading,
            loadFailed = summaryState is SummaryState.Failed,
        ) { event ->
            when (event) {
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
                                    selections = picks.toQuestionSelections(),
                                )
                            }
                        },
                    ) { it.toList() }
                }
            }
            .map<List<ParticipantSummary>, SummaryState> { SummaryState.Loaded(it) }
            .catch { throwable ->
                logger.e(throwable) { "observeSummaries" }
                emit(SummaryState.Failed)
            }

    private sealed interface SummaryState {
        data object Loading : SummaryState
        data class Loaded(val participants: List<ParticipantSummary>) : SummaryState
        data object Failed : SummaryState
    }

    @CircuitInject(ConversationSummaryScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator, screen: ConversationSummaryScreen): ConversationSummaryPresenter
    }
}
