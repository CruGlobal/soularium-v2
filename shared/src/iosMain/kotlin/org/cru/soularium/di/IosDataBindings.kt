package org.cru.soularium.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.ExperimentalForeignApi
import org.cru.soularium.data.db.SoulariumDatabase
import org.cru.soularium.data.devicestate.DEVICE_STATE_FILE
import org.cru.soularium.data.devicestate.preferenceDataStoreAt
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@BindingContainer
@ContributesTo(AppScope::class)
interface IosDataBindings {
    companion object {
        @Provides
        internal fun providesDatabaseBuilder(): RoomDatabase.Builder<SoulariumDatabase> = Room.databaseBuilder<SoulariumDatabase>(name = documentDirectory() + "/soularium.db")

        @Provides
        @SingleIn(AppScope::class)
        internal fun providesDeviceStateDataStore(): DataStore<Preferences> = preferenceDataStoreAt {
            documentDirectory() + "/" + DEVICE_STATE_FILE
        }

        @OptIn(ExperimentalForeignApi::class)
        private fun documentDirectory(): String {
            val directory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            return requireNotNull(directory?.path)
        }
    }
}
