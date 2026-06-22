package org.cru.soularium.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsParamsTest {
    @Test
    fun `empty map stays empty`() {
        assertEquals(emptyMap(), scrubAnalyticsParams(emptyMap()))
    }

    @Test
    fun `non-sensitive keys are kept`() {
        val params = mapOf<String, Any>("question_number" to 3, "round" to 1, "channel" to "sms")
        assertEquals(params, scrubAnalyticsParams(params))
    }

    @Test
    fun `sensitive keys are dropped`() {
        val params =
            mapOf<String, Any>(
                "name" to "Jordan",
                "email" to "jordan@example.org",
                "phone" to "555-0100",
                "notes" to "met at conference",
                "card_id" to 12,
                "question_number" to 2,
            )
        assertEquals(mapOf<String, Any>("question_number" to 2), scrubAnalyticsParams(params))
    }

    @Test
    fun `matching is case-insensitive`() {
        val params = mapOf<String, Any>("Email" to "x@y.z", "PHONE" to "555")
        assertTrue(scrubAnalyticsParams(params).isEmpty())
    }

    @Test
    fun `matching is substring-based so prefixed keys are also dropped`() {
        val params =
            mapOf<String, Any>(
                "first_name" to "Sam",
                "user_email" to "sam@example.org",
                "selected_card_id" to 7,
                "step" to "summary",
            )
        assertEquals(mapOf<String, Any>("step" to "summary"), scrubAnalyticsParams(params))
    }
}
