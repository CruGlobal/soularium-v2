package org.cru.soularium.ui.conversation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import org.cru.soularium.ui.past.PastConversationsViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end smoke tests for the conversation flow (plan Task 49).
 *
 * These drive the real [ConversationViewModel] + state machine against a
 * complete in-memory [SessionRepository], exercising a full session the way
 * [ConversationHost] would, without rendering Compose. They cover the four
 * plan scenarios: a solo run start-to-conclude, a three-person group run, a
 * bookmark-and-resume, and deleting a past conversation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationFlowTest {
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
    fun `solo session completes from start through summary share and conclude`() =
        runTest(testDispatcher) {
            val repo = InMemorySessionRepository()
            val sharer = RecordingSharer()
            val sessionId = SessionId.random()
            val vm = viewModel(sessionId, repo, sharer)

            vm.ensureStarted(SessionKind.SOLO)
            advanceUntilIdle()
            assertEquals(SessionState.AddingParticipants, vm.state.value)

            addParticipants(vm, "Jordan")
            vm.dispatch(SessionEvent.ConfirmParticipants)
            advanceUntilIdle()

            // Five questions, one participant each.
            repeat(5) { playTurn(vm) }
            assertEquals(SessionState.Summary, vm.state.value)

            // Summary exposes the participant's nine final picks (3 + 3 + 1 + 1 + 1).
            assertEquals(1, vm.summaries.value.size)
            assertEquals(9, vm.summaries.value.single().cardIds.size)

            vm.shareSummary(participantIndex = 0)
            advanceUntilIdle()
            assertTrue(sharer.shared.single().isNotEmpty(), "share text should be a non-empty URL")

            vm.dispatch(SessionEvent.Conclude)
            advanceUntilIdle()
            assertEquals(SessionState.Concluded, vm.state.value)
        }

    @Test
    fun `group session of three completes all five questions`() =
        runTest(testDispatcher) {
            val repo = InMemorySessionRepository()
            val sessionId = SessionId.random()
            val vm = viewModel(sessionId, repo)

            vm.ensureStarted(SessionKind.GROUP)
            advanceUntilIdle()
            addParticipants(vm, "Amara", "Ben", "Chen")
            vm.dispatch(SessionEvent.ConfirmParticipants)
            advanceUntilIdle()

            // Question-major order: every participant answers a question before
            // the next one begins — 5 questions x 3 participants = 15 turns.
            repeat(5 * 3) { playTurn(vm) }

            assertEquals(SessionState.Summary, vm.state.value)
            assertEquals(3, vm.summaries.value.size)
            vm.summaries.value.forEach { participant ->
                assertEquals(9, participant.cardIds.size, "${participant.name} should have 9 final picks")
            }
        }

    @Test
    fun `bookmarked session resumes from persisted state and completes`() =
        runTest(testDispatcher) {
            val repo = InMemorySessionRepository()
            val sessionId = SessionId.random()

            // First sitting: play questions 1 and 2, then bookmark at question 3.
            val first = viewModel(sessionId, repo)
            first.ensureStarted(SessionKind.SOLO)
            advanceUntilIdle()
            addParticipants(first, "Riley")
            first.dispatch(SessionEvent.ConfirmParticipants)
            advanceUntilIdle()
            playTurn(first)
            playTurn(first)
            assertEquals(
                QuestionActivity.ShowingPrompt,
                (first.state.value as SessionState.InQuestion).activity,
            )
            assertEquals(3, (first.state.value as SessionState.InQuestion).questionNumber)

            var exited = false
            first.bookmarkAndExit { exited = true }
            advanceUntilIdle()
            assertTrue(exited, "bookmarkAndExit should invoke its completion callback")
            assertEquals(1, repo.bookmarkedSnapshot().size)

            // Second sitting: a fresh ViewModel rehydrates the persisted state.
            val resumed = viewModel(sessionId, repo)
            resumed.ensureStarted(SessionKind.SOLO)
            advanceUntilIdle()
            assertEquals(
                SessionState.InQuestion(3, 0, QuestionActivity.ShowingPrompt),
                resumed.state.value,
            )

            // Finish questions 3, 4, 5.
            repeat(3) { playTurn(resumed) }
            assertEquals(SessionState.Summary, resumed.state.value)

            resumed.dispatch(SessionEvent.Conclude)
            advanceUntilIdle()
            assertEquals(SessionState.Concluded, resumed.state.value)
        }

    @Test
    fun `bookmarked group session rehydrates participant names on resume`() =
        runTest(testDispatcher) {
            val repo = InMemorySessionRepository()
            val sessionId = SessionId.random()

            val first = viewModel(sessionId, repo)
            first.ensureStarted(SessionKind.GROUP)
            advanceUntilIdle()
            addParticipants(first, "Dana", "Eli")
            first.dispatch(SessionEvent.ConfirmParticipants)
            advanceUntilIdle()

            // Both participants finish question 1; bookmark at the start of Q2.
            playTurn(first)
            playTurn(first)
            assertEquals(
                SessionState.InQuestion(2, 0, QuestionActivity.ShowingPrompt),
                first.state.value,
            )
            first.bookmarkAndExit { }
            advanceUntilIdle()

            // A fresh ViewModel must restore the participant list, otherwise
            // group turn advancement breaks (every turn looks like the last).
            val resumed = viewModel(sessionId, repo)
            resumed.ensureStarted(SessionKind.GROUP)
            advanceUntilIdle()
            assertEquals(listOf("Dana", "Eli"), resumed.ui.value.participantNames)

            // Questions 2-5, both participants each, reaching the summary.
            repeat(4 * 2) { playTurn(resumed) }
            assertEquals(SessionState.Summary, resumed.state.value)
            assertEquals(2, resumed.summaries.value.size)
        }

    @Test
    fun `deleting a past conversation removes it from the completed list`() =
        runTest(testDispatcher) {
            val repo = InMemorySessionRepository()
            val sessionId = SessionId.random()

            // Run a full solo session so it lands in the completed list.
            val vm = viewModel(sessionId, repo)
            vm.ensureStarted(SessionKind.SOLO)
            advanceUntilIdle()
            addParticipants(vm, "Sam")
            vm.dispatch(SessionEvent.ConfirmParticipants)
            advanceUntilIdle()
            repeat(5) { playTurn(vm) }
            vm.dispatch(SessionEvent.Conclude)
            advanceUntilIdle()

            val past = PastConversationsViewModel(repo, SilentCrash)
            advanceUntilIdle()
            assertEquals(1, past.completed.value.size)
            assertEquals(sessionId, past.completed.value.single().sessionId)

            past.delete(sessionId)
            advanceUntilIdle()
            assertTrue(past.completed.value.isEmpty(), "deleted session should leave the completed list")
        }

    // ── Driving helpers ────────────────────────────────────────────────────

    private fun viewModel(
        sessionId: SessionId,
        repo: SessionRepository,
        sharer: Sharer = RecordingSharer(),
    ): ConversationViewModel =
        ConversationViewModel(
            sessionId = sessionId,
            sessionRepository = repo,
            analytics = SilentAnalytics,
            crashReporter = SilentCrash,
            sharer = sharer,
        )

    private fun TestScope.addParticipants(
        vm: ConversationViewModel,
        vararg names: String,
    ) {
        names.forEach { vm.dispatch(SessionEvent.AddParticipant(it)) }
        advanceUntilIdle()
    }

    /**
     * Plays one participant's turn for the current question: begin selection,
     * dismiss the one-time instruction panel if shown, pick the required cards
     * across one or two rounds, finalize, and end the discussion.
     */
    private fun TestScope.playTurn(vm: ConversationViewModel) {
        val question = Questions.byNumber((vm.state.value as SessionState.InQuestion).questionNumber)

        vm.dispatch(SessionEvent.BeginSelection)
        advanceUntilIdle()
        if ((vm.state.value as SessionState.InQuestion).activity == QuestionActivity.ShowingInstructions) {
            vm.dispatch(SessionEvent.DismissInstructions)
            advanceUntilIdle()
        }

        // Round 1: a two-round question needs one more than the final count.
        val round1Count =
            if (question.selectionRounds == 2) question.requiredImageCount + 1 else question.requiredImageCount
        pick(vm, round1Count)
        vm.dispatch(SessionEvent.ConfirmSelection)
        advanceUntilIdle()

        // Round 2 (two-round questions only): narrow to exactly the final count.
        if (question.selectionRounds == 2) {
            pick(vm, question.requiredImageCount)
            vm.dispatch(SessionEvent.ConfirmSelection)
            advanceUntilIdle()
        }

        vm.dispatch(SessionEvent.ConfirmFinal)
        advanceUntilIdle()
        vm.dispatch(SessionEvent.EndDiscussion)
        advanceUntilIdle()
    }

    private fun pick(
        vm: ConversationViewModel,
        count: Int,
    ) {
        repeat(count) { vm.dispatch(SessionEvent.PickCard(it + 1)) }
    }
}

