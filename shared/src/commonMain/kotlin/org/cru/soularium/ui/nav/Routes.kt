package org.cru.soularium.ui.nav

object Routes {
    const val INTRO = "intro"
    const val TERMS = "terms"
    const val HOME = "home"
    const val PAST = "past"
    const val ABOUT = "about"
    const val RESOURCES = "resources"
    const val CARDS_AND_QUESTIONS = "cards_and_questions"
    const val SETTINGS = "settings"

    const val ARG_SESSION_ID = "sessionId"
    const val ARG_KIND = "kind"

    const val CONVERSATION = "conversation/{$ARG_SESSION_ID}/{$ARG_KIND}"

    fun conversation(
        sessionId: String,
        kind: String,
    ): String = "conversation/$sessionId/$kind"
}
