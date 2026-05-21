package org.cru.soularium.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.cru.soularium.data.db.entities.CardPickEntity
import org.cru.soularium.data.db.entities.ConversationEntity
import org.cru.soularium.data.db.entities.SessionEntity

@Database(
    entities = [SessionEntity::class, ConversationEntity::class, CardPickEntity::class],
    version = 1,
    exportSchema = true,
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
