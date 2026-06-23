package org.cru.soularium.ui.conversation

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.CardPickId
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.Conversation
import org.cru.soularium.domain.ConversationId
import org.cru.soularium.domain.Session
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.ports.ShareResult
import org.cru.soularium.domain.ports.Sharer
import org.cru.soularium.domain.session.QuestionActivity
import org.cru.soularium.domain.session.SessionEvent
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.ui.nav.ConversationScreen
import org.cru.soularium.ui.screens.PastConversationsPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end smoke tests for the conversation flow.
 *
 * These drive the real [ConversationPresenter] + state machine against a
 * complete in-memory [SessionRepository], exercising a full session the way
 * the Layout would, without rendering Compose. They cover the four scenarios:
 * a solo run start-to-conclude, a three-person group run, a bookmark-and-resume,
 * and deleting a past conversation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class ConversationFlowTest {

    @Test
    fun `solo session completes from start through summary share and conclude`() = runTest {
        val repo = InMemorySessionRepository()
        val sharer = RecordingSharer()
        val sessionId = SessionId.random()
        val screen = ConversationScreen(sessionId, SessionKind.SOLO)
        val navigator = FakeNavigator(screen)

        presenter(navigator, screen, repo, sharer = sharer).test {
            val added = awaitStable { it.sessionState == SessionState.AddingParticipants }
            added.addParticipant("Jordan")
            awaitStable { it.ui.participantNames == listOf("Jordan") }
                .dispatch(SessionEvent.ConfirmParticipants)

            // Five questions, one participant each.
            playAllTurns(turns = 5)
            val summary = awaitStable { it.sessionState == SessionState.Summary && it.summaries.size == 1 }
            assertEquals(9, summary.summaries.single().cardIds.size)

            summary.eventSink(ConversationPresenter.UiEvent.Share(0))
            advanceUntilIdle()

            summary.dispatch(SessionEvent.Conclude)
            awaitStable { it.sessionState == SessionState.Concluded }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, sharer.shared.size)
        assertTrue(sharer.shared.single().isNotEmpty(), "share text should be a non-empty URL")
    }

    @Test
    fun `group session of three completes all five questions`() = runTest {
        val repo = InMemorySessionRepository()
        val sessionId = SessionId.random()
        val screen = ConversationScreen(sessionId, SessionKind.GROUP)
        val navigator = FakeNavigator(screen)

        presenter(navigator, screen, repo).test {
            awaitStable { it.sessionState == SessionState.AddingParticipants }
                .also { it.addParticipant("Amara") }
            awaitStable { it.ui.participantNames == listOf("Amara") }
                .also { it.addParticipant("Ben") }
            awaitStable { it.ui.participantNames == listOf("Amara", "Ben") }
                .also { it.addParticipant("Chen") }
            awaitStable { it.ui.participantNames == listOf("Amara", "Ben", "Chen") }
                .dispatch(SessionEvent.ConfirmParticipants)

            // Question-major: every participant answers a question before the
            // next one begins — 5 questions × 3 participants = 15 turns.
            playAllTurns(turns = 5 * 3)
            val summary = awaitStable { it.sessionState == SessionState.Summary && it.summaries.size == 3 }
            summary.summaries.forEach { participant ->
                assertEquals(9, participant.cardIds.size, "${participant.name} should have 9 final picks")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bookmarked session resumes from persisted state and completes`() = runTest {
        val repo = InMemorySessionRepository()
        val sessionId = SessionId.random()
        val screen = ConversationScreen(sessionId, SessionKind.SOLO)
        val navigator = FakeNavigator(screen)

        // First sitting: play questions 1 and 2, then bookmark at question 3.
        presenter(navigator, screen, repo).test {
            awaitStable { it.sessionState == SessionState.AddingParticipants }
                .also { it.addParticipant("Riley") }
            awaitStable { it.ui.participantNames == listOf("Riley") }
                .dispatch(SessionEvent.ConfirmParticipants)
            playAllTurns(turns = 2)
            val atQ3 = awaitStable {
                (it.sessionState as? SessionState.InQuestion)
                    ?.let { q -> q.questionNumber == 3 && q.activity == QuestionActivity.ShowingPrompt } == true
            }
            atQ3.eventSink(ConversationPresenter.UiEvent.BookmarkAndExit)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repo.bookmarkedSnapshot().size)

        // Second sitting: a fresh presenter rehydrates the persisted state.
        val resumeNavigator = FakeNavigator(screen)
        presenter(resumeNavigator, screen, repo).test {
            val resumed = awaitStable {
                it.sessionState == SessionState.InQuestion(3, 0, QuestionActivity.ShowingPrompt)
            }
            assertEquals(listOf("Riley"), resumed.ui.participantNames)
            playAllTurns(turns = 3, startingFrom = resumed)
            awaitStable { it.sessionState == SessionState.Summary }
                .dispatch(SessionEvent.Conclude)
            awaitStable { it.sessionState == SessionState.Concluded }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bookmarked group session rehydrates participant names on resume`() = runTest {
        val repo = InMemorySessionRepository()
        val sessionId = SessionId.random()
        val screen = ConversationScreen(sessionId, SessionKind.GROUP)
        val navigator = FakeNavigator(screen)

        presenter(navigator, screen, repo).test {
            awaitStable { it.sessionState == SessionState.AddingParticipants }
                .also { it.addParticipant("Dana") }
            awaitStable { it.ui.participantNames == listOf("Dana") }
                .also { it.addParticipant("Eli") }
            awaitStable { it.ui.participantNames == listOf("Dana", "Eli") }
                .dispatch(SessionEvent.ConfirmParticipants)

            // Both participants finish question 1; bookmark at the start of Q2.
            playAllTurns(turns = 2)
            val atQ2 = awaitStable {
                it.sessionState == SessionState.InQuestion(2, 0, QuestionActivity.ShowingPrompt)
            }
            atQ2.eventSink(ConversationPresenter.UiEvent.BookmarkAndExit)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        // A fresh presenter must restore the participant list, otherwise
        // group turn advancement breaks (every turn looks like the last).
        val resumeNavigator = FakeNavigator(screen)
        presenter(resumeNavigator, screen, repo).test {
            val resumed = awaitStable {
                it.sessionState == SessionState.InQuestion(2, 0, QuestionActivity.ShowingPrompt) &&
                    it.ui.participantNames == listOf("Dana", "Eli")
            }
            assertEquals(listOf("Dana", "Eli"), resumed.ui.participantNames)

            // Questions 2-5, both participants each, reaching the summary.
            playAllTurns(turns = 4 * 2, startingFrom = resumed)
            val summary = awaitStable { it.sessionState == SessionState.Summary && it.summaries.size == 2 }
            assertEquals(2, summary.summaries.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting a past conversation removes it from the completed list`() = runTest {
        val repo = InMemorySessionRepository()
        val sessionId = SessionId.random()
        val screen = ConversationScreen(sessionId, SessionKind.SOLO)
        val navigator = FakeNavigator(screen)

        // Run a full solo session so it lands in the completed list.
        presenter(navigator, screen, repo).test {
            awaitStable { it.sessionState == SessionState.AddingParticipants }
                .also { it.addParticipant("Sam") }
            awaitStable { it.ui.participantNames == listOf("Sam") }
                .dispatch(SessionEvent.ConfirmParticipants)
            playAllTurns(turns = 5)
            awaitStable { it.sessionState == SessionState.Summary }
                .dispatch(SessionEvent.Conclude)
            awaitStable { it.sessionState == SessionState.Concluded }
            cancelAndIgnoreRemainingEvents()
        }

        val pastScreen = org.cru.soularium.ui.nav.PastConversationsScreen
        val pastNavigator = FakeNavigator(pastScreen)
        val past = PastConversationsPresenter(pastNavigator, repo, SilentCrash)
        past.test {
            val withRow = awaitStable { it.completed.size == 1 }
            assertEquals(sessionId, withRow.completed.single().sessionId)
            withRow.eventSink(PastConversationsPresenter.UiEvent.Delete(sessionId))
            val empty = awaitStable { it.completed.isEmpty() }
            assertTrue(empty.completed.isEmpty(), "deleted session should leave the completed list")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun presenter(
        navigator: FakeNavigator,
        screen: ConversationScreen,
        repo: SessionRepository,
        sharer: Sharer = RecordingSharer(),
    ): ConversationPresenter = ConversationPresenter(
        navigator = navigator,
        screen = screen,
        sessionRepository = repo,
        analytics = SilentAnalytics,
        crashReporter = SilentCrash,
        sharer = sharer,
    )
}

private suspend fun <T> ReceiveTurbine<T>.awaitStable(predicate: (T) -> Boolean): T {
    var item = awaitItem()
    while (!predicate(item)) item = awaitItem()
    return item
}

private fun ConversationPresenter.UiState.addParticipant(name: String) {
    eventSink(ConversationPresenter.UiEvent.Dispatch(SessionEvent.AddParticipant(name)))
}

private fun ConversationPresenter.UiState.dispatch(event: SessionEvent) {
    eventSink(ConversationPresenter.UiEvent.Dispatch(event))
}

/**
 * Plays [turns] consecutive turns (1 turn = 1 participant answering 1 question)
 * starting from the current state. Drives a turn through the full flow:
 * BeginSelection, optional DismissInstructions, round 1 (and round 2 for the
 * first two questions), ConfirmFinal, and EndDiscussion.
 *
 * Pass [startingFrom] when the caller has already consumed the first
 * ShowingPrompt emission (e.g. to assert on resume state).
 */
private suspend fun ReceiveTurbine<ConversationPresenter.UiState>.playAllTurns(
    turns: Int,
    startingFrom: ConversationPresenter.UiState? = null,
) {
    var firstPrompt: ConversationPresenter.UiState? = startingFrom
    repeat(turns) {
        val prompt = firstPrompt ?: awaitStable {
            (it.sessionState as? SessionState.InQuestion)?.activity == QuestionActivity.ShowingPrompt
        }
        firstPrompt = null
        val question = Questions.byNumber((prompt.sessionState as SessionState.InQuestion).questionNumber)
        prompt.dispatch(SessionEvent.BeginSelection)

        val afterBegin = awaitStable {
            (it.sessionState as? SessionState.InQuestion)?.activity != QuestionActivity.ShowingPrompt
        }
        val landed = if ((afterBegin.sessionState as SessionState.InQuestion).activity ==
            QuestionActivity.ShowingInstructions
        ) {
            afterBegin.dispatch(SessionEvent.DismissInstructions)
            awaitStable {
                (it.sessionState as? SessionState.InQuestion)?.activity == QuestionActivity.SelectingRound1
            }
        } else {
            afterBegin
        }

        val round1Count = if (question.selectionRounds == 2) {
            question.requiredImageCount + 1
        } else {
            question.requiredImageCount
        }
        landed.pick(round1Count)
        val afterRound1 = awaitStable { it.ui.draftPicks.size == round1Count }
        afterRound1.dispatch(SessionEvent.ConfirmSelection)

        // afterConfirm is either SelectingRound2 (two-round Q) or Finalizing (one-round Q).
        val afterConfirm = awaitStable {
            (it.sessionState as? SessionState.InQuestion)?.activity != QuestionActivity.SelectingRound1
        }
        val readyForFinalize =
            if ((afterConfirm.sessionState as SessionState.InQuestion).activity ==
                QuestionActivity.SelectingRound2
            ) {
                afterConfirm.pick(question.requiredImageCount)
                val r2 = awaitStable { it.ui.draftPicks.size == question.requiredImageCount }
                r2.dispatch(SessionEvent.ConfirmSelection)
                awaitStable {
                    (it.sessionState as? SessionState.InQuestion)?.activity == QuestionActivity.Finalizing
                }
            } else {
                // One-round question: afterConfirm is already Finalizing — no extra dispatch
                // happened, so no new emission to await; reuse the captured state.
                afterConfirm
            }
        readyForFinalize.dispatch(SessionEvent.ConfirmFinal)
        awaitStable {
            (it.sessionState as? SessionState.InQuestion)?.activity == QuestionActivity.Discussing
        }.dispatch(SessionEvent.EndDiscussion)
    }
}

private fun ConversationPresenter.UiState.pick(count: Int) {
    repeat(count) { dispatch(SessionEvent.PickCard(it + 1)) }
}

/**
 * In-memory [SessionRepository] that fully persists state, picks, and contacts.
 * Completed/bookmarked status is tracked with id sets rather than by mutating
 * the [Session] timestamp fields.
 */
private class InMemorySessionRepository : SessionRepository {
    private val sessions = mutableMapOf<SessionId, Session>()
    private val states = mutableMapOf<SessionId, SessionState>()
    private val conversations = mutableMapOf<SessionId, MutableList<Conversation>>()
    private val picks = mutableMapOf<ConversationId, MutableList<CardPick>>()
    private val completedIds = mutableSetOf<SessionId>()
    private val bookmarkedIds = mutableSetOf<SessionId>()
    private val completed = MutableStateFlow<List<Session>>(emptyList())
    private val bookmarked = MutableStateFlow<List<Session>>(emptyList())

    fun bookmarkedSnapshot(): List<Session> = bookmarked.value

    override suspend fun createSession(session: Session, initialState: SessionState): SessionId {
        sessions[session.id] = session
        states[session.id] = initialState
        return session.id
    }

    override suspend fun loadSession(id: SessionId): Session? = sessions[id]
    override suspend fun loadState(id: SessionId): SessionState? = states[id]

    override suspend fun persistState(id: SessionId, state: SessionState) {
        states[id] = state
        if (state == SessionState.Concluded) {
            completedIds += id
            bookmarkedIds -= id
            refresh()
        }
    }

    override suspend fun setBookmarked(id: SessionId, bookmarked: Boolean) {
        if (bookmarked) bookmarkedIds += id else bookmarkedIds -= id
        refresh()
    }

    override suspend fun setEnded(id: SessionId) {
        completedIds += id
        refresh()
    }

    override suspend fun upsertParticipants(sessionId: SessionId, names: List<String>): List<ConversationId> {
        val existing = conversations[sessionId].orEmpty()
        val list = names.mapIndexed { idx, name ->
            Conversation(
                id = existing.getOrNull(idx)?.id ?: ConversationId.random(),
                sessionId = sessionId,
                displayOrder = idx,
                contact = ContactInfo(name),
            )
        }
        conversations[sessionId] = list.toMutableList()
        return list.map { it.id }
    }

    override suspend fun upsertContact(conversationId: ConversationId, info: ContactInfo) {
        conversations.values.forEach { list ->
            val idx = list.indexOfFirst { it.id == conversationId }
            if (idx >= 0) list[idx] = list[idx].copy(contact = info)
        }
    }

    override suspend fun upsertPicks(
        conversationId: ConversationId,
        questionNumber: Int,
        cardIds: List<Int>,
        isFinal: Boolean,
    ) {
        val bucket = picks.getOrPut(conversationId) { mutableListOf() }
        bucket.removeAll { it.questionNumber == questionNumber }
        cardIds.forEachIndexed { order, cardId ->
            bucket += CardPick(
                id = CardPickId.random(),
                conversationId = conversationId,
                questionNumber = questionNumber,
                cardId = cardId,
                pickOrder = order,
                isFinal = isFinal,
            )
        }
    }

    override suspend fun loadPicks(conversationId: ConversationId): List<CardPick> = picks[conversationId].orEmpty()

    override fun observeCompletedSessions(): Flow<List<Session>> = completed.asStateFlow()
    override fun observeBookmarkedSessions(): Flow<List<Session>> = bookmarked.asStateFlow()

    override suspend fun deleteSession(id: SessionId) {
        sessions.remove(id)
        states.remove(id)
        completedIds -= id
        bookmarkedIds -= id
        conversations.remove(id)?.forEach { picks.remove(it.id) }
        refresh()
    }

    override suspend fun loadConversations(sessionId: SessionId): List<Conversation> = conversations[sessionId].orEmpty()

    private fun refresh() {
        completed.value = completedIds.mapNotNull { sessions[it] }
        bookmarked.value = bookmarkedIds.filterNot { it in completedIds }.mapNotNull { sessions[it] }
    }
}

private class RecordingSharer : Sharer {
    val shared = mutableListOf<String>()
    override suspend fun share(text: String, subject: String?): ShareResult {
        shared += text
        return ShareResult.Succeeded
    }
}

private object SilentAnalytics : AnalyticsTracker {
    override fun screenView(screenName: String) = Unit
    override fun event(name: String, params: Map<String, Any>) = Unit
}

private object SilentCrash : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, breadcrumb: String?) = Unit
    override fun setKey(key: String, value: String) = Unit
}
