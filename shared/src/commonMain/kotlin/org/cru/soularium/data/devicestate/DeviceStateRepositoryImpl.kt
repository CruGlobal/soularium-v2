package org.cru.soularium.data.devicestate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.DeviceStateRepository

private val logger = Logger.withTag("DeviceStateRepository")

/** [DeviceStateRepository] backed by a preferences [DataStore]. */
internal class DeviceStateRepositoryImpl(private val dataStore: DataStore<Preferences>) : DeviceStateRepository {
    override val deviceState: Flow<DeviceState> =
        dataStore.data
            // An unreadable preferences file would otherwise throw out of every
            // collector — including the app root — crash-looping launch until the
            // user clears app data. Fall back to defaults (re-showing intro/ToS).
            .catch { e ->
                if (e !is IOException) throw e
                logger.e(e) { "device-state read failed" }
                emit(emptyPreferences())
            }
            .map { prefs ->
                DeviceState(
                    hasSeenIntro = prefs[HAS_SEEN_INTRO] ?: false,
                    agreedToTos = prefs[AGREED_TO_TOS] ?: false,
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

    private companion object {
        val HAS_SEEN_INTRO = booleanPreferencesKey("has_seen_intro")
        val AGREED_TO_TOS = booleanPreferencesKey("agreed_to_tos")
    }
}
