package org.cru.soularium.domain.ports

import org.cru.soularium.domain.content.CardImage
import org.cru.soularium.domain.content.Question

interface ContentRepository {
    fun questions(): List<Question>

    fun cards(): List<CardImage>
}
