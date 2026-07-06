package org.cru.soularium.di

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.OptionalBinding

@BindingContainer
@ContributesTo(AppScope::class)
interface LoggingBindings {
    @Multibinds(allowEmpty = true)
    fun logWriters(): Set<LogWriter>

    interface Accessors {
        @OptionalBinding
        val logMinSeverity: Severity get() = Severity.Error
        val logWriters: Set<LogWriter>
    }
}

/**
 * Bootstraps the global Kermit [Logger] from the Metro-assembled set of [LogWriter]s (the
 * [CrashlyticsLogWriter][org.cru.soularium.logging.CrashlyticsLogWriter] plus any platform console
 * writer). Call once at app startup — Android `Application.onCreate`, iOS `MainViewController` —
 * before anything logs. Afterwards, code logs through the global `Logger` (e.g.
 * `Logger.withTag("Foo").e(throwable) { "breadcrumb" }`).
 */
fun LoggingBindings.Accessors.configureLogging() {
    Logger.setMinSeverity(logMinSeverity)
    Logger.setLogWriters(logWriters.toList())
}
