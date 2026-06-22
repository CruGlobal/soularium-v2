package org.cru.soularium.data.devicestate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.DeviceStateRepository

/** [DeviceStateRepository] backed by a preferences [DataStore]. */
internal class DeviceStateRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : DeviceStateRepository {
    override val deviceState: Flow<DeviceState> =
        dataStore.data.map { prefs ->
            DeviceState(
                hasSeenIntro = prefs[HAS_SEEN_INTRO] ?: false,
                agreedToTos = prefs[AGREED_TO_TOS] ?: false,
                locale = prefs[LAST_KNOWN_LOCALE],
            )
        }

    override suspend fun markIntroSeen() {
        dataStore.edit { it[HAS_SEEN_INTRO] = true }
    }

    override suspend fun markTosAgreed() {
        dataStore.edit {
            it[AGREED_TO_TOS] = true
            it[HAS_SEEN_INTRO] = true
        }
    }

    override suspend fun setLocale(locale: String) {
        dataStore.edit { it[LAST_KNOWN_LOCALE] = locale }
    }

    private companion object {
        val HAS_SEEN_INTRO = booleanPreferencesKey("has_seen_intro")
        val AGREED_TO_TOS = booleanPreferencesKey("agreed_to_tos")
        val LAST_KNOWN_LOCALE = stringPreferencesKey("last_known_locale")
    }
}

/** Creates the device-state repository backed by the platform DataStore. */
fun createDeviceStateRepository(): DeviceStateRepository =
    DeviceStateRepositoryImpl(createDeviceStateDataStore())
