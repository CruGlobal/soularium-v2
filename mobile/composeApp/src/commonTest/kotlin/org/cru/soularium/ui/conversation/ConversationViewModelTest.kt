package org.cru.soularium.ui.conversation

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dispatch StartSession transitions to AddingParticipants and logs analytics`() =
        runTest(testDispatcher) {
            val repo = FakeSessionRepository()
            val analytics = RecordingAnalytics()
            val vm =
                ConversationViewModel(
                    sessionId = SessionId.random(),
                    sessionRepository = repo,
                    analytics = analytics,
                    crashReporter = NoOpCrash,
                    sharer = NoOpSharer,
                )

            vm.state.test {
                assertEquals(SessionState.NotStarted, awaitItem())
                vm.dispatch(SessionEvent.StartSession(SessionKind.SOLO))
                assertEquals(SessionState.AddingParticipants, awaitItem())
            }

            assertTrue(
                analytics.events.any { it.first == "session_started" && it.second["kind"] == "solo" },
                "expected a session_started analytics event, got ${analytics.events}",
            )
        }

    @Test
    fun `dispatch AddParticipant updates context and persists`() =
        runTest(testDispatcher) {
            val repo = FakeSessionRepository()
            val vm =
                ConversationViewModel(
                    sessionId = SessionId.random(),
                    sessionRepository = repo,
                    analytics = NoOpAnalytics,
                    crashReporter = NoOpCrash,
                    sharer = NoOpSharer,
                )

            vm.dispatch(SessionEvent.StartSession(SessionKind.SOLO))
            vm.dispatch(SessionEvent.AddParticipant("Alice"))
            vm.dispatch(SessionEvent.AddParticipant("Bob"))
            advanceUntilIdle()

            assertEquals(listOf("Alice", "Bob"), vm.ui.value.participantNames)
            assertEquals(listOf("Alice", "Bob"), repo.lastUpsertedParticipants)
        }

    @Test
    fun `PickCard and UnpickCard mutate draft without invoking transition`() =
        runTest(testDispatcher) {
            val repo = FakeSessionRepository()
            val vm =
                ConversationViewModel(
                    sessionId = SessionId.random(),
                    sessionRepository = repo,
                    analytics = NoOpAnalytics,
                    crashReporter = NoOpCrash,
                    sharer = NoOpSharer,
                )

            vm.dispatch(SessionEvent.PickCard(7))
            vm.dispatch(SessionEvent.PickCard(12))
            vm.dispatch(SessionEvent.UnpickCard(7))
            advanceUntilIdle()

            assertEquals(listOf(12), vm.ui.value.draftPicks)
        }

    @Test
    fun `fresh session shows instructions then BeginSelection-Dismiss reaches SelectingRound1`() =
        runTest(StandardTestDispatcher()) {
            val sessionId = SessionId.random()
            val repo =
                FakeSessionRepository().apply {
                    preloadedState = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
                    preloadedConversations[sessionId] =
                        listOf(
                            Conversation(ConversationId.random(), sessionId, 0, ContactInfo("Alice")),
                        )
                }
            val vm =
                ConversationViewModel(
                    sessionId = sessionId,
                    sessionRepository = repo,
                    analytics = NoOpAnalytics,
                    crashReporter = NoOpCrash,
                    sharer = NoOpSharer,
                )
            advanceUntilIdle()

            // First time this session: BeginSelection routes through the help panel.
            vm.dispatch(SessionEvent.BeginSelection)
            advanceUntilIdle()
            assertEquals(
                QuestionActivity.ShowingInstructions,
                (vm.state.value as SessionState.InQuestion).activity,
            )

            vm.dispatch(SessionEvent.DismissInstructions)
            advanceUntilIdle()
            assertEquals(
                QuestionActivity.SelectingRound1,
                (vm.state.value as SessionState.InQuestion).activity,
            )
        }

    @Test
    fun `DismissInstructions marks instructions shown for the rest of the session`() =
        runTest(StandardTestDispatcher()) {
            val sessionId = SessionId.random()
            val repo =
                FakeSessionRepository().apply {
                    preloadedState = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
                }
            val vm =
                ConversationViewModel(
                    sessionId = sessionId,
                    sessionRepository = repo,
                    analytics = NoOpAnalytics,
                    crashReporter = NoOpCrash,
                    sharer = NoOpSharer,
                )
            advanceUntilIdle()

            assertFalse(vm.ui.value.instructionsShown)
            vm.dispatch(SessionEvent.BeginSelection)
            vm.dispatch(SessionEvent.DismissInstructions)
            advanceUntilIdle()
            assertTrue(vm.ui.value.instructionsShown)
        }

    @Test
    fun `loadState rehydrates from repository on init`() =
        runTest(StandardTestDispatcher()) {
            val sessionId = SessionId.random()
            val repo =
                FakeSessionRepository().apply {
                    preloadedState = SessionState.InQuestion(3, 0, QuestionActivity.Finalizing)
                }
            val vm =
                ConversationViewModel(
                    sessionId = sessionId,
                    sessionRepository = repo,
                    analytics = NoOpAnalytics,
                    crashReporter = NoOpCrash,
                    sharer = NoOpSharer,
                )
            advanceUntilIdle()

            assertEquals(SessionState.InQuestion(3, 0, QuestionActivity.Finalizing), vm.state.value)
        }
}

private class FakeSessionRepository : SessionRepository {
    var preloadedState: SessionState? = null
    val preloadedConversations = mutableMapOf<SessionId, List<Conversation>>()
    var lastUpsertedParticipants: List<String>? = null
    val persistedStates = mutableListOf<Pair<SessionId, SessionState>>()

    override suspend fun createSession(
        session: Session,
        initialState: SessionState,
    ): SessionId = session.id

    override suspend fun loadSession(id: SessionId): Session? = null

    override suspend fun loadState(id: SessionId): SessionState? = preloadedState

    override suspend fun persistState(
        id: SessionId,
        state: SessionState,
    ) {
        persistedStates += id to state
    }

    override suspend fun setBookmarked(
        id: SessionId,
        bookmarked: Boolean,
    ) = Unit

    override suspend fun setEnded(id: SessionId) = Unit

    override suspend fun upsertParticipants(
        sessionId: SessionId,
        names: List<String>,
    ): List<ConversationId> {
        lastUpsertedParticipants = names
        val existing = preloadedConversations[sessionId].orEmpty()
        val out =
            names.mapIndexed { idx, n ->
                existing.getOrNull(idx)?.id ?: ConversationId.random()
            }
        preloadedConversations[sessionId] =
            out.mapIndexed { idx, cid ->
                Conversation(cid, sessionId, idx, ContactInfo(names[idx]))
            }
        return out
    }

    override suspend fun upsertContact(
        conversationId: ConversationId,
        info: ContactInfo,
    ) = Unit

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

    override fun event(
        name: String,
        params: Map<String, Any>,
    ) {
        events += name to params
    }
}

private object NoOpAnalytics : AnalyticsTracker {
    override fun screenView(screenName: String) = Unit

    override fun event(
        name: String,
        params: Map<String, Any>,
    ) = Unit
}

private object NoOpCrash : CrashReporter {
    override fun recordNonFatal(
        throwable: Throwable,
        breadcrumb: String?,
    ) = Unit

    override fun setKey(
        key: String,
        value: String,
    ) = Unit
}

private object NoOpSharer : Sharer {
    override suspend fun share(
        text: String,
        subject: String?,
    ): ShareResult = ShareResult.Succeeded
}
