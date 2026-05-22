package org.cru.soularium.di

import org.cru.soularium.data.db.SoulariumDatabase
import org.cru.soularium.data.db.createDatabase
import org.cru.soularium.data.devicestate.createDeviceStateRepository
import org.cru.soularium.data.repository.ContentRepositoryImpl
import org.cru.soularium.data.repository.SessionRepositoryImpl
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.ports.ContentRepository
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.ui.conversation.ConversationViewModel
import org.cru.soularium.ui.nav.DeviceStateViewModel
import org.cru.soularium.ui.past.PastConversationsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
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

        viewModel { (sessionId: SessionId) ->
            ConversationViewModel(sessionId, get(), get(), get(), get())
        }
        viewModel { PastConversationsViewModel(get(), get()) }
        viewModel { DeviceStateViewModel(get()) }
    }
