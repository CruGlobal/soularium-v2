package org.cru.soularium.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.DeviceStateRepository

/**
 * Owns persistent device state: resolves the first-launch start destination
 * and applies the onboarding / locale mutations.
 */
class DeviceStateViewModel(
    private val repository: DeviceStateRepository,
) : ViewModel() {
    /** Current device state; [DeviceState] defaults apply until the store is read. */
    val deviceState: StateFlow<DeviceState> =
        repository.deviceState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DeviceState(),
        )

    /**
     * The navigation start destination, or `null` while the device store is
     * still being read — callers should show a splash until it resolves.
     */
    val startRoute: StateFlow<String?> =
        repository.deviceState
            .map { state ->
                when {
                    !state.hasSeenIntro -> Routes.INTRO
                    !state.agreedToTos -> Routes.TERMS
                    else -> Routes.HOME
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    fun markIntroSeen() = mutate { repository.markIntroSeen() }

    fun markTosAgreed() = mutate { repository.markTosAgreed() }

    fun setLocale(code: String) = mutate { repository.setLocale(code) }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
