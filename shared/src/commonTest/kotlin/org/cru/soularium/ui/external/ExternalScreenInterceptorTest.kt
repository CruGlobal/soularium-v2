package org.cru.soularium.ui.external

import androidx.compose.ui.platform.UriHandler
import com.slack.circuitx.navigation.intercepting.InterceptedResult
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.cru.soularium.ui.home.HomeScreen

class ExternalScreenInterceptorTest {
    private val uriHandler = RecordingUriHandler()
    private val interceptor = ExternalScreenInterceptor(uriHandler)

    @Test
    fun `goTo - UrlScreen - opens the url and consumes the navigation`() {
        val result = interceptor.goTo(ExternalScreen.Url("https://example.com"))

        assertEquals(listOf("https://example.com"), uriHandler.opened)
        assertEquals(NavigationInterceptor.SuccessConsumed, result)
    }

    @Test
    fun `goTo - EmailScreen with subject - opens an encoded mailto and consumes the navigation`() {
        val result = interceptor.goTo(ExternalScreen.Email("test@example.com", subject = "Hi there"))

        assertEquals(listOf("mailto:test@example.com?subject=Hi%20there"), uriHandler.opened)
        assertEquals(NavigationInterceptor.SuccessConsumed, result)
    }

    @Test
    fun `goTo - EmailScreen without subject - opens a bare mailto`() {
        interceptor.goTo(ExternalScreen.Email("test@example.com"))

        assertEquals(listOf("mailto:test@example.com"), uriHandler.opened)
    }

    @Test
    fun `goTo - EmailScreen - encodes the address while keeping the at-sign literal`() {
        interceptor.goTo(ExternalScreen.Email("a b@example.com"))

        assertEquals(listOf("mailto:a%20b@example.com"), uriHandler.opened)
    }

    @Test
    fun `goTo - non-external screen - is skipped and opens nothing`() {
        val result = interceptor.goTo(HomeScreen)

        assertTrue(uriHandler.opened.isEmpty())
        assertEquals(InterceptedResult.Skipped, result)
    }
}

private class RecordingUriHandler : UriHandler {
    val opened = mutableListOf<String>()

    override fun openUri(uri: String) {
        opened += uri
    }
}
