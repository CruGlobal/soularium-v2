package org.cru.soularium.ui.nav

import com.slack.circuit.runtime.screen.Screen
import org.ccci.gto.android.common.parcelize.Parcelize
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind

@Parcelize
data object IntroScreen : Screen

@Parcelize
data object TermsScreen : Screen

@Parcelize
data object HomeScreen : Screen

@Parcelize
data object PastConversationsScreen : Screen

@Parcelize
data object AboutScreen : Screen

@Parcelize
data object ResourcesScreen : Screen

@Parcelize
data object CardsAndQuestionsScreen : Screen

@Parcelize
data object SettingsScreen : Screen

@Parcelize
data class ConversationScreen(
    /** Backing string id — value-class types aren't directly parcelable. */
    val sessionIdValue: String,
    val kind: SessionKind,
) : Screen {
    val sessionId: SessionId get() = SessionId(sessionIdValue)

    companion object {
        operator fun invoke(sessionId: SessionId, kind: SessionKind): ConversationScreen = ConversationScreen(sessionId.value, kind)
    }
}
