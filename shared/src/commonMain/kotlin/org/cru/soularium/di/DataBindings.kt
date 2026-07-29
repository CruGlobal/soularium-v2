package org.cru.soularium.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.data.devicestate.DeviceStateRepositoryImpl
import org.cru.soularium.data.game.GameSessionStoreImpl
import org.cru.soularium.db.repository.SessionRepository
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.game.GameEngine
import org.cru.soularium.game.GameEngineFactory

@BindingContainer
@ContributesTo(AppScope::class)
interface DataBindings {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun providesDeviceStateRepository(dataStore: DataStore<Preferences>): DeviceStateRepository =
            DeviceStateRepositoryImpl(dataStore)

        // Effects carry no session id of their own, so a fresh GameSessionStoreImpl is built per
        // session/kind pair rather than sharing one instance across engines.
        @Provides
        fun providesGameEngineFactory(
            sessionRepository: SessionRepository,
            analytics: AnalyticsTracker,
            crashReporter: CrashReporter,
        ): GameEngineFactory = GameEngineFactory { sessionId, kind ->
            GameEngine(
                sessionId,
                kind,
                GameSessionStoreImpl(sessionId, sessionRepository, analytics, crashReporter),
            )
        }
    }
}
