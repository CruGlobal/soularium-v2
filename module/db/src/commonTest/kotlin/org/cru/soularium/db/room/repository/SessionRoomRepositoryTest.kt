package org.cru.soularium.db.room.repository

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.db.repository.SessionRepositoryTest
import org.cru.soularium.db.room.SoulariumDatabase
import org.cru.soularium.db.room.buildInMemorySoulariumDatabase

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
}
