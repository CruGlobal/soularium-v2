package org.cru.soularium.ui.conversation

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
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
import org.cru.soularium.domain.session.SessionEvent
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.ui.nav.ConversationScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
            awaitStableState { it.sessionState == SessionState.AddingParticipants }
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
            val started = awaitStableState { it.sessionState == SessionState.AddingParticipants }
            started.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.AddParticipant("Alice")))
            val withAlice = awaitStableState { it.ui.participantNames == listOf("Alice") }
            withAlice.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.AddParticipant("Bob")))
            val withBoth = awaitStableState { it.ui.participantNames == listOf("Alice", "Bob") }
            assertEquals(listOf("Alice", "Bob"), withBoth.ui.participantNames)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("Alice", "Bob"), repo.lastUpsertedParticipants)
    }

    @Test
    fun `PickCard and UnpickCard mutate draft without invoking transition`() = runTest {
        val repo = FakeSessionRepository()
        presenter(repo).test {
            val started = awaitStableState { it.sessionState == SessionState.AddingParticipants }
            started.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.PickCard(7)))
            val one = awaitStableState { it.ui.draftPicks == listOf(7) }
            one.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.PickCard(12)))
            val two = awaitStableState { it.ui.draftPicks == listOf(7, 12) }
            two.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.UnpickCard(7)))
            val final = awaitStableState { it.ui.draftPicks == listOf(12) }
            assertEquals(listOf(12), final.ui.draftPicks)
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
                (it.sessionState as? SessionState.InQuestion)?.activity == QuestionActivity.ShowingPrompt &&
                    it.ui.participantNames == listOf("Alice")
            }
            prompt.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.BeginSelection))
            awaitStableState {
                (it.sessionState as? SessionState.InQuestion)?.activity == QuestionActivity.ShowingInstructions
            }.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.DismissInstructions))
            val round1 = awaitStableState {
                (it.sessionState as? SessionState.InQuestion)?.activity == QuestionActivity.SelectingRound1
            }
            assertEquals(QuestionActivity.SelectingRound1, (round1.sessionState as SessionState.InQuestion).activity)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DismissInstructions marks instructions shown for the rest of the session`() = runTest {
        val repo = FakeSessionRepository().apply {
            preloadedState = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
        }
        presenter(repo).test {
            val initial = awaitStableState {
                it.sessionState is SessionState.InQuestion
            }
            assertFalse(initial.ui.instructionsShown)
            initial.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.BeginSelection))
            awaitStableState {
                (it.sessionState as? SessionState.InQuestion)?.activity == QuestionActivity.ShowingInstructions
            }.eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.DismissInstructions))
            val shown = awaitStableState { it.ui.instructionsShown }
            assertTrue(shown.ui.instructionsShown)
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
                it.sessionState == SessionState.InQuestion(3, 0, QuestionActivity.ShowingPrompt)
            }
            assertEquals(SessionState.InQuestion(3, 0, QuestionActivity.ShowingPrompt), state.sessionState)
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
                it.sessionState == SessionState.InQuestion(3, 0, QuestionActivity.ShowingPrompt)
            }
            assertEquals(SessionState.InQuestion(3, 0, QuestionActivity.ShowingPrompt), state.sessionState)
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

    override fun observeCompletedSessions(): Flow<List<Session>> = MutableStateFlow<List<Session>>(emptyList()).asStateFlow()

    override fun observeBookmarkedSessions(): Flow<List<Session>> = MutableStateFlow<List<Session>>(emptyList()).asStateFlow()

    override suspend fun deleteSession(id: SessionId) = Unit

    override suspend fun loadConversations(sessionId: SessionId): List<Conversation> = preloadedConversations[sessionId].orEmpty()
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
