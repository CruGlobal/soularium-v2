package org.cru.soularium.analytics

/**
 * Analytics parameter keys whose values may carry personally identifying or
 * otherwise sensitive information. Any key *containing* one of these tokens
 * (case-insensitive) is dropped before an event is logged.
 */
private val SENSITIVE_KEY_TOKENS = listOf("name", "email", "phone", "notes", "card_id")

/**
 * Strips entries whose key looks like it carries PII or card-selection detail,
 * so participant data never leaves the device through an analytics event.
 *
 * Applied by the Firebase [org.cru.soularium.domain.ports.AnalyticsTracker]
 * implementation before forwarding params to `FirebaseAnalytics.logEvent`
 * (wired once Firebase config files land — see Tasks 41–42).
 */
fun scrubAnalyticsParams(params: Map<String, Any>): Map<String, Any> = params.filterKeys { key ->
    val lower = key.lowercase()
    SENSITIVE_KEY_TOKENS.none { token -> lower.contains(token) }
}
