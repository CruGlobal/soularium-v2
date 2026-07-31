package org.cru.soularium.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState
import org.cru.soularium.model.game.SessionState.InQuestion.QuestionState

@OptIn(ExperimentalCoroutinesApi::class)
class GameEngineTest {
    private val sessionId = Session.Id.random()

    private fun TestScope.engine(
        host: FakeGameEngineHost = FakeGameEngineHost(),
        initial: GameState? = null,
        kind: Session.Kind = Session.Kind.SOLO,
    ): GameEngineImpl = GameEngineImpl(
        host = host,
        dispatcher = StandardTestDispatcher(testScheduler),
        sessionId = sessionId,
        kind = kind,
        initialState = initial ?: GameState(),
    )

    private fun inQuestion(q: Int = 1, participant: Int = 0, phase: QuestionState = QuestionState.ShowingPrompt) =
        SessionState.InQuestion(questionNumber = q, activeParticipantIndex = participant, activity = phase)

    // --- NotStarted ---

    @Test
    fun `NotStarted plus StartSession to AddingParticipants`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.NotStarted))
        e.dispatch(SessionEvent.StartSession(Session.Kind.SOLO))
        assertEquals(SessionState.AddingParticipants, e.state.value.session)
        advanceUntilIdle()
        assertTrue(host.executed.any { it is Effect.PersistState && it.state == SessionState.AddingParticipants })
    }

    @Test
    fun `NotStarted plus other event is invalid`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.NotStarted))
        e.dispatch(SessionEvent.ConfirmParticipants)
        assertEquals(SessionState.NotStarted, e.state.value.session) // state unchanged
        advanceUntilIdle()
        val analytics = host.executed.filterIsInstance<Effect.LogAnalytics>().single()
        assertEquals("transition_error", analytics.event)
    }

    // --- AddingParticipants ---

    @Test
    fun `AddParticipant persists growing list`() = runTest {
        val host = FakeGameEngineHost()
        val e =
            engine(
                host,
                initial = GameState(session = SessionState.AddingParticipants, participantNames = listOf("Alice")),
            )
        e.dispatch(SessionEvent.AddParticipant("Bob"))
        assertEquals(SessionState.AddingParticipants, e.state.value.session)
        advanceUntilIdle()
        val persist = host.executed.filterIsInstance<Effect.PersistParticipants>().single()
        assertEquals(listOf("Alice", "Bob"), persist.names)
    }

    @Test
    fun `RemoveParticipant drops by index`() = runTest {
        val host = FakeGameEngineHost()
        val e =
            engine(
                host,
                initial =
                GameState(session = SessionState.AddingParticipants, participantNames = listOf("Alice", "Bob")),
            )
        e.dispatch(SessionEvent.RemoveParticipant(0))
        advanceUntilIdle()
        val persist = host.executed.filterIsInstance<Effect.PersistParticipants>().single()
        assertEquals(listOf("Bob"), persist.names)
    }

    @Test
    fun `ConfirmParticipants with names advances to InQuestion 1 ShowingPrompt`() = runTest {
        val e =
            engine(
                initial = GameState(session = SessionState.AddingParticipants, participantNames = listOf("Alice")),
            )
        e.dispatch(SessionEvent.ConfirmParticipants)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(1, next.questionNumber)
        assertEquals(0, next.activeParticipantIndex)
        assertEquals(QuestionState.ShowingPrompt, next.activity)
    }

    @Test
    fun `ConfirmParticipants empty errors`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.dispatch(SessionEvent.ConfirmParticipants)
        assertEquals(SessionState.AddingParticipants, e.state.value.session)
        advanceUntilIdle()
        val analytics = host.executed.filterIsInstance<Effect.LogAnalytics>().single()
        assertEquals("transition_error", analytics.event)
    }

    // --- InQuestion: BeginSelection ---

    @Test
    fun `BeginSelection from ShowingPrompt without instructions goes to Selecting`() = runTest {
        val e = engine(initial = GameState(session = inQuestion(), instructionsShown = true))
        e.dispatch(SessionEvent.BeginSelection)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.Selecting, next.activity)
    }

    @Test
    fun `BeginSelection from ShowingPrompt with instructions goes to ShowingInstructions`() = runTest {
        val e = engine(initial = GameState(session = inQuestion(), instructionsShown = false))
        e.dispatch(SessionEvent.BeginSelection)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.ShowingInstructions, next.activity)
    }

    @Test
    fun `DismissInstructions to Selecting`() = runTest {
        val e = engine(initial = GameState(session = inQuestion(phase = QuestionState.ShowingInstructions)))
        e.dispatch(SessionEvent.DismissInstructions)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.Selecting, next.activity)
    }

    @Test
    fun `BeginSelection from Finalizing returns to Selecting with picks intact`() = runTest {
        val e = engine(initial = GameState(session = inQuestion(phase = QuestionState.Finalizing)))
        e.dispatch(SessionEvent.BeginSelection)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.Selecting, next.activity)
    }

    // --- InQuestion: ConfirmSelection ---

    @Test
    fun `Q1 ConfirmSelection with exactly 3 picks goes to Finalizing`() = runTest {
        val host = FakeGameEngineHost()
        val e =
            engine(
                host,
                initial = GameState(
                    session = inQuestion(phase = QuestionState.Selecting),
                    draftPicks = listOf(1, 2, 3)
                ),
            )
        e.dispatch(SessionEvent.ConfirmSelection)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.Finalizing, next.activity)
        advanceUntilIdle()
        val picks = host.executed.filterIsInstance<Effect.PersistPicks>().single()
        assertEquals(true, picks.isFinal)
    }

    @Test
    fun `Q1 ConfirmSelection with wrong count errors`() = runTest {
        val host = FakeGameEngineHost()
        val s = inQuestion(phase = QuestionState.Selecting)
        val e = engine(host, initial = GameState(session = s, draftPicks = listOf(1, 2)))
        e.dispatch(SessionEvent.ConfirmSelection)
        assertEquals(s, e.state.value.session)
        advanceUntilIdle()
        val analytics = host.executed.filterIsInstance<Effect.LogAnalytics>().single()
        assertEquals("transition_error", analytics.event)
    }

    @Test
    fun `Q3 ConfirmSelection with 1 pick goes to Finalizing`() = runTest {
        val host = FakeGameEngineHost()
        val e =
            engine(
                host,
                initial =
                GameState(session = inQuestion(q = 3, phase = QuestionState.Selecting), draftPicks = listOf(7)),
            )
        e.dispatch(SessionEvent.ConfirmSelection)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.Finalizing, next.activity)
        advanceUntilIdle()
        val picks = host.executed.filterIsInstance<Effect.PersistPicks>().single()
        assertEquals(true, picks.isFinal)
    }

    @Test
    fun `Q3 ConfirmSelection with zero picks errors`() = runTest {
        val host = FakeGameEngineHost()
        val s = inQuestion(q = 3, phase = QuestionState.Selecting)
        val e = engine(host, initial = GameState(session = s, draftPicks = emptyList()))
        e.dispatch(SessionEvent.ConfirmSelection)
        assertEquals(s, e.state.value.session)
        advanceUntilIdle()
        val analytics = host.executed.filterIsInstance<Effect.LogAnalytics>().single()
        assertEquals("transition_error", analytics.event)
    }

    // --- InQuestion: ConfirmFinal ---

    @Test
    fun `ConfirmFinal with valid count advances to Discussing`() = runTest {
        val host = FakeGameEngineHost()
        val e =
            engine(
                host,
                initial =
                GameState(session = inQuestion(q = 2, phase = QuestionState.Finalizing), draftPicks = listOf(1, 2, 3)),
            )
        e.dispatch(SessionEvent.ConfirmFinal)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.Discussing, next.activity)
        advanceUntilIdle()
        assertEquals(
            1,
            host.executed.filterIsInstance<Effect.LogAnalytics>().count { it.event == "question_completed" },
        )
    }

    @Test
    fun `ConfirmFinal with wrong count errors`() = runTest {
        val host = FakeGameEngineHost()
        val s = inQuestion(q = 2, phase = QuestionState.Finalizing)
        val e = engine(host, initial = GameState(session = s, draftPicks = listOf(1, 2)))
        e.dispatch(SessionEvent.ConfirmFinal)
        assertEquals(s, e.state.value.session)
        advanceUntilIdle()
        val analytics = host.executed.filterIsInstance<Effect.LogAnalytics>().single()
        assertEquals("transition_error", analytics.event)
    }

    // --- InQuestion: EndDiscussion ---

    @Test
    fun `EndDiscussion to next participant within Q1`() = runTest {
        val e =
            engine(
                initial =
                GameState(
                    session = inQuestion(phase = QuestionState.Discussing),
                    participantNames = listOf("Alice", "Bob"),
                ),
            )
        e.dispatch(SessionEvent.EndDiscussion)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(1, next.questionNumber)
        assertEquals(1, next.activeParticipantIndex)
        assertEquals(QuestionState.ShowingPrompt, next.activity)
    }

    @Test
    fun `EndDiscussion last participant of Q1 advances to Q2`() = runTest {
        val e =
            engine(
                initial =
                GameState(session = inQuestion(phase = QuestionState.Discussing), participantNames = listOf("Alice")),
            )
        e.dispatch(SessionEvent.EndDiscussion)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(2, next.questionNumber)
        assertEquals(0, next.activeParticipantIndex)
        assertEquals(QuestionState.ShowingPrompt, next.activity)
    }

    @Test
    fun `EndDiscussion last participant of Q5 advances to Summary`() = runTest {
        val e =
            engine(
                initial =
                GameState(
                    session = inQuestion(q = 5, phase = QuestionState.Discussing),
                    participantNames = listOf("Alice"),
                ),
            )
        e.dispatch(SessionEvent.EndDiscussion)
        assertEquals(SessionState.Summary, e.state.value.session)
    }

    // --- Summary, CollectingContact, Concluded ---

    @Test
    fun `Summary plus CollectContact advances to CollectingContact`() = runTest {
        val host = FakeGameEngineHost()
        val info = ContactInfo("Alice", email = "alice@example.com")
        val e = engine(host, initial = GameState(session = SessionState.Summary))
        e.dispatch(SessionEvent.CollectContact(0, info))
        val next = assertIs<SessionState.CollectingContact>(e.state.value.session)
        assertEquals(0, next.participantIndex)
        advanceUntilIdle()
        assertEquals(1, host.executed.filterIsInstance<Effect.PersistContact>().size)
    }

    @Test
    fun `Summary plus SkipContact goes to Concluded`() = runTest {
        val e = engine(initial = GameState(session = SessionState.Summary))
        e.dispatch(SessionEvent.SkipContact)
        assertEquals(SessionState.Concluded, e.state.value.session)
    }

    @Test
    fun `Summary plus Conclude goes to Concluded with analytics`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.Summary))
        e.dispatch(SessionEvent.Conclude)
        assertEquals(SessionState.Concluded, e.state.value.session)
        advanceUntilIdle()
        assertEquals(
            1,
            host.executed.filterIsInstance<Effect.LogAnalytics>().count { it.event == "session_completed" },
        )
    }

    @Test
    fun `CollectingContact SkipContact advances to next participant`() = runTest {
        val e =
            engine(
                initial =
                GameState(session = SessionState.CollectingContact(0), participantNames = listOf("Alice", "Bob")),
            )
        e.dispatch(SessionEvent.SkipContact)
        val next = assertIs<SessionState.CollectingContact>(e.state.value.session)
        assertEquals(1, next.participantIndex)
    }

    @Test
    fun `CollectingContact SkipContact past last participant goes to Concluded`() = runTest {
        val e =
            engine(
                initial =
                GameState(session = SessionState.CollectingContact(1), participantNames = listOf("Alice", "Bob")),
            )
        e.dispatch(SessionEvent.SkipContact)
        assertEquals(SessionState.Concluded, e.state.value.session)
    }

    @Test
    fun `CollectingContact CollectContact saves contact and advances to next participant`() = runTest {
        val host = FakeGameEngineHost()
        val e =
            engine(
                host,
                initial =
                GameState(session = SessionState.CollectingContact(0), participantNames = listOf("Alice", "Bob")),
            )
        e.dispatch(SessionEvent.CollectContact(0, ContactInfo("Alice", email = "alice@example.com")))
        val next = assertIs<SessionState.CollectingContact>(e.state.value.session)
        assertEquals(1, next.participantIndex)
        advanceUntilIdle()
        assertEquals(1, host.executed.filterIsInstance<Effect.PersistContact>().size)
    }

    @Test
    fun `CollectingContact CollectContact for last participant goes to Concluded`() = runTest {
        val host = FakeGameEngineHost()
        val e =
            engine(
                host,
                initial =
                GameState(session = SessionState.CollectingContact(1), participantNames = listOf("Alice", "Bob")),
            )
        e.dispatch(SessionEvent.CollectContact(1, ContactInfo("Bob")))
        assertEquals(SessionState.Concluded, e.state.value.session)
        advanceUntilIdle()
        assertEquals(1, host.executed.filterIsInstance<Effect.PersistContact>().size)
    }

    // --- Concluded is terminal ---

    @Test
    fun `Concluded plus any event errors`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.Concluded))
        e.dispatch(SessionEvent.BeginSelection)
        assertEquals(SessionState.Concluded, e.state.value.session)
        advanceUntilIdle()
        val analytics = host.executed.filterIsInstance<Effect.LogAnalytics>().single()
        assertEquals("transition_error", analytics.event)
    }

    // --- Loop behavior: draft picks, effect ordering, start/bookmark/discard ---

    @Test
    fun `ToggleCard outside Selecting is invalid`() = runTest {
        val host = FakeGameEngineHost()
        val session = inQuestion(phase = QuestionState.ShowingPrompt)
        val e = engine(host, initial = GameState(session = session, draftPicks = listOf(1)))
        e.dispatch(SessionEvent.ToggleCard(2))
        assertEquals(session, e.state.value.session)
        assertEquals(listOf(1), e.state.value.draftPicks)
        advanceUntilIdle()
        val analytics = host.executed.filterIsInstance<Effect.LogAnalytics>().single()
        assertEquals("transition_error", analytics.event)
    }

    @Test
    fun `ToggleCard adds then removes a draft pick`() = runTest {
        val e =
            engine(initial = GameState(session = inQuestion(phase = QuestionState.Selecting)))
        e.dispatch(SessionEvent.ToggleCard(7))
        assertEquals(listOf(7), e.state.value.draftPicks)
        e.dispatch(SessionEvent.ToggleCard(7))
        assertEquals(emptyList(), e.state.value.draftPicks)
    }

    @Test
    fun `draft picks are kept through Finalizing and Discussing`() = runTest {
        val e =
            engine(
                initial =
                GameState(
                    session = inQuestion(phase = QuestionState.Selecting),
                    participantNames = listOf("Alice"),
                    draftPicks = listOf(1, 2, 3),
                ),
            )
        e.dispatch(SessionEvent.ConfirmSelection)
        assertEquals(listOf(1, 2, 3), e.state.value.draftPicks)
        e.dispatch(SessionEvent.ConfirmFinal)
        assertEquals(listOf(1, 2, 3), e.state.value.draftPicks)
    }

    @Test
    fun `draft picks reset when the next turn begins`() = runTest {
        val e =
            engine(
                initial =
                GameState(
                    session = inQuestion(phase = QuestionState.Discussing),
                    participantNames = listOf("Alice"),
                    draftPicks = listOf(1, 2, 3),
                ),
            )
        e.dispatch(SessionEvent.EndDiscussion)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.ShowingPrompt, next.activity)
        assertEquals(emptyList(), e.state.value.draftPicks)
    }

    @Test
    fun `ChangeSelection retains draft picks`() = runTest {
        val e =
            engine(
                initial =
                GameState(
                    session = inQuestion(phase = QuestionState.Finalizing),
                    participantNames = listOf("Alice"),
                    draftPicks = listOf(1, 2, 3),
                ),
            )
        e.dispatch(SessionEvent.BeginSelection)
        val next = assertIs<SessionState.InQuestion>(e.state.value.session)
        assertEquals(QuestionState.Selecting, next.activity)
        assertEquals(listOf(1, 2, 3), e.state.value.draftPicks)
    }

    @Test
    fun `DismissInstructions marks instructions shown for the rest of the session`() = runTest {
        val e =
            engine(
                initial =
                GameState(
                    session = inQuestion(phase = QuestionState.ShowingInstructions),
                    participantNames = listOf("Alice"),
                ),
            )
        e.dispatch(SessionEvent.DismissInstructions)
        assertTrue(e.state.value.instructionsShown)
    }

    @Test
    fun `PersistParticipants effect echoes into in-memory names in the same emission`() = runTest {
        val e = engine(initial = GameState(session = SessionState.AddingParticipants))
        e.dispatch(SessionEvent.AddParticipant("Alice"))
        assertEquals(listOf("Alice"), e.state.value.participantNames) // before advanceUntilIdle
    }

    @Test
    fun `effects execute in FIFO order`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.dispatch(SessionEvent.AddParticipant("Alice"))
        e.dispatch(SessionEvent.AddParticipant("Ben"))
        advanceUntilIdle()
        val persists = host.executed.filterIsInstance<Effect.PersistParticipants>()
        assertEquals(listOf(listOf("Alice"), listOf("Alice", "Ben")), persists.map { it.names })
    }

    @Test
    fun `close drains queued effects before stopping`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.dispatch(SessionEvent.AddParticipant("Alice"))
        e.close() // no advanceUntilIdle before closing
        advanceUntilIdle()
        assertTrue(host.executed.any { it is Effect.PersistParticipants })
    }

    @Test
    fun `host failure during execute reports nonfatal and keeps processing`() = runTest {
        val host = FakeGameEngineHost().apply { executeError = IllegalStateException("db down") }
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        val event = SessionEvent.AddParticipant("Alice")
        e.dispatch(event)
        advanceUntilIdle()
        assertEquals(listOf("applyEffects after $event"), host.nonFatals)
        host.executeError = null
        e.dispatch(SessionEvent.AddParticipant("Ben"))
        advanceUntilIdle()
        assertTrue(host.executed.isNotEmpty())
    }

    @Test
    fun `start with no persisted session creates it and auto-starts`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host)
        e.start()
        advanceUntilIdle()
        assertEquals(1, host.created.size)
        assertEquals(SessionState.AddingParticipants, e.state.value.session)
    }

    @Test
    fun `start rehydrates persisted state and participant names`() = runTest {
        val host =
            FakeGameEngineHost().apply {
                persistedState = inQuestion(q = 3)
                participantNames = listOf("Riley")
                sessionExists = true
            }
        val e = engine(host)
        e.start()
        advanceUntilIdle()
        assertEquals(inQuestion(q = 3), e.state.value.session)
        assertEquals(listOf("Riley"), e.state.value.participantNames)
        assertEquals(0, host.created.size)
    }

    @Test
    fun `start snaps a mid-question state back to the prompt`() = runTest {
        val host =
            FakeGameEngineHost().apply {
                persistedState = inQuestion(q = 2, phase = QuestionState.Selecting)
                sessionExists = true
            }
        val e = engine(host)
        e.start()
        advanceUntilIdle()
        assertEquals(inQuestion(q = 2), e.state.value.session) // ShowingPrompt
    }

    @Test
    fun `start with an unreadable snapshot recreates the session`() = runTest {
        val host =
            FakeGameEngineHost().apply {
                findSessionStateError = IllegalStateException("corrupt")
                sessionExists = true
            }
        val e = engine(host)
        e.start()
        advanceUntilIdle()
        assertEquals(1, host.created.size) // force-recreated
        assertTrue(host.nonFatals.contains("findSessionState on start"))
        assertEquals(SessionState.AddingParticipants, e.state.value.session)
    }

    @Test
    fun `awaitIdle returns only after queued effects have executed`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.dispatch(SessionEvent.AddParticipant("Alice"))
        e.awaitIdle()
        assertTrue(host.executed.any { it is Effect.PersistParticipants })
    }

    @Test
    fun `bookmark runs after queued effects and sets the flag`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.dispatch(SessionEvent.AddParticipant("Alice"))
        e.bookmark()
        assertEquals(listOf(true), host.bookmarked)
        assertTrue(host.executed.any { it is Effect.PersistParticipants }) // FIFO: effect first
    }

    @Test
    fun `discard deletes the session`() = runTest {
        val host = FakeGameEngineHost()
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.discard()
        assertEquals(1, host.deleted)
    }

    @Test
    fun `bookmark completes even when the host write fails`() = runTest {
        val host = FakeGameEngineHost().apply { setBookmarkedError = IllegalStateException("db down") }
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.bookmark() // completing without hanging is the assertion
        assertEquals(listOf("bookmarkAndExit"), host.nonFatals)
        // parity with the old presenter: conversation_bookmarked is logged unconditionally
        assertTrue(
            host.executed.any {
                it is Effect.LogAnalytics && it.event == "conversation_bookmarked"
            },
        )
    }

    @Test
    fun `discard completes even when the delete fails`() = runTest {
        val host = FakeGameEngineHost().apply { deleteSessionError = IllegalStateException("db down") }
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.discard() // completing without hanging is the assertion
        assertEquals(listOf("discardAndExit"), host.nonFatals)
    }

    @Test
    fun `loadSummaries runs after queued effects`() = runTest {
        val host = FakeGameEngineHost().apply {
            summaries = listOf(GameEngine.ParticipantSummary(0, "Alice", emptyList()))
        }
        val e = engine(host, initial = GameState(session = SessionState.AddingParticipants))
        e.dispatch(SessionEvent.AddParticipant("Alice"))
        val loaded = e.loadSummaries()
        assertEquals(host.summaries, loaded)
        assertTrue(host.executed.any { it is Effect.PersistParticipants }) // FIFO: effect first
    }

    @Test
    fun `loadSummaries returns empty and reports when the host read fails`() = runTest {
        val host = FakeGameEngineHost().apply { loadSummariesError = IllegalStateException("db down") }
        val e = engine(host, initial = GameState(session = SessionState.Summary))
        assertEquals(emptyList(), e.loadSummaries())
        assertEquals(listOf("loadSummaries"), host.nonFatals)
    }
}
