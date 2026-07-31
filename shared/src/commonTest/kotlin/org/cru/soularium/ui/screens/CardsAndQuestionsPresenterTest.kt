package org.cru.soularium.ui.screens

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.ui.nav.CardsAndQuestionsScreen

@RunOnAndroidWith(AndroidJUnit4::class)
class CardsAndQuestionsPresenterTest {
    private val navigator = FakeNavigator(CardsAndQuestionsScreen)
    private val presenter = CardsAndQuestionsPresenter(navigator)

    @Test
    fun `UiEvent - Back - pops the navigator`() = runTest {
        presenter.test {
            awaitItem().eventSink(CardsAndQuestionsPresenter.UiEvent.Back)
            navigator.awaitPop()
        }
    }

    @Test
    fun `UiState - selectedTab - defaults to the images tab`() = runTest {
        presenter.test {
            assertEquals(TAB_IMAGES, awaitItem().selectedTab.value)
        }
    }
}
