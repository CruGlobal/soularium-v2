package org.cru.soularium.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.cru.soularium.analytics.AnalyticsTracker
import org.cru.soularium.analytics.CrashReporter
import org.cru.soularium.db.repository.FakeSessionRepository
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.game.content.Question
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState
import org.cru.soularium.model.game.SessionState.InQuestion.QuestionState

/**
 * End-to-end smoke tests for a conversation session, driven directly against the real
 * [GameEngine] + [GameEngineHostImpl] adapter and an in-memory [FakeSessionRepository] —
 * no Circuit, no presenter, no Robolectric. Covers a solo run start-to-conclude, a
 * three-person group run, bookmark-and-resume for both, and the host-level corner cases
 * that used to live in the presenter test (bootstrap analytics, resuming a session with no
 * persisted state).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameEngineFlowTest {

    private fun TestScope.engine(
        repo: SessionRepository,
        sessionId: Session.Id,
        kind: Session.Kind,
        analytics: AnalyticsTracker = SilentAnalytics,
    ): GameEngine = GameEngineImpl(
        host = GameEngineHostImpl(repo, analytics, SilentCrash),
        dispatcher = StandardTestDispatcher(testScheduler),
        sessionId = sessionId,
        kind = kind,
        initialState = GameState(),
    )

    /**
     * Drives one full turn: [SessionEvent.BeginSelection] (dismissing instructions the first
     * time they show), [SessionEvent.ToggleCard] for the question's required image count,
     * [SessionEvent.ConfirmSelection], [SessionEvent.ConfirmFinal], [SessionEvent.EndDiscussion].
     */
    private fun playTurn(engine: GameEngine) {
        engine.dispatch(SessionEvent.BeginSelection)
        val afterBegin = engine.state.value.session as SessionState.InQuestion
        if (afterBegin.activity == QuestionState.ShowingInstructions) {
            engine.dispatch(SessionEvent.DismissInstructions)
        }
        val selecting = engine.state.value.session as SessionState.InQuestion
        val required = Question.forNumber(selecting.questionNumber).requiredImageCount
        repeat(required) { engine.dispatch(SessionEvent.ToggleCard(it + 1)) }
        engine.dispatch(SessionEvent.ConfirmSelection)
        engine.dispatch(SessionEvent.ConfirmFinal)
        engine.dispatch(SessionEvent.EndDiscussion)
    }

    @Test
    fun `solo session completes from start through summary and conclude`() = runTest {
        val repo = FakeSessionRepository()
        val sessionId = Session.Id.random()
        val e = engine(repo, sessionId, Session.Kind.SOLO)

        e.start()
        e.dispatch(SessionEvent.AddParticipant("Jordan"))
        e.dispatch(SessionEvent.ConfirmParticipants)

        repeat(5) { playTurn(e) }

        assertEquals(SessionState.Summary, e.state.value.session)
        e.awaitIdle()

        val conversation = repo.loadConversations(sessionId).single()
        assertEquals(9, repo.loadPicks(conversation.id).size)

        e.dispatch(SessionEvent.Conclude)
        assertEquals(SessionState.Concluded, e.state.value.session)
    }

    @Test
    fun `group session of three completes all five questions with nine final picks each`() = runTest {
        val repo = FakeSessionRepository()
        val sessionId = Session.Id.random()
        val e = engine(repo, sessionId, Session.Kind.GROUP)

        e.start()
        listOf("Amara", "Ben", "Chen").forEach { e.dispatch(SessionEvent.AddParticipant(it)) }
        e.dispatch(SessionEvent.ConfirmParticipants)

        // Question-major: every participant answers a question before the next one begins —
        // 5 questions × 3 participants = 15 turns.
        repeat(5 * 3) { playTurn(e) }

        assertEquals(SessionState.Summary, e.state.value.session)
        e.awaitIdle()

        val conversations = repo.loadConversations(sessionId)
        assertEquals(3, conversations.size)
        conversations.forEach { conversation ->
            assertEquals(
                9,
                repo.loadPicks(conversation.id).size,
                "${conversation.contact.name} should have 9 final picks"
            )
        }
    }

    @Test
    fun `solo session bookmarks mid-question and resumes from persisted state`() = runTest {
        val repo = FakeSessionRepository()
        val sessionId = Session.Id.random()

        // First sitting: play questions 1 and 2, then bookmark at question 3.
        val first = engine(repo, sessionId, Session.Kind.SOLO)
        first.start()
        first.dispatch(SessionEvent.AddParticipant("Riley"))
        first.dispatch(SessionEvent.ConfirmParticipants)
        repeat(2) { playTurn(first) }
        first.bookmark()
        assertEquals(1, repo.bookmarkedSnapshot().size)

        // Second sitting: a fresh engine rehydrates the persisted state.
        val second = engine(repo, sessionId, Session.Kind.SOLO)
        second.start()
        val resumed = assertIs<SessionState.InQuestion>(second.state.value.session)
        assertEquals(3, resumed.questionNumber)
        assertEquals(QuestionState.ShowingPrompt, resumed.activity)
        assertEquals(listOf("Riley"), second.state.value.participantNames)
    }

    @Test
    fun `group session bookmark rehydrates participant names on resume`() = runTest {
        val repo = FakeSessionRepository()
        val sessionId = Session.Id.random()

        val first = engine(repo, sessionId, Session.Kind.GROUP)
        first.start()
        first.dispatch(SessionEvent.AddParticipant("Dana"))
        first.dispatch(SessionEvent.AddParticipant("Eli"))
        first.dispatch(SessionEvent.ConfirmParticipants)
        // Both participants finish question 1; bookmark at the start of Q2.
        repeat(2) { playTurn(first) }
        first.bookmark()

        // A fresh engine must restore the participant list, otherwise group turn
        // advancement breaks (every turn looks like the last).
        val second = engine(repo, sessionId, Session.Kind.GROUP)
        second.start()
        assertEquals(listOf("Dana", "Eli"), second.state.value.participantNames)
        val resumed = assertIs<SessionState.InQuestion>(second.state.value.session)
        assertEquals(2, resumed.questionNumber)
    }

    @Test
    fun `starting a fresh session logs session_started analytics with the session kind`() = runTest {
        val repo = FakeSessionRepository()
        val sessionId = Session.Id.random()
        val analytics = RecordingAnalytics()
        val e = engine(repo, sessionId, Session.Kind.SOLO, analytics)

        e.start()
        e.awaitIdle()

        assertTrue(
            analytics.events.any { it.first == "session_started" && it.second["kind"] == "solo" },
            "expected a session_started analytics event, got ${analytics.events}",
        )
    }

    @Test
    fun `an existing session with no persisted state restarts in place without deleting it`() = runTest {
        // Simulates a mid-upgrade resume where the persisted state_snapshot_json no longer
        // decodes in this build (e.g. a removed QuestionState variant): the repository
        // surfaces an unreadable snapshot as a null state, and the engine restarts the
        // existing session in place rather than deleting and recreating it.
        val repo = FakeSessionRepository()
        val sessionId = Session.Id.random()
        repo.seedSession(Session(id = sessionId, kind = Session.Kind.SOLO))
        val e = engine(repo, sessionId, Session.Kind.SOLO)

        e.start()

        assertEquals(SessionState.AddingParticipants, e.state.value.session)
        assertTrue(repo.deletedSessions.isEmpty(), "restart reuses the session row instead of deleting it")
    }
}

private object SilentAnalytics : AnalyticsTracker {
    override fun screenView(screenName: String) = Unit
    override fun event(name: String, params: Map<String, Any>) = Unit
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

private object SilentCrash : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, breadcrumb: String?) = Unit
    override fun setKey(key: String, value: String) = Unit
}
