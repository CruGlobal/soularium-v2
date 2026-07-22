package org.cru.soularium.ui.conversation

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.Conversation
import org.cru.soularium.domain.ConversationId
import org.cru.soularium.domain.Session
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.ports.ShareResult
import org.cru.soularium.domain.ports.Sharer
import org.cru.soularium.domain.session.QuestionActivity
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.ui.nav.ConversationScreen

@RunOnAndroidWith(AndroidJUnit4::class)
class ConversationPresenterTest {

    private val sessionId = SessionId.random()
    private val screen = ConversationScreen(sessionId, SessionKind.SOLO)
    private val navigator = FakeNavigator(screen)

    private fun presenter(
        repo: SessionRepository,
        analytics: AnalyticsTracker = NoOpAnalytics,
        sharer: Sharer = NoOpSharer,
    ) = ConversationPresenter(
        navigator = navigator,
        screen = screen,
        sessionRepository = repo,
        analytics = analytics,
        crashReporter = NoOpCrash,
        sharer = sharer,
    )

    /** Drives the presenter to its first stable state (post-bootstrap). */
    private suspend fun ReceiveTurbine<ConversationPresenter.UiState>.awaitStableState(
        predicate: (ConversationPresenter.UiState) -> Boolean,
    ): ConversationPresenter.UiState {
        var item = awaitItem()
        while (!predicate(item)) item = awaitItem()
        return item
    }

