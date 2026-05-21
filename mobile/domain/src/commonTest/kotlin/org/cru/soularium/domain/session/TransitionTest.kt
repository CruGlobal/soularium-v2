package org.cru.soularium.domain.session

import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.SessionKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransitionTest {
    private fun ctx(
        names: List<String> = listOf("Alice"),
        draft: List<Int> = emptyList(),
        finals: List<Int> = emptyList(),
        showInstructions: Boolean = false,
    ) = SessionContext(names, draft, finals, showInstructions)

    // --- NotStarted ---

    @Test
    fun `NotStarted plus StartSession to AddingParticipants`() {
        val r = transition(SessionState.NotStarted, SessionEvent.StartSession(SessionKind.SOLO), ctx(emptyList()))
        assertEquals(SessionState.AddingParticipants, r.next)
        assertNull(r.error)
        assertEquals(1, r.effects.filterIsInstance<Effect.LogAnalytics>().size)
    }

    @Test
    fun `NotStarted plus other event is invalid`() {
        val r = transition(SessionState.NotStarted, SessionEvent.ConfirmParticipants, ctx())
        assertEquals(SessionState.NotStarted, r.next)
        assertNotNull(r.error)
    }

    // --- AddingParticipants ---

    @Test
    fun `AddParticipant persists growing list`() {
        val r = transition(SessionState.AddingParticipants, SessionEvent.AddParticipant("Bob"), ctx(listOf("Alice")))
        assertEquals(SessionState.AddingParticipants, r.next)
        val persist = r.effects.filterIsInstance<Effect.PersistParticipants>().single()
        assertEquals(listOf("Alice", "Bob"), persist.names)
    }

    @Test
    fun `RemoveParticipant drops by index`() {
        val r = transition(SessionState.AddingParticipants, SessionEvent.RemoveParticipant(0), ctx(listOf("Alice", "Bob")))
        val persist = r.effects.filterIsInstance<Effect.PersistParticipants>().single()
        assertEquals(listOf("Bob"), persist.names)
    }

    @Test
    fun `ConfirmParticipants with names advances to InQuestion 1 ShowingPrompt`() {
        val r = transition(SessionState.AddingParticipants, SessionEvent.ConfirmParticipants, ctx(listOf("Alice")))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(1, next.questionNumber)
        assertEquals(0, next.activeParticipantIndex)
        assertEquals(QuestionActivity.ShowingPrompt, next.activity)
    }

    @Test
    fun `ConfirmParticipants empty errors`() {
        val r = transition(SessionState.AddingParticipants, SessionEvent.ConfirmParticipants, ctx(emptyList()))
        assertEquals(SessionState.AddingParticipants, r.next)
        assertNotNull(r.error)
    }

    // --- InQuestion: BeginSelection ---

    @Test
    fun `BeginSelection from ShowingPrompt without instructions goes to SelectingRound1`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
        val r = transition(s, SessionEvent.BeginSelection, ctx(showInstructions = false))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.SelectingRound1, next.activity)
    }

    @Test
    fun `BeginSelection from ShowingPrompt with instructions goes to ShowingInstructions`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
        val r = transition(s, SessionEvent.BeginSelection, ctx(showInstructions = true))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.ShowingInstructions, next.activity)
    }

    @Test
    fun `DismissInstructions to SelectingRound1`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.ShowingInstructions)
        val r = transition(s, SessionEvent.DismissInstructions, ctx())
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.SelectingRound1, next.activity)
    }

    // --- InQuestion: ConfirmSelection ---

    @Test
    fun `Q1 round1 ConfirmSelection with at least 4 picks goes to SelectingRound2`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.SelectingRound1)
        val r = transition(s, SessionEvent.ConfirmSelection, ctx(draft = listOf(1, 2, 3, 4)))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.SelectingRound2, next.activity)
        val picks = r.effects.filterIsInstance<Effect.PersistPicks>().single()
        assertEquals(false, picks.isFinal)
    }

    @Test
    fun `Q1 round1 ConfirmSelection with only 3 picks errors (needs at least 4)`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.SelectingRound1)
        val r = transition(s, SessionEvent.ConfirmSelection, ctx(draft = listOf(1, 2, 3)))
        assertEquals(s, r.next)
        assertNotNull(r.error)
    }

    @Test
    fun `Q1 round2 ConfirmSelection with exactly 3 picks goes to Finalizing`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.SelectingRound2)
        val r = transition(s, SessionEvent.ConfirmSelection, ctx(draft = listOf(1, 2, 3)))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.Finalizing, next.activity)
        val picks = r.effects.filterIsInstance<Effect.PersistPicks>().single()
        assertEquals(true, picks.isFinal)
    }

    @Test
    fun `Q1 round2 ConfirmSelection with wrong count errors`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.SelectingRound2)
        val r = transition(s, SessionEvent.ConfirmSelection, ctx(draft = listOf(1, 2)))
        assertEquals(s, r.next)
        assertNotNull(r.error)
    }

    @Test
    fun `Q3 one-round ConfirmSelection with 1 pick goes to Finalizing`() {
        val s = SessionState.InQuestion(3, 0, QuestionActivity.SelectingRound1)
        val r = transition(s, SessionEvent.ConfirmSelection, ctx(draft = listOf(7)))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.Finalizing, next.activity)
        val picks = r.effects.filterIsInstance<Effect.PersistPicks>().single()
        assertEquals(true, picks.isFinal)
    }

    @Test
    fun `Q3 one-round ConfirmSelection with zero picks errors`() {
        val s = SessionState.InQuestion(3, 0, QuestionActivity.SelectingRound1)
        val r = transition(s, SessionEvent.ConfirmSelection, ctx(draft = emptyList()))
        assertEquals(s, r.next)
        assertNotNull(r.error)
    }

    // --- InQuestion: ConfirmFinal ---

    @Test
    fun `ConfirmFinal with valid count advances to Discussing`() {
        val s = SessionState.InQuestion(2, 0, QuestionActivity.Finalizing)
        val r = transition(s, SessionEvent.ConfirmFinal, ctx(draft = listOf(1, 2, 3)))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.Discussing, next.activity)
        assertEquals(1, r.effects.filterIsInstance<Effect.LogAnalytics>().count { it.event == "question_completed" })
    }

    @Test
    fun `ConfirmFinal with wrong count errors`() {
        val s = SessionState.InQuestion(2, 0, QuestionActivity.Finalizing)
        val r = transition(s, SessionEvent.ConfirmFinal, ctx(draft = listOf(1, 2)))
        assertEquals(s, r.next)
        assertNotNull(r.error)
    }

    // --- InQuestion: EndDiscussion ---

    @Test
    fun `EndDiscussion to next participant within Q1`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.Discussing)
        val r = transition(s, SessionEvent.EndDiscussion, ctx(names = listOf("Alice", "Bob")))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(1, next.questionNumber)
        assertEquals(1, next.activeParticipantIndex)
        assertEquals(QuestionActivity.ShowingPrompt, next.activity)
    }

    @Test
    fun `EndDiscussion last participant of Q1 advances to Q2`() {
        val s = SessionState.InQuestion(1, 0, QuestionActivity.Discussing)
        val r = transition(s, SessionEvent.EndDiscussion, ctx(names = listOf("Alice")))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(2, next.questionNumber)
        assertEquals(0, next.activeParticipantIndex)
        assertEquals(QuestionActivity.ShowingPrompt, next.activity)
    }

    @Test
    fun `EndDiscussion last participant of Q5 advances to Summary`() {
        val s = SessionState.InQuestion(5, 0, QuestionActivity.Discussing)
        val r = transition(s, SessionEvent.EndDiscussion, ctx(names = listOf("Alice")))
        assertEquals(SessionState.Summary, r.next)
    }

    // --- Summary, CollectingContact, Concluded ---

    @Test
    fun `Summary plus CollectContact advances to CollectingContact`() {
        val info = ContactInfo("Alice", email = "alice@example.com")
        val r = transition(SessionState.Summary, SessionEvent.CollectContact(0, info), ctx())
        val next = assertIs<SessionState.CollectingContact>(r.next)
        assertEquals(0, next.participantIndex)
        assertEquals(1, r.effects.filterIsInstance<Effect.PersistContact>().size)
    }

    @Test
    fun `Summary plus SkipContact goes to Concluded`() {
        val r = transition(SessionState.Summary, SessionEvent.SkipContact, ctx())
        assertEquals(SessionState.Concluded, r.next)
    }

    @Test
    fun `Summary plus Conclude goes to Concluded with analytics`() {
        val r = transition(SessionState.Summary, SessionEvent.Conclude, ctx())
        assertEquals(SessionState.Concluded, r.next)
        assertEquals(1, r.effects.filterIsInstance<Effect.LogAnalytics>().count { it.event == "session_completed" })
    }

    @Test
    fun `CollectingContact SkipContact advances to next participant`() {
        val r = transition(
            SessionState.CollectingContact(0),
            SessionEvent.SkipContact,
            ctx(names = listOf("Alice", "Bob")),
        )
        val next = assertIs<SessionState.CollectingContact>(r.next)
        assertEquals(1, next.participantIndex)
    }

    @Test
    fun `CollectingContact SkipContact past last participant goes to Concluded`() {
        val r = transition(
            SessionState.CollectingContact(1),
            SessionEvent.SkipContact,
            ctx(names = listOf("Alice", "Bob")),
        )
        assertEquals(SessionState.Concluded, r.next)
    }

    // --- Bookmark from any state ---

    @Test
    fun `Bookmark from InQuestion emits PersistBookmark and stays put`() {
        val s = SessionState.InQuestion(3, 0, QuestionActivity.Discussing)
        val r = transition(s, SessionEvent.Bookmark, ctx())
        assertEquals(s, r.next)
        assertEquals(1, r.effects.filterIsInstance<Effect.PersistBookmark>().count { it.bookmark })
    }

    @Test
    fun `Bookmark from Summary emits PersistBookmark and stays put`() {
        val r = transition(SessionState.Summary, SessionEvent.Bookmark, ctx())
        assertEquals(SessionState.Summary, r.next)
        assertEquals(1, r.effects.filterIsInstance<Effect.PersistBookmark>().count { it.bookmark })
    }

    // --- Concluded is terminal ---

    @Test
    fun `Concluded plus any event errors`() {
        val r = transition(SessionState.Concluded, SessionEvent.Bookmark, ctx())
        assertEquals(SessionState.Concluded, r.next)
        assertNotNull(r.error)
    }
}
