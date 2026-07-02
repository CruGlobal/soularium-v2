package org.cru.soularium.ui.conversation

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.db.repository.FakeSessionRepository
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.model.CardPick
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session
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
    )

    @Test
    fun `Loaded UiState composes participants from conversations and their final picks`() = runTest {
        val alice = Conversation(Conversation.Id.random(), sessionId, 0, ContactInfo("Alice"))
        val bob = Conversation(Conversation.Id.random(), sessionId, 1, ContactInfo("Bob"))
        val repo = FakeSessionRepository().apply {
            seedConversations(sessionId, listOf(alice, bob))
            seedPicks(alice.id, listOf(finalPick(alice.id, 1, cardId = 3), finalPick(alice.id, 2, cardId = 7)))
            seedPicks(bob.id, listOf(finalPick(bob.id, 1, cardId = 12)))
        }
        presenter(repo).test {
            val loaded = awaitUntil { !it.isLoading && !it.loadFailed && it.participants.size == 2 }
            assertEquals(listOf("Alice", "Bob"), loaded.participants.map { it.name })
            assertEquals(
                listOf(QuestionSelections(1, listOf(3)), QuestionSelections(2, listOf(7))),
                loaded.participants[0].selections,
            )
            assertEquals(
                listOf(QuestionSelections(1, listOf(12))),
                loaded.participants[1].selections,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picks are grouped by question and sorted by pickOrder within a question`() = runTest {
        val alice = Conversation(Conversation.Id.random(), sessionId, 0, ContactInfo("Alice"))
        val repo = FakeSessionRepository().apply {
            seedConversations(sessionId, listOf(alice))
            seedPicks(
                alice.id,
                listOf(
                    // Draft picks (isFinal=false) must be filtered out.
                    pick(alice.id, questionNumber = 1, cardId = 99, pickOrder = 0, isFinal = false),
                    // Out-of-order final picks: presenter must group by question and sort
                    // by pickOrder within each group; sections themselves sort by questionNumber.
                    finalPick(alice.id, questionNumber = 2, cardId = 22, pickOrder = 0),
                    finalPick(alice.id, questionNumber = 1, cardId = 11, pickOrder = 1),
                    finalPick(alice.id, questionNumber = 1, cardId = 10, pickOrder = 0),
                ),
            )
        }
        presenter(repo).test {
            val loaded = awaitUntil { !it.isLoading && it.participants.size == 1 }
            assertEquals(
                listOf(
                    QuestionSelections(1, listOf(10, 11)),
                    QuestionSelections(2, listOf(22)),
                ),
                loaded.participants.single().selections,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeConversations throwing surfaces as UiState error`() = runTest {
        val repo = FakeSessionRepository().apply {
            observeConversationsError = SerializationException("db decode failure")
        }
        presenter(repo).test {
            val errored = awaitUntil { it.loadFailed }
            assertTrue(errored.loadFailed)
            assertTrue(errored.participants.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial emission is loading before flows resolve`() = runTest {
        // A never-emitting flow keeps the collector on the initial Loading value —
        // proves the presenter starts in Loading rather than jumping to Loaded/empty.
        val repo = FakeSessionRepository().apply {
            observeConversationsOverride = flow { /* never emits */ }
        }
        presenter(repo).test {
            val first = awaitItem()
            assertTrue(first.isLoading)
            assertFalse(first.loadFailed)
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
