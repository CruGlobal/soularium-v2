package org.cru.soularium.di

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.platformLogWriter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(AppScope::class)
interface IosLoggingBindings {
    companion object {
        @Provides
        @IntoSet
        internal fun providesPlatformLogWriter(): LogWriter = platformLogWriter()
    }
}
