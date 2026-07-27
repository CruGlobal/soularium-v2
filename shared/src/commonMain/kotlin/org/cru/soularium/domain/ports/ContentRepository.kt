package org.cru.soularium.domain.ports

import org.cru.soularium.game.content.CardImage
import org.cru.soularium.game.content.Question

interface ContentRepository {
    fun questions(): List<Question>

    fun cards(): List<CardImage>
}
