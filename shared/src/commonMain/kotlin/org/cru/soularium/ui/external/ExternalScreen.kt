package org.cru.soularium.ui.external

import com.slack.circuit.runtime.screen.Screen
import org.ccci.gto.android.common.parcelize.Parcelize

/**
 * A [Screen] that is handled outside the Circuit render stack (e.g. by opening
 * an external app). These never enter the back stack — the navigator's
 * `ExternalScreenInterceptor` consumes them.
 */
sealed interface ExternalScreen : Screen {
    /** Opens [url] in the platform browser / handler via the ambient `UriHandler`. */
    @Parcelize
    data class Url(val url: String) : ExternalScreen

    /** Opens a `mailto:` link to [email], with an optional pre-resolved [subject]. */
    @Parcelize
    data class Email(val email: String, val subject: String? = null) : ExternalScreen
}
