package org.cru.soularium.data.repository

import org.cru.soularium.domain.content.CardImage
import org.cru.soularium.domain.content.Question
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.domain.ports.ContentRepository

class ContentRepositoryImpl : ContentRepository {
    override fun questions(): List<Question> = Questions.all

    override fun cards(): List<CardImage> = (1..50).map { CardImage(it) }
}
