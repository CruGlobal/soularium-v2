package org.cru.soularium.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactInfo(
    @SerialName("name") val name: String,
    @SerialName("surname") val surname: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("notes") val notes: String? = null,
)
