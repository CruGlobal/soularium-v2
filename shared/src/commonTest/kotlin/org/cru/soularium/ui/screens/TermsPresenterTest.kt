package org.cru.soularium.ui.screens

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.ui.nav.HomeScreen
import org.cru.soularium.ui.nav.TermsScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunOnAndroidWith(AndroidJUnit4::class)
class TermsPresenterTest {
    // Two-screen stack so pop() has somewhere to land — mirrors the real
    // launch path of Intro → Terms.
    private val navigator = FakeNavigator(org.cru.soularium.ui.nav.IntroScreen, TermsScreen)
    private val deviceStateRepo = FakeDeviceStateRepository()
    private val presenter = TermsPresenter(navigator, deviceStateRepo)

    @Test
    fun `Agree marks ToS agreed and resets root to Home`() = runTest {
        presenter.test {
            awaitItem().eventSink(TermsPresenter.UiEvent.Agree)
            assertEquals(HomeScreen, navigator.awaitResetRoot().newRoot)
        }
        assertTrue(deviceStateRepo.snapshot().agreedToTos)
    }

    @Test
    fun `Back pops the navigator`() = runTest {
        presenter.test {
            awaitItem().eventSink(TermsPresenter.UiEvent.Back)
            assertEquals(TermsScreen, navigator.awaitPop().poppedScreen)
        }
    }
}
