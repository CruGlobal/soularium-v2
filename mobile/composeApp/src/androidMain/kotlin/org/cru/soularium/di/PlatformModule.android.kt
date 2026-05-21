package org.cru.soularium.di

import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.Sharer
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AnalyticsTracker> { NoOpAnalyticsTracker() }
    single<CrashReporter> { NoOpCrashReporter() }
    single<Sharer> { NoOpSharer() }
}