/**
 * In-memory [SessionRepository] that fully persists state, picks, and
 * contacts. Completed/bookmarked status is tracked with id sets rather than
 * the [Session] timestamp fields, since `:shared` does not depend on
 * kotlinx-datetime and so cannot touch `Session.endedAt`/`bookmarkedAt`.
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

    override suspend fun createSession(
        session: Session,
        initialState: SessionState,
    ): SessionId {
        sessions[session.id] = session
        states[session.id] = initialState
        return session.id
    }

    override suspend fun loadSession(id: SessionId): Session? = sessions[id]

    override suspend fun loadState(id: SessionId): SessionState? = states[id]

    override suspend fun persistState(
        id: SessionId,
        state: SessionState,
    ) {
        states[id] = state
        if (state == SessionState.Concluded) {
            completedIds += id
            bookmarkedIds -= id
            refresh()
        }
    }

    override suspend fun setBookmarked(
        id: SessionId,
        bookmarked: Boolean,
    ) {
        if (bookmarked) bookmarkedIds += id else bookmarkedIds -= id
        refresh()
    }

    override suspend fun setEnded(id: SessionId) {
        completedIds += id
        refresh()
    }

    override suspend fun upsertParticipants(
        sessionId: SessionId,
        names: List<String>,
    ): List<ConversationId> {
        val existing = conversations[sessionId].orEmpty()
        val list =
            names.mapIndexed { idx, name ->
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

    override suspend fun upsertContact(
        conversationId: ConversationId,
        info: ContactInfo,
    ) {
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
            bucket +=
                CardPick(
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

    override suspend fun share(
        text: String,
        subject: String?,
    ): ShareResult {
        shared += text
        return ShareResult.Succeeded
    }
}

private object SilentAnalytics : AnalyticsTracker {
    override fun screenView(screenName: String) = Unit

    override fun event(
        name: String,
        params: Map<String, Any>,
    ) = Unit
}

private object SilentCrash : CrashReporter {
    override fun recordNonFatal(
        throwable: Throwable,
        breadcrumb: String?,
    ) = Unit

    override fun setKey(
        key: String,
        value: String,
    ) = Unit
}
