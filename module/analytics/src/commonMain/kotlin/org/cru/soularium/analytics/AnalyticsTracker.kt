package org.cru.soularium.analytics

interface AnalyticsTracker {
    fun screenView(screenName: String)

    fun event(name: String, params: Map<String, Any> = emptyMap())
}
