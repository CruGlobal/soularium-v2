package org.cru.soularium.ui.external

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodeURLQueryComponent

internal class ExternalScreenInterceptor(private val uriHandler: UriHandler) : NavigationInterceptor {
    override fun goTo(screen: Screen, navigationContext: NavigationContext) = when (screen) {
        is ExternalScreen.Url -> {
            uriHandler.openUri(screen.url)
            NavigationInterceptor.SuccessConsumed
        }

        is ExternalScreen.Email -> {
            uriHandler.openUri(buildEmailUri(screen.email, screen.subject))
            NavigationInterceptor.SuccessConsumed
        }

        else -> NavigationInterceptor.Skipped
    }

    private fun buildEmailUri(email: String, subject: String?): String = buildString {
        append("mailto:${email.encodeURLPathPart()}")
        if (!subject.isNullOrEmpty()) append("?subject=${subject.encodeURLQueryComponent()}")
    }
}

@Composable
internal fun rememberExternalScreenInterceptor(): NavigationInterceptor {
    val uriHandler = LocalUriHandler.current
    return remember(uriHandler) { ExternalScreenInterceptor(uriHandler) }
}
