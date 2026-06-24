package org.cru.soularium.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.data.db.SoulariumDatabase
import org.cru.soularium.data.devicestate.DEVICE_STATE_FILE
import org.cru.soularium.data.devicestate.preferenceDataStoreAt

@BindingContainer
@ContributesTo(AppScope::class)
interface AndroidDataBindings {
    companion object {
        @Provides
        internal fun providesDatabaseBuilder(context: Context): RoomDatabase.Builder<SoulariumDatabase> = Room.databaseBuilder<SoulariumDatabase>(
            context = context,
            name = context.getDatabasePath("soularium.db").absolutePath,
        )

        @Provides
        @SingleIn(AppScope::class)
        internal fun providesDeviceStateDataStore(context: Context): DataStore<Preferences> = preferenceDataStoreAt {
            context.filesDir.resolve(DEVICE_STATE_FILE).absolutePath
        }
    }
}
