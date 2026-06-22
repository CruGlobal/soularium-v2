package org.cru.soularium.domain

sealed interface DomainError {
    data object PersistenceFailed : DomainError

    data class InvalidStateTransition(val from: String, val event: String) : DomainError

    data class InvalidSelectionCount(val expected: Int, val got: Int) : DomainError

    data object ShareUnavailable : DomainError

    data class ContentLoadFailed(val resource: String) : DomainError
}
