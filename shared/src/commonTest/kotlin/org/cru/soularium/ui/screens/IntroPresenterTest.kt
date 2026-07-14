package org.cru.soularium.ui.screens

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.domain.ports.FakeDeviceStateRepository
import org.cru.soularium.ui.nav.IntroScreen
import org.cru.soularium.ui.resources.terms.TermsScreen

@RunOnAndroidWith(AndroidJUnit4::class)
class IntroPresenterTest {
    private val navigator = FakeNavigator(IntroScreen)
    private val deviceStateRepo = FakeDeviceStateRepository()
    private val presenter = IntroPresenter(navigator, deviceStateRepo)

    @Test
    fun `Continue marks intro seen and navigates to Terms`() = runTest {
        presenter.test {
            awaitItem().eventSink(IntroPresenter.UiEvent.Continue)
            assertEquals(TermsScreen, navigator.awaitNextScreen())
        }
        assertTrue(deviceStateRepo.snapshot().hasSeenIntro)
    }
}
