package org.cru.soularium.domain.ports

interface Sharer {
    suspend fun share(text: String, subject: String? = null): ShareResult
}

sealed interface ShareResult {
    data object Succeeded : ShareResult

    data object Cancelled : ShareResult

    data object NoAppAvailable : ShareResult
}
