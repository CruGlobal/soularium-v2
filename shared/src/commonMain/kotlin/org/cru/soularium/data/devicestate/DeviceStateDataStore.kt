package org.cru.soularium.data.devicestate

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import okio.Path.Companion.toPath

/** File name for the device-state DataStore, shared by the platform binding containers. */
internal const val DEVICE_STATE_FILE = "soularium_device_state.preferences_pb"

/**
 * Builds a preferences [DataStore] at the absolute path returned by [producePath]. A
 * corrupt file is replaced with empty preferences so one bad write can't wedge launch.
 */
internal fun preferenceDataStoreAt(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { producePath().toPath() },
    )
