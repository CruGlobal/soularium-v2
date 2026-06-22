package org.cru.soularium.domain.content

object Questions {
    val all: List<Question> =
        listOf(
            Question(
                number = 1,
                selectionRounds = 2,
                requiredImageCount = 3,
                promptKey = "q1_prompt",
                selectionKey = "q1_selection",
                finalizingKey = "q1_finalizing",
                discussionKey = "q1_discussion",
            ),
            Question(
                number = 2,
                selectionRounds = 2,
                requiredImageCount = 3,
                promptKey = "q2_prompt",
                selectionKey = "q2_selection",
                finalizingKey = "q2_finalizing",
                discussionKey = "q2_discussion",
            ),
            Question(
                number = 3,
                selectionRounds = 1,
                requiredImageCount = 1,
                promptKey = "q3_prompt",
                selectionKey = "q3_selection",
                finalizingKey = "q3_finalizing",
                discussionKey = "q3_discussion",
            ),
            Question(
                number = 4,
                selectionRounds = 1,
                requiredImageCount = 1,
                promptKey = "q4_prompt",
                selectionKey = "q4_selection",
                finalizingKey = "q4_finalizing",
                discussionKey = "q4_discussion",
            ),
            Question(
                number = 5,
                selectionRounds = 1,
                requiredImageCount = 1,
                promptKey = "q5_prompt",
                selectionKey = "q5_selection",
                finalizingKey = "q5_finalizing",
                discussionKey = "q5_discussion",
            ),
        )

    fun byNumber(n: Int): Question = all.first { it.number == n }
}
