package org.cru.soularium.ui.resources

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.ui.resources.terms.TermsScreen

@RunOnAndroidWith(AndroidJUnit4::class)
class ResourcesPresenterTest {
    private val navigator = FakeNavigator(ResourcesScreen)
    private val presenter = ResourcesPresenter(navigator)

    @Test
    fun `UiEvent - Back - pops the navigator`() = runTest {
        presenter.test {
            awaitItem().eventSink(ResourcesPresenter.UiEvent.Back)
            navigator.awaitPop()
        }
    }

    @Test
    fun `UiEvent - OpenTerms - navigates to the Terms screen`() = runTest {
        presenter.test {
            awaitItem().eventSink(ResourcesPresenter.UiEvent.OpenTerms)
            assertEquals(TermsScreen, navigator.awaitNextScreen())
        }
    }
}
