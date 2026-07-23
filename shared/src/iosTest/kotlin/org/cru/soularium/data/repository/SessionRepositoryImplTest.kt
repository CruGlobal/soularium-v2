package org.cru.soularium.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.cru.soularium.data.db.SoulariumDatabase
import org.cru.soularium.data.db.inMemorySoulariumDatabase
import org.cru.soularium.domain.session.SessionState
import org.cru.soularium.model.Session

class SessionRepositoryImplTest {
    private fun newRepo(): Pair<SoulariumDatabase, SessionRepositoryImpl> {
        val db = inMemorySoulariumDatabase()
        return db to SessionRepositoryImpl(db.sessions(), db.conversations(), db.cardPicks())
    }

    @Test
    fun `a session round-trips through createSession and loadState`() = runTest {
        val (db, repo) = newRepo()
        val sessionId = Session.Id.random()
        repo.createSession(Session(id = sessionId, kind = Session.Kind.GROUP), SessionState.AddingParticipants)

        assertEquals(Session.Kind.GROUP, repo.loadSession(sessionId)?.kind)
        assertEquals(SessionState.AddingParticipants, repo.loadState(sessionId))
        db.close()
    }

    @Test
    fun `persistState stamps endedAt once the session is Concluded`() = runTest {
        val (db, repo) = newRepo()
        val sessionId = Session.Id.random()
        repo.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.NotStarted)

        assertNull(repo.loadSession(sessionId)?.endedAt, "a fresh session has no endedAt")
        repo.persistState(sessionId, SessionState.Concluded)
        assertNotNull(repo.loadSession(sessionId)?.endedAt, "Concluded stamps endedAt")
        db.close()
    }

    @Test
    fun `upsertParticipants prunes conversations when the list shrinks`() = runTest {
        val (db, repo) = newRepo()
        val sessionId = Session.Id.random()
        repo.createSession(Session(id = sessionId, kind = Session.Kind.GROUP), SessionState.AddingParticipants)

        repo.upsertParticipants(sessionId, listOf("Ana", "Ben", "Cara"))
        assertEquals(3, repo.loadConversations(sessionId).size)

        repo.upsertParticipants(sessionId, listOf("Ana", "Ben"))
        assertEquals(
            listOf("Ana", "Ben"),
            repo.loadConversations(sessionId).map { it.contact.name },
            "the dropped participant's conversation row is pruned",
        )
        db.close()
    }

    @Test
    fun `deleting a session cascades to its conversations and card picks`() = runTest {
        val (db, repo) = newRepo()
        val sessionId = Session.Id.random()
        repo.createSession(Session(id = sessionId, kind = Session.Kind.SOLO), SessionState.AddingParticipants)
        val conversationId = repo.upsertParticipants(sessionId, listOf("Ana")).single()
        repo.upsertPicks(conversationId, questionNumber = 1, cardIds = listOf(4, 8, 15), isFinal = true)
        assertEquals(3, repo.loadPicks(conversationId).size)

        repo.deleteSession(sessionId)

        assertNull(repo.loadSession(sessionId))
        assertTrue(repo.loadConversations(sessionId).isEmpty())
        // The picks are removed via the ON DELETE CASCADE chain, which only
        // runs when SQLite foreign-key enforcement is enabled.
        assertTrue(repo.loadPicks(conversationId).isEmpty(), "card picks cascade-delete with the session")
        db.close()
    }
}
