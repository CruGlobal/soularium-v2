package org.cru.soularium.ui.past

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.startedAtLocalDate

/**
 * Flat, UI-ready representation of a single past session row.
 *
 * @param sessionId        the session's identifier.
 * @param kind             SOLO or GROUP session.
 * @param formattedDate    pre-formatted start date string (e.g. "2024-03-15").
 * @param participantNames names of every participant in the session.
 */
data class PastConversationItem(
    val sessionId: SessionId,
    val kind: SessionKind,
    val formattedDate: String,
    val participantNames: List<String>,
)

/**
 * ViewModel for the Past Conversations screen.
 *
 * Observes completed and bookmarked sessions from [SessionRepository] and
 * enriches each one with participant names by calling [SessionRepository.loadConversations].
 *
 * Date formatting uses [org.cru.soularium.domain.startedAtLocalDate], a domain-layer
 * extension that converts each session's start [kotlinx.datetime.Instant] to a local
 * "YYYY-MM-DD" string.
 *
 * @param repository    the [SessionRepository] used to load and delete sessions.
 * @param crashReporter records non-fatal failures, e.g. a failed delete.
 */
class PastConversationsViewModel(
    private val repository: SessionRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {

    private val _completed = MutableStateFlow<List<PastConversationItem>>(emptyList())
    val completed: StateFlow<List<PastConversationItem>> = _completed.asStateFlow()

    private val _bookmarked = MutableStateFlow<List<PastConversationItem>>(emptyList())
    val bookmarked: StateFlow<List<PastConversationItem>> = _bookmarked.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCompletedSessions().collect { sessions ->
                _completed.value =
                    sessions.map { session ->
                        val names =
                            runCatching { repository.loadConversations(session.id) }
                                .getOrDefault(emptyList())
                                .map { it.contact.name }
                        PastConversationItem(
                            sessionId = session.id,
                            kind = session.kind,
                            formattedDate = session.startedAtLocalDate(),
                            participantNames = names,
                        )
                    }
            }
        }
        viewModelScope.launch {
            repository.observeBookmarkedSessions().collect { sessions ->
                _bookmarked.value =
                    sessions.map { session ->
                        val names =
                            runCatching { repository.loadConversations(session.id) }
                                .getOrDefault(emptyList())
                                .map { it.contact.name }
                        PastConversationItem(
                            sessionId = session.id,
                            kind = session.kind,
                            formattedDate = session.startedAtLocalDate(),
                            participantNames = names,
                        )
                    }
            }
        }
    }

    /**
     * Deletes the session with [sessionId] from the repository.
     * Fire-and-forget; the repository observables will emit an updated list.
     */
    fun delete(sessionId: SessionId) {
        viewModelScope.launch {
            runCatching { repository.deleteSession(sessionId) }
                .onFailure { crashReporter.recordNonFatal(it, "deleteSession") }
        }
    }
}
