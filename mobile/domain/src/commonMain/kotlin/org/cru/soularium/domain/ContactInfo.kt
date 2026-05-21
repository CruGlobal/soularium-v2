package org.cru.soularium.domain

import kotlinx.serialization.Serializable

@Serializable
data class ContactInfo(
    val name: String,
    val surname: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val notes: String? = null,
)
