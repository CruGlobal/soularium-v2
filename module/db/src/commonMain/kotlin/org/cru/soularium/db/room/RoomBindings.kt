package org.cru.soularium.db.room

import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.db.repository.SessionRepository

@BindingContainer
@ContributesTo(AppScope::class)
object RoomBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun providesDatabase(builder: RoomDatabase.Builder<SoulariumDatabase>): SoulariumDatabase = builder
        .build()

    @Provides
    internal fun providesSessionRepository(db: SoulariumDatabase): SessionRepository = db.sessionRepository
}
