package org.cru.soularium.data.devicestate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.cash.turbine.test
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.cru.soularium.domain.DeviceState

class DeviceStateRepositoryImplTest {
    // The error path logs through Kermit; the Android artifact's default writer
    // touches android.util.Log, which needs a JVM-safe stand-in on the plain host.
    @BeforeTest
    fun silenceLogs() = Logger.setLogWriters(CommonWriter())

    @AfterTest
    fun restoreLogs() = Logger.setLogWriters(platformLogWriter())

    @Test
    fun `deviceState - maps stored preferences`() = runTest {
        val prefs = mutablePreferencesOf()
        prefs[booleanPreferencesKey("has_seen_intro")] = true
        val repository = DeviceStateRepositoryImpl(FakePreferencesDataStore(flowOf(prefs)))

        repository.deviceState.test {
            assertEquals(DeviceState(hasSeenIntro = true, agreedToTos = false), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `deviceState - an unreadable store falls back to defaults instead of crashing`() = runTest {
        val repository =
            DeviceStateRepositoryImpl(
                FakePreferencesDataStore(flow { throw IOException("preferences file unreadable") }),
            )

        repository.deviceState.test {
            assertEquals(
                DeviceState(hasSeenIntro = false, agreedToTos = false),
                awaitItem(),
                "a corrupt device-state store must not crash-loop the app at launch",
            )
            awaitComplete()
        }
    }
}

private class FakePreferencesDataStore(override val data: Flow<Preferences>) : DataStore<Preferences> {
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        emptyPreferences()
}
