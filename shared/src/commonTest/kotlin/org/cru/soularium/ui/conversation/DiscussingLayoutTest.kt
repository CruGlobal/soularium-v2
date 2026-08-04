package org.cru.soularium.ui.conversation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.card_a11y_description_1
import org.cru.soularium.generated.resources.image_x_of_y
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
@RunOnAndroidWith(AndroidJUnit4::class)
class DiscussingLayoutTest {
    private fun discussing(cardIds: List<Int>) = ConversationPresenter.UiState.Discussing(
        questionNumber = 1,
        participantName = "Alice",
        cardIds = cardIds,
        showExitDialog = false,
        eventSink = {},
    )

    @Test
    fun `UI - Pager - images use the card's accessibility description`() = runComposeUiTest {
        setContent { DiscussingLayout(discussing(cardIds = listOf(1, 2, 3))) }

        onNode(hasContentDescription(getString(Res.string.card_a11y_description_1))).assertExists()
        onAllNodes(hasContentDescription(getString(Res.string.image_x_of_y, 1, 3))).assertCountEquals(0)
    }
}
