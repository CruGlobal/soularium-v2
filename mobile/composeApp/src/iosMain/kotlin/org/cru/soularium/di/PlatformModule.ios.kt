package org.cru.soularium.di

import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter
import org.cru.soularium.domain.ports.Sharer
import org.cru.soularium.platform.IosSharer
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        // Analytics + crash reporting stay no-op until Firebase config files
        // (GoogleService-Info.plist) land — see Tasks 41–42.
        single<AnalyticsTracker> { NoOpAnalyticsTracker() }
        single<CrashReporter> { NoOpCrashReporter() }
        single<Sharer> { IosSharer() }
    }
