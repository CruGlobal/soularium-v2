package org.cru.soularium.data.devicestate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.cru.soularium.data.db.dataAndroidContext

internal actual fun createDeviceStateDataStore(): DataStore<Preferences> =
    preferenceDataStoreAt {
        dataAndroidContext().filesDir.resolve(DEVICE_STATE_FILE).absolutePath
    }
