package org.cru.soularium.di

import com.slack.circuit.foundation.Circuit
import org.cru.soularium.data.db.SoulariumDatabase
import org.cru.soularium.data.db.createDatabase
import org.cru.soularium.data.devicestate.createDeviceStateRepository
import org.cru.soularium.data.repository.ContentRepositoryImpl
import org.cru.soularium.data.repository.SessionRepositoryImpl
import org.cru.soularium.domain.ports.ContentRepository
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.ui.nav.SoulariumPresenterFactory
import org.cru.soularium.ui.nav.SoulariumUiFactory
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module =
    module {
        single<SoulariumDatabase> { createDatabase() }
        single { get<SoulariumDatabase>().sessions() }
        single { get<SoulariumDatabase>().conversations() }
        single { get<SoulariumDatabase>().cardPicks() }

        single<SessionRepository> { SessionRepositoryImpl(get(), get(), get()) }
        single<ContentRepository> { ContentRepositoryImpl() }
        single<DeviceStateRepository> { createDeviceStateRepository() }

        single { SoulariumPresenterFactory(get(), get(), get(), get(), get()) }
        single<Circuit> {
            Circuit.Builder()
                .addPresenterFactory(get<SoulariumPresenterFactory>())
                .addUiFactory(SoulariumUiFactory)
                .build()
        }
    }