    @Test
    fun `bootstrap from NotStarted transitions to AddingParticipants and logs analytics`() = runTest {
        val analytics = RecordingAnalytics()
        presenter(FakeSessionRepository(), analytics = analytics).test {
            awaitStableState { it is ConversationPresenter.UiState.AddingParticipants }
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(
            analytics.events.any { it.first == "session_started" && it.second["kind"] == "solo" },
            "expected a session_started analytics event, got ${analytics.events}",
        )
    }

    @Test
    fun `AddParticipant updates context and persists`() = runTest {
        val repo = FakeSessionRepository()
        presenter(repo).test {
            val started = awaitStableState { it is ConversationPresenter.UiState.AddingParticipants }
            started.eventSink(ConversationPresenter.UiEvent.AddingParticipants.AddParticipant("Alice"))
            val withAlice = awaitStableState {
                (it as? ConversationPresenter.UiState.AddingParticipants)?.participantNames == listOf("Alice")
            }
            withAlice.eventSink(ConversationPresenter.UiEvent.AddingParticipants.AddParticipant("Bob"))
            val withBoth = awaitStableState {
                (it as? ConversationPresenter.UiState.AddingParticipants)?.participantNames == listOf("Alice", "Bob")
            } as ConversationPresenter.UiState.AddingParticipants
            assertEquals(listOf("Alice", "Bob"), withBoth.participantNames)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("Alice", "Bob"), repo.lastUpsertedParticipants)
    }

    @Test
    fun `ToggleCard accumulates and removes picks without invoking transition`() = runTest {
        val repo = FakeSessionRepository().apply {
            // Resume at question 3 ShowingPrompt; the test drives forward to a
            // Selection page so we can observe selectedCardIds toggling.
            preloadedState = SessionState.InQuestion(3, 0, QuestionActivity.ShowingPrompt)
            preloadedConversations[sessionId] = listOf(
                Conversation(ConversationId.random(), sessionId, 0, ContactInfo("Alice")),
            )
        }
        presenter(repo).test {
            val prompt = awaitStableState { it is ConversationPresenter.UiState.QuestionPrompt }
            prompt.eventSink(ConversationPresenter.UiEvent.QuestionPrompt.BeginSelection)
            val afterBegin = awaitStableState {
                it is ConversationPresenter.UiState.Instructions ||
                    it is ConversationPresenter.UiState.Selection
            }
            val selection = if (afterBegin is ConversationPresenter.UiState.Instructions) {
                afterBegin.eventSink(ConversationPresenter.UiEvent.Instructions.Dismiss)
                awaitStableState { it is ConversationPresenter.UiState.Selection }
            } else {
                afterBegin
            } as ConversationPresenter.UiState.Selection

            // Picking 7 (not present) adds it; the page stays on Selection.
            selection.eventSink(ConversationPresenter.UiEvent.Selection.ToggleCard(7))
            val one = awaitStableState {
                (it as? ConversationPresenter.UiState.Selection)?.selectedCardIds == listOf(7)
            } as ConversationPresenter.UiState.Selection
            one.eventSink(ConversationPresenter.UiEvent.Selection.ToggleCard(12))
            val two = awaitStableState {
                (it as? ConversationPresenter.UiState.Selection)?.selectedCardIds == listOf(7, 12)
            } as ConversationPresenter.UiState.Selection
            // Toggling 7 again removes it.
            two.eventSink(ConversationPresenter.UiEvent.Selection.ToggleCard(7))
            val final = awaitStableState {
                (it as? ConversationPresenter.UiState.Selection)?.selectedCardIds == listOf(12)
            } as ConversationPresenter.UiState.Selection
            assertEquals(listOf(12), final.selectedCardIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fresh session shows instructions then DismissInstructions reaches SelectingRound1`() = runTest {
        val repo = FakeSessionRepository().apply {
            preloadedState = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
            preloadedConversations[sessionId] = listOf(
                Conversation(ConversationId.random(), sessionId, 0, ContactInfo("Alice")),
            )
        }
        presenter(repo).test {
            val prompt = awaitStableState {
                it is ConversationPresenter.UiState.QuestionPrompt && it.participantName == "Alice"
            }
            prompt.eventSink(ConversationPresenter.UiEvent.QuestionPrompt.BeginSelection)
            awaitStableState { it is ConversationPresenter.UiState.Instructions }
                .eventSink(ConversationPresenter.UiEvent.Instructions.Dismiss)
            awaitStableState { it is ConversationPresenter.UiState.Selection }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DismissInstructions suppresses the Instructions page for the rest of the session`() = runTest {
        // After the first dismissal, BeginSelection on a fresh prompt must skip
        // the Instructions page entirely and land on SelectingRound1.
        val repo = FakeSessionRepository().apply {
            preloadedState = SessionState.InQuestion(2, 0, QuestionActivity.ShowingPrompt)
        }
        presenter(repo).test {
            val firstPrompt = awaitStableState { it is ConversationPresenter.UiState.QuestionPrompt }
            firstPrompt.eventSink(ConversationPresenter.UiEvent.QuestionPrompt.BeginSelection)
            val instructions = awaitStableState { it is ConversationPresenter.UiState.Instructions }
            instructions.eventSink(ConversationPresenter.UiEvent.Instructions.Dismiss)
            val selection = awaitStableState { it is ConversationPresenter.UiState.Selection }

            // Re-fire BeginSelection: with instructions already dismissed, the
            // presenter must skip the Instructions page entirely.
            selection.eventSink(ConversationPresenter.UiEvent.QuestionPrompt.BeginSelection)
            val seen = mutableListOf<ConversationPresenter.UiState>()
            repeat(2) {
                runCatching { seen += awaitItem() }
            }
            assertTrue(
                seen.none { it is ConversationPresenter.UiState.Instructions },
                "Instructions should not appear again after dismissal, saw $seen",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadState rehydrates from repository on init`() = runTest {
        val repo = FakeSessionRepository().apply {
            preloadedState = SessionState.InQuestion(3, 0, QuestionActivity.ShowingPrompt)
        }
        presenter(repo).test {
            val state = awaitStableState {
                it is ConversationPresenter.UiState.QuestionPrompt && it.questionNumber == 3
            } as ConversationPresenter.UiState.QuestionPrompt
            assertEquals(3, state.questionNumber)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resuming mid-question snaps the activity back to the prompt`() = runTest {
        val repo = FakeSessionRepository().apply {
            // Bookmarked mid-selection: volatile draft picks behind Finalizing
            // were never persisted, so resuming there would strand the user on
            // an empty selection screen.
            preloadedState = SessionState.InQuestion(3, 0, QuestionActivity.Finalizing)
        }
        presenter(repo).test {
            val state = awaitStableState {
                it is ConversationPresenter.UiState.QuestionPrompt && it.questionNumber == 3
            } as ConversationPresenter.UiState.QuestionPrompt
            assertEquals(3, state.questionNumber)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeSessionRepository : SessionRepository {
    var preloadedState: SessionState? = null
    val preloadedConversations = mutableMapOf<SessionId, List<Conversation>>()
    var lastUpsertedParticipants: List<String>? = null
    val persistedStates = mutableListOf<Pair<SessionId, SessionState>>()

    override suspend fun createSession(session: Session, initialState: SessionState): SessionId = session.id

    override suspend fun loadSession(id: SessionId): Session? = null

    override suspend fun loadState(id: SessionId): SessionState? = preloadedState

    override suspend fun persistState(id: SessionId, state: SessionState) {
        persistedStates += id to state
    }

    override suspend fun setBookmarked(id: SessionId, bookmarked: Boolean) = Unit

    override suspend fun setEnded(id: SessionId) = Unit

    override suspend fun upsertParticipants(sessionId: SessionId, names: List<String>): List<ConversationId> {
        lastUpsertedParticipants = names
        val existing = preloadedConversations[sessionId].orEmpty()
        val out = names.mapIndexed { idx, _ ->
            existing.getOrNull(idx)?.id ?: ConversationId.random()
        }
        preloadedConversations[sessionId] = out.mapIndexed { idx, cid ->
            Conversation(cid, sessionId, idx, ContactInfo(names[idx]))
        }
        return out
    }

    override suspend fun upsertContact(conversationId: ConversationId, info: ContactInfo) = Unit

    override suspend fun upsertPicks(
        conversationId: ConversationId,
        questionNumber: Int,
        cardIds: List<Int>,
        isFinal: Boolean,
    ) = Unit

    override suspend fun loadPicks(conversationId: ConversationId): List<CardPick> = emptyList()

    override fun observeCompletedSessions(): Flow<List<Session>> =
        MutableStateFlow<List<Session>>(emptyList()).asStateFlow()

    override fun observeBookmarkedSessions(): Flow<List<Session>> =
        MutableStateFlow<List<Session>>(emptyList()).asStateFlow()

    override suspend fun deleteSession(id: SessionId) = Unit

    override suspend fun loadConversations(sessionId: SessionId): List<Conversation> =
        preloadedConversations[sessionId].orEmpty()
}

private class RecordingAnalytics : AnalyticsTracker {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()

    override fun screenView(screenName: String) {
        events += "screen_view" to mapOf("screen_name" to screenName)
    }

    override fun event(name: String, params: Map<String, Any>) {
        events += name to params
    }
}

private object NoOpAnalytics : AnalyticsTracker {
    override fun screenView(screenName: String) = Unit
    override fun event(name: String, params: Map<String, Any>) = Unit
}

private object NoOpCrash : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, breadcrumb: String?) = Unit
    override fun setKey(key: String, value: String) = Unit
}

private object NoOpSharer : Sharer {
    override suspend fun share(text: String, subject: String?): ShareResult = ShareResult.Succeeded
}
