package org.cru.soularium.di

import org.cru.soularium.domain.ports.AnalyticsTracker
import org.cru.soularium.domain.ports.CrashReporter

class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun screenView(screenName: String) = Unit

    override fun event(
        name: String,
        params: Map<String, Any>,
    ) = Unit
}

class NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(
        throwable: Throwable,
        breadcrumb: String?,
    ) = Unit

    override fun setKey(
        key: String,
        value: String,
    ) = Unit
}
