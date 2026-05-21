package org.cru.soularium.data.devicestate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal actual fun createDeviceStateDataStore(): DataStore<Preferences> =
    preferenceDataStoreAt { documentDirectory() + "/" + DEVICE_STATE_FILE }

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val directory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(directory?.path)
}
