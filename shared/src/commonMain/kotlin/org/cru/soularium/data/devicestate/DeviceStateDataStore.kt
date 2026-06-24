package org.cru.soularium.data.devicestate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/** File name for the device-state DataStore, shared by the platform binding containers. */
internal const val DEVICE_STATE_FILE = "soularium_device_state.preferences_pb"

/** Builds a preferences [DataStore] at the absolute path returned by [producePath]. */
internal fun preferenceDataStoreAt(producePath: () -> String): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
