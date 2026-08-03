package org.cru.soularium.ui.conversation

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.ccci.gto.support.turbine.awaitItemMatching
import org.cru.soularium.game.FakeGameEngine
import org.cru.soularium.game.GameEngine
import org.cru.soularium.game.GameState
import org.cru.soularium.game.SessionEvent
import org.cru.soularium.model.CardPick
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState
import org.cru.soularium.model.game.SessionState.InQuestion.QuestionState
import org.cru.soularium.ui.nav.ConversationScreen

/**
 * Presenter behavior pinned against a scripted [FakeGameEngine]: UiEvent-routing tests assert on
 * [FakeGameEngine.dispatched], UiState-projection tests script [FakeGameEngine.stateFlow]. The
 * full turn-by-turn gameplay flow (real engine + real store) lives in `GameEngineFlowTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class ConversationPresenterTest {

    private val sessionId = Session.Id.random()
    private val screen = ConversationScreen(sessionId, Session.Kind.SOLO)
    private val navigator = FakeNavigator(screen)

    private fun presenter(fakeEngine: FakeGameEngine = FakeGameEngine()) = ConversationPresenter(
        navigator = navigator,
        screen = screen,
        gameEngineFactory = FakeGameEngine.Factory(fakeEngine),
    )

    // ── Bootstrap ─────────────────────────────────────────────────────────

    @Test
    fun `bootstrap starts the engine exactly once`() = runTest {
        val fakeEngine = FakeGameEngine()
        presenter(fakeEngine = fakeEngine).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, fakeEngine.startCount)
    }

    // ── UiState projections ──────────────────────────────────────────────

    @Test
    fun `UiState - AddingParticipants participantNames - reflects the engine's participant list`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.AddingParticipants, participantNames = listOf("Alice", "Bob")),
        )
        presenter(fakeEngine = fakeEngine).test {
            val state = awaitItem() as ConversationPresenter.UiState.AddingParticipants
            assertEquals(listOf("Alice", "Bob"), state.participantNames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UiState - QuestionPrompt questionNumber - reflects the engine's current question`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(
                session = SessionState.InQuestion(3, 0, QuestionState.ShowingPrompt),
                participantNames = listOf("Alice"),
            ),
        )
        presenter(fakeEngine = fakeEngine).test {
            val prompt = awaitItem() as ConversationPresenter.UiState.QuestionPrompt
            assertEquals(3, prompt.questionNumber)
            assertEquals(5, prompt.totalQuestions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UiState - QuestionPrompt participantName - resolves the active participant and group flag`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(
                session = SessionState.InQuestion(2, 1, QuestionState.ShowingPrompt),
                participantNames = listOf("Alice", "Bob"),
            ),
        )
        presenter(fakeEngine = fakeEngine).test {
            val prompt = awaitItem() as ConversationPresenter.UiState.QuestionPrompt
            assertEquals("Bob", prompt.participantName)
            assertTrue(prompt.isGroup)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UiState - Instructions - renders when the engine reports ShowingInstructions`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.InQuestion(2, 0, QuestionState.ShowingInstructions)),
        )
        presenter(fakeEngine = fakeEngine).test {
            assertTrue(awaitItem() is ConversationPresenter.UiState.Instructions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UiState - Selection selectedCardIds - reflects the engine's draft picks`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.InQuestion(1, 0, QuestionState.Selecting), draftPicks = listOf(7, 12)),
        )
        presenter(fakeEngine = fakeEngine).test {
            val selection = awaitItem() as ConversationPresenter.UiState.Selection
            assertEquals(listOf(7, 12), selection.selectedCardIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UiState - Selection isConfirmEnabled - true only once picks equal the required count`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.InQuestion(1, 0, QuestionState.Selecting), draftPicks = listOf(1, 2)),
        )
        presenter(fakeEngine = fakeEngine).test {
            val partial = awaitItem() as ConversationPresenter.UiState.Selection
            assertFalse(partial.isConfirmEnabled)
            fakeEngine.stateFlow.value = fakeEngine.stateFlow.value.copy(draftPicks = listOf(1, 2, 3))
            val full = awaitItem() as ConversationPresenter.UiState.Selection
            assertTrue(full.isConfirmEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UiState - Summary participants - maps the engine's summaries into per-question selections`() = runTest {
        val conversationId = Conversation.Id.random()
        val fakeEngine = FakeGameEngine(GameState(session = SessionState.Summary)).apply {
            summaries = listOf(
                GameEngine.ParticipantSummary(
                    participantIndex = 0,
                    name = "Alice",
                    picks = listOf(
                        CardPick(
                            CardPick.Id.random(),
                            conversationId,
                            questionNumber = 1,
                            cardId = 3,
                            pickOrder = 0,
                            isFinal = true,
                        ),
                        CardPick(
                            CardPick.Id.random(),
                            conversationId,
                            questionNumber = 2,
                            cardId = 9,
                            pickOrder = 0,
                            isFinal = true,
                        ),
                    ),
                ),
            )
        }
        presenter(fakeEngine).test {
            val summary = awaitItemMatching {
                (it as? ConversationPresenter.UiState.Summary)?.participants?.isNotEmpty() == true
            } as ConversationPresenter.UiState.Summary
            assertEquals(
                listOf(QuestionSelections(1, listOf(3)), QuestionSelections(2, listOf(9))),
                summary.participants.single().selections,
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(fakeEngine.loadSummariesCount >= 1, "expected the presenter to load summaries from the engine")
    }

    @Test
    fun `UiState - Concluded - pops the navigator`() = runTest {
        val fakeEngine = FakeGameEngine(GameState(session = SessionState.Concluded))
        presenter(fakeEngine = fakeEngine).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        navigator.awaitPop()
    }

    // ── UiEvent routing ───────────────────────────────────────────────────

    @Test
    fun `UiEvent - AddingParticipants AddParticipant - dispatches AddParticipant to the engine`() = runTest {
        val fakeEngine = FakeGameEngine(GameState(session = SessionState.AddingParticipants))
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.AddingParticipants.AddParticipant("Alice"))
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.AddParticipant("Alice"), fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - AddingParticipants RemoveParticipant - dispatches RemoveParticipant to the engine`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.AddingParticipants, participantNames = listOf("Alice", "Bob")),
        )
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.AddingParticipants.RemoveParticipant(0))
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.RemoveParticipant(0), fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - AddingParticipants Confirm - dispatches ConfirmParticipants`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.AddingParticipants, participantNames = listOf("Alice")),
        )
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.AddingParticipants.Confirm)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.ConfirmParticipants, fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - QuestionPrompt BeginSelection - dispatches BeginSelection`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.InQuestion(1, 0, QuestionState.ShowingPrompt)),
        )
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.QuestionPrompt.BeginSelection)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.BeginSelection, fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - Instructions Dismiss - dispatches DismissInstructions`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.InQuestion(1, 0, QuestionState.ShowingInstructions)),
        )
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.Instructions.Dismiss)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.DismissInstructions, fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - Selection ToggleCard - dispatches ToggleCard with the tapped card id`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.InQuestion(1, 0, QuestionState.Selecting)),
        )
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.Selection.ToggleCard(7))
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.ToggleCard(7), fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - Selection Confirm - dispatches ConfirmSelection`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.InQuestion(1, 0, QuestionState.Selecting), draftPicks = listOf(1, 2, 3)),
        )
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.Selection.Confirm)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.ConfirmSelection, fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - Finalizing Confirm - dispatches ConfirmFinal`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(
                session = SessionState.InQuestion(1, 0, QuestionState.Finalizing),
                draftPicks = listOf(1, 2, 3),
            ),
        )
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.Finalizing.Confirm)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.ConfirmFinal, fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - Discussing Done - dispatches EndDiscussion`() = runTest {
        val fakeEngine = FakeGameEngine(
            GameState(session = SessionState.InQuestion(1, 0, QuestionState.Discussing)),
        )
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.Discussing.Done)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.EndDiscussion, fakeEngine.dispatched.single())
    }

    @Test
    fun `UiEvent - Summary Done - dispatches Conclude`() = runTest {
        val fakeEngine = FakeGameEngine(GameState(session = SessionState.Summary))
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.Summary.Done)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SessionEvent.Conclude, fakeEngine.dispatched.single())
    }

    // ── Bookmark / discard ────────────────────────────────────────────────

    @Test
    fun `UiEvent - BookmarkAndExit - bookmarks the engine and pops the navigator`() = runTest {
        val fakeEngine = FakeGameEngine(GameState(session = SessionState.AddingParticipants))
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.BookmarkAndExit)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, fakeEngine.bookmarkCount)
        navigator.awaitPop()
    }

    @Test
    fun `UiEvent - DiscardAndExit - discards the engine and pops the navigator`() = runTest {
        val fakeEngine = FakeGameEngine(GameState(session = SessionState.AddingParticipants))
        presenter(fakeEngine = fakeEngine).test {
            awaitItem().eventSink(ConversationPresenter.UiEvent.DiscardAndExit)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, fakeEngine.discardCount)
        navigator.awaitPop()
    }

    // Note: closeCount is intentionally not asserted anywhere in this file — Circuit's test
    // harness may not run the composition's onDispose, so the assertion would be flaky rather
    // than meaningful (per the task brief).
}
