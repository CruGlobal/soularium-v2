package org.cru.soularium.di

import android.content.Context
import android.content.pm.ApplicationInfo
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.platformLogWriter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(AppScope::class)
interface AndroidLoggingBindings {
    companion object {
        @Provides
        @ElementsIntoSet
        internal fun providesPlatformLogWriter(context: Context): Set<LogWriter> = buildSet {
            // Only log to logcat on debuggable builds; release builds report solely via Crashlytics.
            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                add(platformLogWriter())
            }
        }
    }
}
