package org.cru.soularium.ui.conversation

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.ShareResult
import org.cru.soularium.domain.ports.Sharer
import org.cru.soularium.model.CardPick
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState
import org.cru.soularium.ui.nav.ConversationSummaryScreen

@RunOnAndroidWith(AndroidJUnit4::class)
class ConversationSummaryPresenterTest {

    private val sessionId = Session.Id.random()
    private val screen = ConversationSummaryScreen(sessionId)
    private val navigator = FakeNavigator(screen)

    private fun presenter(repo: SessionRepository) = ConversationSummaryPresenter(
        navigator = navigator,
        screen = screen,
        sessionRepository = repo,
        sharer = SummaryNoOpSharer,
        analytics = SummaryNoOpAnalytics,
        crashReporter = SummaryNoOpCrash,
    )

    @Test
    fun `Loaded UiState composes participants from conversations and their final picks`() = runTest {
        val alice = Conversation(Conversation.Id.random(), sessionId, 0, ContactInfo("Alice"))
        val bob = Conversation(Conversation.Id.random(), sessionId, 1, ContactInfo("Bob"))
        val repo = FakeSummarySessionRepository(
            conversations = listOf(alice, bob),
            picks = mapOf(
                alice.id to listOf(finalPick(alice.id, 1, cardId = 3), finalPick(alice.id, 2, cardId = 7)),
                bob.id to listOf(finalPick(bob.id, 1, cardId = 12)),
            ),
        )
        presenter(repo).test {
            val loaded = awaitUntil { !it.isLoading && it.error == null && it.participants.size == 2 }
            assertEquals(listOf("Alice", "Bob"), loaded.participants.map { it.name })
            assertEquals(listOf(3, 7), loaded.participants[0].cardIds)
            assertEquals(listOf(12), loaded.participants[1].cardIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picks are filtered to final and sorted by questionNumber then pickOrder`() = runTest {
        val alice = Conversation(Conversation.Id.random(), sessionId, 0, ContactInfo("Alice"))
        val repo = FakeSummarySessionRepository(
            conversations = listOf(alice),
            picks = mapOf(
                alice.id to listOf(
                    // Draft picks (isFinal=false) must be filtered out.
                    pick(alice.id, questionNumber = 1, cardId = 99, pickOrder = 0, isFinal = false),
                    // Out-of-order final picks: presenter must sort by (question, pickOrder).
                    finalPick(alice.id, questionNumber = 2, cardId = 22, pickOrder = 0),
                    finalPick(alice.id, questionNumber = 1, cardId = 11, pickOrder = 1),
                    finalPick(alice.id, questionNumber = 1, cardId = 10, pickOrder = 0),
                ),
            ),
        )
        presenter(repo).test {
            val loaded = awaitUntil { !it.isLoading && it.participants.size == 1 }
            assertEquals(listOf(10, 11, 22), loaded.participants.single().cardIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeConversations throwing surfaces as UiState error`() = runTest {
        val repo = FakeSummarySessionRepository(
            conversations = emptyList(),
            picks = emptyMap(),
            observeConversationsThrows = SerializationException("db decode failure"),
        )
        presenter(repo).test {
            val errored = awaitUntil { it.error != null }
            assertNotNull(errored.error)
            assertTrue(errored.participants.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial emission is loading before flows resolve`() = runTest {
        // A never-emitting flow keeps the collector on the initial Loading value —
        // proves the presenter starts in Loading rather than jumping to Loaded/empty.
        val repo = FakeSummarySessionRepository(
            conversations = emptyList(),
            picks = emptyMap(),
            observeConversationsOverride = flow { /* never emits */ },
        )
        presenter(repo).test {
            val first = awaitItem()
            assertTrue(first.isLoading)
            assertNull(first.error)
            assertTrue(first.participants.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<ConversationSummaryPresenter.UiState>.awaitUntil(
        predicate: (ConversationSummaryPresenter.UiState) -> Boolean,
    ): ConversationSummaryPresenter.UiState {
        var item = awaitItem()
        while (!predicate(item)) item = awaitItem()
        return item
    }
}

private fun finalPick(
    conversationId: Conversation.Id,
    questionNumber: Int,
    cardId: Int,
    pickOrder: Int = 0,
): CardPick = pick(conversationId, questionNumber, cardId, pickOrder, isFinal = true)

private fun pick(
    conversationId: Conversation.Id,
    questionNumber: Int,
    cardId: Int,
    pickOrder: Int,
    isFinal: Boolean,
): CardPick = CardPick(
    id = CardPick.Id.random(),
    conversationId = conversationId,
    questionNumber = questionNumber,
    cardId = cardId,
    pickOrder = pickOrder,
    isFinal = isFinal,
)

private class FakeSummarySessionRepository(
    private val conversations: List<Conversation>,
    private val picks: Map<Conversation.Id, List<CardPick>>,
    private val observeConversationsThrows: Throwable? = null,
    private val observeConversationsOverride: Flow<List<Conversation>>? = null,
) : SessionRepository {
    override fun observeConversations(sessionId: Session.Id): Flow<List<Conversation>> = when {
        observeConversationsOverride != null -> observeConversationsOverride
        observeConversationsThrows != null -> flow { throw observeConversationsThrows }
        else -> flowOf(conversations)
    }

    override fun observePicks(conversationId: Conversation.Id): Flow<List<CardPick>> =
        flowOf(picks[conversationId] ?: emptyList())

    override suspend fun loadConversations(sessionId: Session.Id): List<Conversation> = conversations

    override suspend fun loadPicks(conversationId: Conversation.Id): List<CardPick> =
        picks[conversationId] ?: emptyList()

    // Unused by ConversationSummaryPresenter — stub with defaults.
    override suspend fun createSession(session: Session, initialState: SessionState): Session.Id = session.id
    override suspend fun loadSession(id: Session.Id): Session? = null
    override suspend fun loadState(id: Session.Id): SessionState? = null
    override suspend fun persistState(id: Session.Id, state: SessionState) = Unit
    override suspend fun setBookmarked(id: Session.Id, bookmarked: Boolean) = Unit
    override suspend fun setEnded(id: Session.Id) = Unit
    override suspend fun upsertParticipants(sessionId: Session.Id, names: List<String>): List<Conversation.Id> =
        emptyList()
    override suspend fun upsertContact(conversationId: Conversation.Id, info: ContactInfo) = Unit
    override suspend fun upsertPicks(
        conversationId: Conversation.Id,
        questionNumber: Int,
        cardIds: List<Int>,
        isFinal: Boolean,
    ) = Unit
    override fun observeCompletedSessions(): Flow<List<Session>> = flowOf(emptyList())
    override fun observeBookmarkedSessions(): Flow<List<Session>> = flowOf(emptyList())
    override suspend fun deleteSession(id: Session.Id) = Unit
}

private object SummaryNoOpAnalytics : AnalyticsTracker {
    override fun screenView(screenName: String) = Unit
    override fun event(name: String, params: Map<String, Any>) = Unit
}

private object SummaryNoOpCrash : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, breadcrumb: String?) = Unit
    override fun setKey(key: String, value: String) = Unit
}

private object SummaryNoOpSharer : Sharer {
    override suspend fun share(text: String, subject: String?): ShareResult = ShareResult.Succeeded
}
