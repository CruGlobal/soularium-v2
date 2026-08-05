package org.cru.soularium.db.repository

import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.Conversation
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

/**
 * Contract tests for [SessionRepository]. A concrete subclass supplies a
 * [repository] backed by a real implementation, keeping these behavioural
 * assertions independent of any particular persistence technology.
 */
abstract class SessionRepositoryTest {
    abstract val repository: SessionRepository

    @Test
    fun `findSession - returns the persisted session or null when absent`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.GROUP), SessionState.NotStarted)

        assertEquals(Session.Kind.GROUP, repository.findSession(sessionId)?.kind)
        assertNull(repository.findSession(Session.Id.random()), "an unknown id resolves to null")
    }

    @Test
    fun `findSessionFlow - emits the session and re-emits on change`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.NotStarted)

        repository.findSessionFlow(sessionId).test {
            assertNull(awaitItem()?.endedAt, "a fresh session has no endedAt")
            repository.persistState(sessionId, SessionState.Concluded)
            assertNotNull(awaitItem()?.endedAt, "Concluded stamps endedAt and re-emits")
        }
    }

    @Test
    fun `findSessionFlow - emits null for an unknown id`() = runTest {
        repository.findSessionFlow(Session.Id.random()).test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `findSessionState - returns the latest persisted state or null when absent`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.NotStarted)

        assertEquals(SessionState.NotStarted, repository.findSessionState(sessionId))
        repository.persistState(sessionId, SessionState.AddingParticipants)
        assertEquals(
            SessionState.AddingParticipants,
            repository.findSessionState(sessionId),
            "reflects the latest persisted state",
        )
        assertNull(repository.findSessionState(Session.Id.random()), "an unknown id resolves to null")
    }

    @Test
    fun `createSession - persists the session and its state`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.GROUP), SessionState.AddingParticipants)

        assertEquals(Session.Kind.GROUP, repository.findSession(sessionId)?.kind)
        assertEquals(SessionState.AddingParticipants, repository.findSessionState(sessionId))
    }

    @Test
    fun `persistState - stamps endedAt when the session is Concluded`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.NotStarted)

        assertNull(repository.findSession(sessionId)?.endedAt, "a fresh session has no endedAt")
        repository.persistState(sessionId, SessionState.Concluded)
        assertNotNull(repository.findSession(sessionId)?.endedAt, "Concluded stamps endedAt")
    }

    @Test
    fun `upsertParticipants - prunes conversations when the list shrinks`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.GROUP), SessionState.AddingParticipants)

        repository.upsertParticipants(sessionId, listOf("Ana", "Ben", "Cara"))
        assertEquals(3, repository.loadConversations(sessionId).size)

        repository.upsertParticipants(sessionId, listOf("Ana", "Ben"))
        assertEquals(
            listOf("Ana", "Ben"),
            repository.loadConversations(sessionId).map { it.contact.name },
            "the dropped participant's conversation row is pruned",
        )
    }

    @Test
    fun `setSelectionInstructionsShown - persists the flag`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.NotStarted)
        assertEquals(false, repository.findSession(sessionId)?.selectionInstructionsShown)

        repository.setSelectionInstructionsShown(sessionId)

        assertEquals(true, repository.findSession(sessionId)?.selectionInstructionsShown)
    }

    @Test
    fun `setSelectionInstructionsShown - ignores an unknown session`() = runTest {
        repository.setSelectionInstructionsShown(Session.Id.random())
    }

    @Test
    fun `observeBookmarkedSessions - excludes sessions that have ended`() = runTest {
        val inProgress = Session.Id.random()
        val finished = Session.Id.random()
        repository.createSession(Session(id = inProgress, kind = Session.Kind.SOLO), SessionState.AddingParticipants)
        repository.createSession(Session(id = finished, kind = Session.Kind.SOLO), SessionState.AddingParticipants)
        repository.setBookmarked(inProgress, bookmarked = true)
        repository.setBookmarked(finished, bookmarked = true)
        repository.persistState(finished, SessionState.Concluded)

        repository.observeBookmarkedSessions().test {
            assertEquals(
                listOf(inProgress),
                awaitItem().map { it.id },
                "a bookmarked session that has ended stays out of the bookmarked list",
            )
        }
    }

    @Test
    fun `persistState - ignores a deleted session`() = runTest {
        val sessionId = Session.Id.random()
        repository.persistState(sessionId, SessionState.Concluded)

        assertNull(repository.findSession(sessionId), "a deleted session is not resurrected")
        assertNull(repository.findSessionState(sessionId))
    }

    @Test
    fun `upsertParticipants - ignores a deleted session`() = runTest {
        val sessionId = Session.Id.random()

        assertTrue(repository.upsertParticipants(sessionId, listOf("Ana")).isEmpty())
        assertTrue(repository.loadConversations(sessionId).isEmpty())
    }

    @Test
    fun `upsertContact - ignores a deleted conversation`() = runTest {
        repository.upsertContact(Conversation.Id.random(), ContactInfo("Ana"))
    }

    @Test
    fun `deleteSession - cascades to conversations and card picks`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.AddingParticipants)
        val conversationId = repository.upsertParticipants(sessionId, listOf("Ana")).single()
        repository.upsertPicks(conversationId, questionNumber = 1, cardIds = listOf(4, 8, 15), isFinal = true)
        assertEquals(3, repository.loadPicks(conversationId).size)

        repository.deleteSession(sessionId)

        assertNull(repository.findSession(sessionId))
        assertTrue(repository.loadConversations(sessionId).isEmpty())
        // The picks are removed via the ON DELETE CASCADE chain, which SQLite
        // enforces while foreign-key constraints are active.
        assertTrue(repository.loadPicks(conversationId).isEmpty(), "card picks cascade-delete with the session")
    }
}
