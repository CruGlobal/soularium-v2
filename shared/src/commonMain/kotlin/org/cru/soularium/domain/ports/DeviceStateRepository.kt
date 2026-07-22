package org.cru.soularium.domain.ports

import kotlinx.coroutines.flow.Flow
import org.cru.soularium.domain.DeviceState

/**
 * Persistent per-device first-launch flags, backed by DataStore rather than the
 * session database.
 */
interface DeviceStateRepository {
    /** Emits the current [DeviceState] and every subsequent change. */
    val deviceState: Flow<DeviceState>

    /** Records that the Intro onboarding has been shown. */
    suspend fun markIntroSeen()

    /** Records that the Terms of Use have been accepted (implies intro seen). */
    suspend fun markTosAgreed()
}
