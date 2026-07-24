package org.cru.soularium.db.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.cru.soularium.db.room.dao.CardPickDao
import org.cru.soularium.db.room.dao.ConversationDao
import org.cru.soularium.db.room.dao.SessionDao
import org.cru.soularium.db.room.entities.CardPickEntity
import org.cru.soularium.db.room.entities.ConversationEntity
import org.cru.soularium.db.room.entities.SessionEntity
import org.cru.soularium.db.room.repository.SessionRoomRepository

@Database(
    entities = [SessionEntity::class, ConversationEntity::class, CardPickEntity::class],
    version = 1,
)
@ConstructedBy(SoulariumDatabaseConstructor::class)
internal abstract class SoulariumDatabase : RoomDatabase() {
    // DAOs
    internal abstract val cardPickDao: CardPickDao
    internal abstract val conversationDao: ConversationDao
    internal abstract val sessionDao: SessionDao

    // Repositories
    internal abstract val sessionRepository: SessionRoomRepository
}

@Suppress("KotlinNoActualForExpect")
internal expect object SoulariumDatabaseConstructor : RoomDatabaseConstructor<SoulariumDatabase> {
    override fun initialize(): SoulariumDatabase
}
