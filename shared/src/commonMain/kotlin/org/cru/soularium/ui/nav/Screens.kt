package org.cru.soularium.ui.nav

import com.slack.circuit.runtime.screen.Screen
import org.ccci.gto.android.common.parcelize.Parcelize
import org.cru.soularium.model.Session

@Parcelize
data object IntroScreen : Screen

@Parcelize
data object PastConversationsScreen : Screen

@Parcelize
data object CardsAndQuestionsScreen : Screen

@Parcelize
data class ConversationScreen(
    /** Backing string id — value-class types aren't directly parcelable. */
    val sessionIdValue: String,
    val kind: Session.Kind,
) : Screen {
    val sessionId: Session.Id get() = Session.Id(sessionIdValue)

    companion object {
        operator fun invoke(sessionId: Session.Id, kind: Session.Kind): ConversationScreen =
            ConversationScreen(sessionId.value, kind)
    }
}

@Parcelize
data class ConversationSummaryScreen(
    /** Backing string id — value-class types aren't directly parcelable. */
    val sessionIdValue: String,
) : Screen {
    val sessionId: Session.Id get() = Session.Id(sessionIdValue)

    companion object {
        operator fun invoke(sessionId: Session.Id): ConversationSummaryScreen =
            ConversationSummaryScreen(sessionId.value)
    }
}
