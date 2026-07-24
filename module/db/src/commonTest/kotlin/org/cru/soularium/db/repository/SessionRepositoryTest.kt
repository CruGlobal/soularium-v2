package org.cru.soularium.db.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
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
    fun `createSession - persists the session and its state`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.GROUP), SessionState.AddingParticipants)

        assertEquals(Session.Kind.GROUP, repository.loadSession(sessionId)?.kind)
        assertEquals(SessionState.AddingParticipants, repository.loadState(sessionId))
    }

    @Test
    fun `persistState - stamps endedAt when the session is Concluded`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.NotStarted)

        assertNull(repository.loadSession(sessionId)?.endedAt, "a fresh session has no endedAt")
        repository.persistState(sessionId, SessionState.Concluded)
        assertNotNull(repository.loadSession(sessionId)?.endedAt, "Concluded stamps endedAt")
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
    fun `deleteSession - cascades to conversations and card picks`() = runTest {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.AddingParticipants)
        val conversationId = repository.upsertParticipants(sessionId, listOf("Ana")).single()
        repository.upsertPicks(conversationId, questionNumber = 1, cardIds = listOf(4, 8, 15), isFinal = true)
        assertEquals(3, repository.loadPicks(conversationId).size)

        repository.deleteSession(sessionId)

        assertNull(repository.loadSession(sessionId))
        assertTrue(repository.loadConversations(sessionId).isEmpty())
        // The picks are removed via the ON DELETE CASCADE chain, which SQLite
        // enforces while foreign-key constraints are active.
        assertTrue(repository.loadPicks(conversationId).isEmpty(), "card picks cascade-delete with the session")
    }
}
