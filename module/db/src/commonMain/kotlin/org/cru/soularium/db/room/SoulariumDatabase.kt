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

@Database(
    entities = [SessionEntity::class, ConversationEntity::class, CardPickEntity::class],
    version = 1,
)
@ConstructedBy(SoulariumDatabaseConstructor::class)
abstract class SoulariumDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao

    abstract fun conversations(): ConversationDao

    abstract fun cardPicks(): CardPickDao
}

@Suppress("KotlinNoActualForExpect")
expect object SoulariumDatabaseConstructor : RoomDatabaseConstructor<SoulariumDatabase> {
    override fun initialize(): SoulariumDatabase
}
