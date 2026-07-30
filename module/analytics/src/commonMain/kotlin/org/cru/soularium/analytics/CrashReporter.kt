package org.cru.soularium.analytics

/**
 * Temporary home — crash reporting is planned to move to Kermit logging; avoid building on this interface.
 */
interface CrashReporter {
    fun recordNonFatal(throwable: Throwable, breadcrumb: String? = null)

    fun setKey(key: String, value: String)
}
