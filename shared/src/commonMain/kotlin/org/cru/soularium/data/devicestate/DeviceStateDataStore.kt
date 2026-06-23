package org.cru.soularium.data.devicestate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/** File name for the device-state DataStore, shared by the platform actuals. */
internal const val DEVICE_STATE_FILE = "soularium_device_state.preferences_pb"

/** Creates the platform-specific device-state [DataStore]. */
internal expect fun createDeviceStateDataStore(): DataStore<Preferences>

/** Builds a preferences [DataStore] at the absolute path returned by [producePath]. */
internal fun preferenceDataStoreAt(producePath: () -> String): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
