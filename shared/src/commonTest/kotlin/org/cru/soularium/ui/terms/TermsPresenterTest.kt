package org.cru.soularium.ui.terms

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.FakeDeviceStateRepository
import org.cru.soularium.ui.nav.HomeScreen

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
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(HomeScreen, navigator.awaitResetRoot().newRoot)
        assertTrue(deviceStateRepo.snapshot().agreedToTos)
    }

    @Test
    fun `Back pops the navigator`() = runTest {
        presenter.test {
            awaitItem().eventSink(TermsPresenter.UiEvent.Back)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(TermsScreen, navigator.awaitPop().poppedScreen)
    }

    @Test
    fun `offers Agree while the user has not yet accepted the terms`() = runTest {
        presenter.test {
            assertTrue(awaitStable { it.showAgree }.showAgree)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hides Agree once the user has already accepted the terms`() = runTest {
        deviceStateRepo.update(DeviceState(hasSeenIntro = true, agreedToTos = true))
        presenter.test {
            // The agreed state never offers Agree — neither the null-device-state
            // default nor the resolved agreed state should show it.
            assertFalse(awaitItem().showAgree)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private suspend fun app.cash.turbine.ReceiveTurbine<TermsPresenter.UiState>.awaitStable(
    predicate: (TermsPresenter.UiState) -> Boolean,
): TermsPresenter.UiState {
    var item = awaitItem()
    while (!predicate(item)) item = awaitItem()
    return item
}
