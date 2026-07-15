package org.cru.soularium.ui.screens

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.FakeDeviceStateRepository
import org.cru.soularium.ui.home.HomeScreen
import org.cru.soularium.ui.nav.SettingsScreen

@RunOnAndroidWith(AndroidJUnit4::class)
class SettingsPresenterTest {
    // Two-screen stack so pop() has somewhere to land.
    private val navigator = FakeNavigator(HomeScreen, SettingsScreen)
    private val deviceStateRepo = FakeDeviceStateRepository()
    private val presenter = SettingsPresenter(navigator, deviceStateRepo)

    @Test
    fun `initial selected locale reflects device state`() = runTest {
        deviceStateRepo.update(DeviceState(locale = "fr"))
        presenter.test {
            // collectAsState emits its initial DeviceState() default before the
            // repository flow's first value arrives — consume until we see FR.
            awaitStable { it.selectedLocale == AppLocale.FR }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SelectLocale persists the chosen locale and reflects it`() = runTest {
        presenter.test {
            val initial = awaitStable { it.selectedLocale == AppLocale.EN }
            initial.eventSink(SettingsPresenter.UiEvent.SelectLocale(AppLocale.ES))
            awaitStable { it.selectedLocale == AppLocale.ES }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("es", deviceStateRepo.snapshot().locale)
    }

    @Test
    fun `Back pops the navigator`() = runTest {
        presenter.test {
            awaitItem().eventSink(SettingsPresenter.UiEvent.Back)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SettingsScreen, navigator.awaitPop().poppedScreen)
    }
}

private suspend fun <T> app.cash.turbine.ReceiveTurbine<T>.awaitStable(predicate: (T) -> Boolean): T {
    var item = awaitItem()
    while (!predicate(item)) item = awaitItem()
    return item
}
