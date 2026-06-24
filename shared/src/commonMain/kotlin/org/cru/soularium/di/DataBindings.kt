package org.cru.soularium.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.data.db.CardPickDao
import org.cru.soularium.data.db.ConversationDao
import org.cru.soularium.data.db.SessionDao
import org.cru.soularium.data.db.SoulariumDatabase
import org.cru.soularium.data.db.withForeignKeysEnabled
import org.cru.soularium.data.devicestate.DeviceStateRepositoryImpl
import org.cru.soularium.domain.ports.DeviceStateRepository

@BindingContainer
@ContributesTo(AppScope::class)
interface DataBindings {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun providesDatabase(builder: RoomDatabase.Builder<SoulariumDatabase>): SoulariumDatabase = builder
            .setDriver(BundledSQLiteDriver())
            .withForeignKeysEnabled()
            .build()

        @Provides
        fun providesSessionDao(db: SoulariumDatabase): SessionDao = db.sessions()

        @Provides
        fun providesConversationDao(db: SoulariumDatabase): ConversationDao = db.conversations()

        @Provides
        fun providesCardPickDao(db: SoulariumDatabase): CardPickDao = db.cardPicks()

        @Provides
        @SingleIn(AppScope::class)
        fun providesDeviceStateRepository(dataStore: DataStore<Preferences>): DeviceStateRepository = DeviceStateRepositoryImpl(dataStore)
    }
}
