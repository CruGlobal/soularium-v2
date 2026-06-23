package org.cru.soularium.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.data.db.CardPickDao
import org.cru.soularium.data.db.ConversationDao
import org.cru.soularium.data.db.SessionDao
import org.cru.soularium.data.db.SoulariumDatabase
import org.cru.soularium.data.db.createDatabase
import org.cru.soularium.data.devicestate.createDeviceStateRepository
import org.cru.soularium.domain.ports.DeviceStateRepository

@BindingContainer
@ContributesTo(AppScope::class)
interface DataBindings {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun providesDatabase(): SoulariumDatabase = createDatabase()

        @Provides
        fun providesSessionDao(db: SoulariumDatabase): SessionDao = db.sessions()

        @Provides
        fun providesConversationDao(db: SoulariumDatabase): ConversationDao = db.conversations()

        @Provides
        fun providesCardPickDao(db: SoulariumDatabase): CardPickDao = db.cardPicks()

        @Provides
        @SingleIn(AppScope::class)
        fun providesDeviceStateRepository(): DeviceStateRepository = createDeviceStateRepository()
    }
}
