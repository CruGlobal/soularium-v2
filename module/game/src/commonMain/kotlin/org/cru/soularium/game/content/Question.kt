package org.cru.soularium.game.content

enum class Question(val number: Int, val requiredImageCount: Int) {
    CURRENT_LIFE(1, 3),
    DESIRED_LIFE(2, 3),
    GOD(3, 1),
    SPIRITUAL_EXPERIENCE(4, 1),
    DESIRED_SPIRITUAL_LIFE(5, 1),

    ;

    companion object {
        fun forNumber(n: Int): Question = entries.first { it.number == n }
    }
}
