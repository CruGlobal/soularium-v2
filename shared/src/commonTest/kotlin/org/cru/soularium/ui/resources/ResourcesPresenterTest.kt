package org.cru.soularium.ui.resources

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.ui.external.ExternalScreen
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

    @Test
    fun `UiEvent - OpenMySoularium - navigates to the mySoularium UrlScreen`() = runTest {
        presenter.test {
            awaitItem().eventSink(ResourcesPresenter.UiEvent.OpenMySoularium)
            assertEquals(ExternalScreen.Url(URL_MYSOULARIUM), navigator.awaitNextScreen())
        }
    }

    @Test
    fun `UiEvent - OpenCruSoularium - navigates to the Cru UrlScreen`() = runTest {
        presenter.test {
            awaitItem().eventSink(ResourcesPresenter.UiEvent.OpenCruSoularium)
            assertEquals(ExternalScreen.Url(URL_CRU_SOULARIUM), navigator.awaitNextScreen())
        }
    }

    @Test
    fun `UiEvent - OpenPrivacyPolicy - navigates to the privacy policy UrlScreen`() = runTest {
        presenter.test {
            awaitItem().eventSink(ResourcesPresenter.UiEvent.OpenPrivacyPolicy)
            assertEquals(ExternalScreen.Url(URL_PRIVACY_POLICY), navigator.awaitNextScreen())
        }
    }

    @Test
    fun `UiEvent - SendFeedback - navigates to the feedback EmailScreen`() = runTest {
        presenter.test {
            awaitItem().eventSink(ResourcesPresenter.UiEvent.SendFeedback)
            val screen = navigator.awaitNextScreen()
            assertTrue(screen is ExternalScreen.Email)
            assertEquals(EMAIL_FEEDBACK, screen.email)
        }
    }
}
