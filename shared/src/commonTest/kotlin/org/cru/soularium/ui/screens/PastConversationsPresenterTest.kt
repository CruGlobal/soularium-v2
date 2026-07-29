package org.cru.soularium.ui.screens

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.analytics.CrashReporter
import org.cru.soularium.db.repository.FakeSessionRepository
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState
import org.cru.soularium.ui.nav.PastConversationsScreen

/**
 * Re-homed from the old engine-flow smoke test, which drove a full presenter session just to
 * produce a completed row — seeding the repository directly is enough to exercise deletion of a
 * past conversation.
 */
@RunOnAndroidWith(AndroidJUnit4::class)
class PastConversationsPresenterTest {

    @Test
    fun `deleting a past conversation removes it from the completed list`() = runTest {
        val sessionId = Session.Id.random()
        val repo = FakeSessionRepository().apply {
            seedSession(Session(id = sessionId, kind = Session.Kind.SOLO))
            seedConversations(
                sessionId,
                listOf(Conversation(Conversation.Id.random(), sessionId, 0, ContactInfo("Sam"))),
            )
        }
        repo.persistState(sessionId, SessionState.Concluded)

        val navigator = FakeNavigator(PastConversationsScreen)
        val presenter = PastConversationsPresenter(navigator, repo, NoOpCrash)

        presenter.test {
            val withRow = awaitStable { it.completed.size == 1 }
            assertEquals(sessionId, withRow.completed.single().sessionId)
            withRow.eventSink(PastConversationsPresenter.UiEvent.Delete(sessionId))
            val empty = awaitStable { it.completed.isEmpty() }
            assertTrue(empty.completed.isEmpty(), "deleted session should leave the completed list")
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private suspend fun ReceiveTurbine<PastConversationsPresenter.UiState>.awaitStable(
    predicate: (PastConversationsPresenter.UiState) -> Boolean,
): PastConversationsPresenter.UiState {
    var item = awaitItem()
    while (!predicate(item)) item = awaitItem()
    return item
}

private object NoOpCrash : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, breadcrumb: String?) = Unit
    override fun setKey(key: String, value: String) = Unit
}
