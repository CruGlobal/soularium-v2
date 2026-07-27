package org.cru.soularium.db.room.repository

import androidx.room.execSQL
import androidx.room.useWriterConnection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.db.repository.SessionRepositoryTest
import org.cru.soularium.db.room.SoulariumDatabase
import org.cru.soularium.db.room.buildInMemorySoulariumDatabase
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

@RunOnAndroidWith(AndroidJUnit4::class)
class SessionRoomRepositoryTest : SessionRepositoryTest() {
    private lateinit var db: SoulariumDatabase

    override val repository: SessionRepository get() = db.sessionRepository

    @BeforeTest
    fun createDb() {
        db = buildInMemorySoulariumDatabase()
    }

    @AfterTest
    fun closeDb() {
        db.close()
    }

    @Test
    fun `findSessionState - returns null when the persisted snapshot is unreadable`() = runTest {
        val sessionId = seedSessionWithUnreadableSnapshot()

        assertNull(repository.findSessionState(sessionId))
    }

    @Test
    fun `findSession - returns the session when the persisted snapshot is unreadable`() = runTest {
        val sessionId = seedSessionWithUnreadableSnapshot()

        assertEquals(Session.Kind.GROUP, repository.findSession(sessionId)?.kind)
    }

    private suspend fun seedSessionWithUnreadableSnapshot(): Session.Id {
        val sessionId = Session.Id.random()
        repository.createSession(Session(id = sessionId, kind = Session.Kind.GROUP), SessionState.AddingParticipants)
        db.useWriterConnection { it.execSQL("UPDATE sessions SET state_snapshot_json = 'not json'") }
        return sessionId
    }
}
