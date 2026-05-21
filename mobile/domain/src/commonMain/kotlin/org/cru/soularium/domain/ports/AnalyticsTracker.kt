package org.cru.soularium.domain.ports

interface AnalyticsTracker {
    fun screenView(screenName: String)
    fun event(name: String, params: Map<String, Any> = emptyMap())
}
