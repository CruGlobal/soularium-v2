package org.cru.soularium.ui.conversation

import org.cru.soularium.game.content.Question
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.q1_discussion
import org.cru.soularium.generated.resources.q1_finalizing
import org.cru.soularium.generated.resources.q1_prompt
import org.cru.soularium.generated.resources.q1_selection
import org.cru.soularium.generated.resources.q2_discussion
import org.cru.soularium.generated.resources.q2_finalizing
import org.cru.soularium.generated.resources.q2_prompt
import org.cru.soularium.generated.resources.q2_selection
import org.cru.soularium.generated.resources.q3_discussion
import org.cru.soularium.generated.resources.q3_finalizing
import org.cru.soularium.generated.resources.q3_prompt
import org.cru.soularium.generated.resources.q3_selection
import org.cru.soularium.generated.resources.q4_discussion
import org.cru.soularium.generated.resources.q4_finalizing
import org.cru.soularium.generated.resources.q4_prompt
import org.cru.soularium.generated.resources.q4_selection
import org.cru.soularium.generated.resources.q5_discussion
import org.cru.soularium.generated.resources.q5_finalizing
import org.cru.soularium.generated.resources.q5_prompt
import org.cru.soularium.generated.resources.q5_selection
import org.jetbrains.compose.resources.StringResource

internal val Question.promptRes: StringResource
    get() = when (this) {
        Question.CURRENT_LIFE -> Res.string.q1_prompt
        Question.DESIRED_LIFE -> Res.string.q2_prompt
        Question.GOD -> Res.string.q3_prompt
        Question.SPIRITUAL_EXPERIENCE -> Res.string.q4_prompt
        Question.DESIRED_SPIRITUAL_LIFE -> Res.string.q5_prompt
    }

internal val Question.selectionRes: StringResource
    get() = when (this) {
        Question.CURRENT_LIFE -> Res.string.q1_selection
        Question.DESIRED_LIFE -> Res.string.q2_selection
        Question.GOD -> Res.string.q3_selection
        Question.SPIRITUAL_EXPERIENCE -> Res.string.q4_selection
        Question.DESIRED_SPIRITUAL_LIFE -> Res.string.q5_selection
    }

internal val Question.finalizingRes: StringResource
    get() = when (this) {
        Question.CURRENT_LIFE -> Res.string.q1_finalizing
        Question.DESIRED_LIFE -> Res.string.q2_finalizing
        Question.GOD -> Res.string.q3_finalizing
        Question.SPIRITUAL_EXPERIENCE -> Res.string.q4_finalizing
        Question.DESIRED_SPIRITUAL_LIFE -> Res.string.q5_finalizing
    }

internal val Question.discussionRes: StringResource
    get() = when (this) {
        Question.CURRENT_LIFE -> Res.string.q1_discussion
        Question.DESIRED_LIFE -> Res.string.q2_discussion
        Question.GOD -> Res.string.q3_discussion
        Question.SPIRITUAL_EXPERIENCE -> Res.string.q4_discussion
        Question.DESIRED_SPIRITUAL_LIFE -> Res.string.q5_discussion
    }
