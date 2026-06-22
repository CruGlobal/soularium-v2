package org.cru.soularium.domain

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Returns [Session.startedAt] as a "YYYY-MM-DD" string in the device's local time zone.
 */
fun Session.startedAtLocalDate(): String {
    val local = startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')}"
}
