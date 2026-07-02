package org.cru.soularium.domain.ports

interface CrashReporter {
    fun recordNonFatal(throwable: Throwable, breadcrumb: String? = null)

    fun setKey(key: String, value: String)
}
