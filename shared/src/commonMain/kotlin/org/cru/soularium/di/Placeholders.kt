package org.cru.soularium.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun screenView(screenName: String) = Unit

    override fun event(name: String, params: Map<String, Any>) = Unit
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, breadcrumb: String?) = Unit

    override fun setKey(key: String, value: String) = Unit
}
