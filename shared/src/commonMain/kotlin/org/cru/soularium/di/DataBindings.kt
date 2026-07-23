package org.cru.soularium.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.data.devicestate.DeviceStateRepositoryImpl
import org.cru.soularium.domain.ports.DeviceStateRepository

@BindingContainer
@ContributesTo(AppScope::class)
interface DataBindings {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun providesDeviceStateRepository(dataStore: DataStore<Preferences>): DeviceStateRepository =
            DeviceStateRepositoryImpl(dataStore)
    }
}
