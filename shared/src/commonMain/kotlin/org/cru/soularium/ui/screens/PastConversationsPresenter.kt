package org.cru.soularium.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import kotlinx.coroutines.launch
import org.cru.soularium.domain.Session
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.startedAtLocalDate
import org.cru.soularium.ui.nav.ConversationScreen
import org.cru.soularium.ui.nav.PastConversationsScreen

/**
 * Flat, UI-ready representation of a single past session row.
 */
data class PastConversationItem(
    val sessionId: SessionId,
    val kind: SessionKind,
    val formattedDate: String,
    val participantNames: List<String>,
)

@AssistedInject
class PastConversationsPresenter(
    @Assisted private val navigator: Navigator,
    private val repository: SessionRepository,
    private val crashReporter: CrashReporter,
) : Presenter<PastConversationsPresenter.UiState> {

    data class UiState(
        val completed: List<PastConversationItem>,
        val bookmarked: List<PastConversationItem>,
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent
        data class Open(val sessionId: SessionId) : UiEvent
        data class Delete(val sessionId: SessionId) : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        val completedSessions by remember { repository.observeCompletedSessions() }
            .collectAsState(initial = emptyList())
        val bookmarkedSessions by remember { repository.observeBookmarkedSessions() }
            .collectAsState(initial = emptyList())

        val completed by produceState(initialValue = emptyList(), completedSessions) {
            value = completedSessions.map { it.toItem(repository) }
        }
        val bookmarked by produceState(initialValue = emptyList(), bookmarkedSessions) {
            value = bookmarkedSessions.map { it.toItem(repository) }
        }

        val all = completed + bookmarked
        return UiState(
            completed = completed,
            bookmarked = bookmarked,
        ) { event ->
            when (event) {
                UiEvent.Back -> navigator.pop()
                is UiEvent.Open -> {
                    val item = all.firstOrNull { it.sessionId == event.sessionId }
                    if (item != null) {
                        navigator.goTo(ConversationScreen(event.sessionId, item.kind))
                    }
                }
                is UiEvent.Delete -> scope.launch {
                    runCatching { repository.deleteSession(event.sessionId) }
                        .onFailure { crashReporter.recordNonFatal(it, "deleteSession") }
                }
            }
        }
    }

    private suspend fun Session.toItem(repo: SessionRepository): PastConversationItem {
        val names =
            runCatching { repo.loadConversations(id) }
                .getOrDefault(emptyList())
                .map { it.contact.name }
        return PastConversationItem(
            sessionId = id,
            kind = kind,
            formattedDate = startedAtLocalDate(),
            participantNames = names,
        )
    }

    @CircuitInject(PastConversationsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): PastConversationsPresenter
    }
}
