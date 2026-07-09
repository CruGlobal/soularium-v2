package org.cru.soularium.domain.ports

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.cru.soularium.domain.DeviceState

/** In-memory [DeviceStateRepository] fake for tests, seeded with an optional [initial] state. */
internal class FakeDeviceStateRepository(initial: DeviceState = DeviceState()) : DeviceStateRepository {
    private val state = MutableStateFlow(initial)

    override val deviceState: Flow<DeviceState> = state

    fun snapshot(): DeviceState = state.value
    fun update(newState: DeviceState) {
        state.value = newState
    }

    override suspend fun markIntroSeen() {
        state.update { it.copy(hasSeenIntro = true) }
    }

    override suspend fun markTosAgreed() {
        state.update { it.copy(hasSeenIntro = true, agreedToTos = true) }
    }

    override suspend fun setLocale(locale: String) {
        state.update { it.copy(locale = locale) }
    }
}
