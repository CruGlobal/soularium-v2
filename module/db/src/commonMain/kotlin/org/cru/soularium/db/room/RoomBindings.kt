package org.cru.soularium.db.room

import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.db.room.dao.CardPickDao
import org.cru.soularium.db.room.dao.ConversationDao
import org.cru.soularium.db.room.dao.SessionDao

@BindingContainer
@ContributesTo(AppScope::class)
object RoomBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun providesDatabase(builder: RoomDatabase.Builder<SoulariumDatabase>): SoulariumDatabase = builder
        .withForeignKeysEnabled()
        .build()

    @Provides
    internal fun providesSessionDao(db: SoulariumDatabase): SessionDao = db.sessions()

    @Provides
    internal fun providesConversationDao(db: SoulariumDatabase): ConversationDao = db.conversations()

    @Provides
    internal fun providesCardPickDao(db: SoulariumDatabase): CardPickDao = db.cardPicks()
}
