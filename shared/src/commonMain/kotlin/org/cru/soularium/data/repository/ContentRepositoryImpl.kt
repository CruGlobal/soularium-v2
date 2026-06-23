package org.cru.soularium.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.cru.soularium.domain.content.CardImage
import org.cru.soularium.domain.content.Question
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.domain.ports.ContentRepository

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ContentRepositoryImpl : ContentRepository {
    override fun questions(): List<Question> = Questions.all

    override fun cards(): List<CardImage> = (1..50).map { CardImage(it) }
}
