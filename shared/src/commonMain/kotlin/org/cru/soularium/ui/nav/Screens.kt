package org.cru.soularium.ui.nav

import com.slack.circuit.runtime.screen.Screen
import org.ccci.gto.android.common.parcelize.Parcelize
import org.cru.soularium.model.Session
import org.cru.soularium.model.SessionId

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
    val sessionId: SessionId get() = SessionId(sessionIdValue)

    companion object {
        operator fun invoke(sessionId: SessionId, kind: Session.Kind): ConversationScreen =
            ConversationScreen(sessionId.value, kind)
    }
}
