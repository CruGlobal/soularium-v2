package org.cru.soularium

import kotlin.test.Test
import kotlin.test.assertEquals
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.ui.nav.HomeScreen
import org.cru.soularium.ui.nav.IntroScreen
import org.cru.soularium.ui.resources.terms.TermsScreen

/**
 * Verifies the launch-time routing rules previously housed in DeviceStateViewModel:
 * the first screen is derived purely from persisted [DeviceState] flags.
 */
class StartScreenTest {

    @Test
    fun `first launch routes to Intro`() {
        assertEquals(IntroScreen, resolveStartScreen(DeviceState()))
    }

    @Test
    fun `intro seen but terms not accepted routes to Terms`() {
        assertEquals(TermsScreen, resolveStartScreen(DeviceState(hasSeenIntro = true)))
    }

    @Test
    fun `intro seen and terms accepted routes to Home`() {
        assertEquals(
            HomeScreen,
            resolveStartScreen(DeviceState(hasSeenIntro = true, agreedToTos = true)),
        )
    }
}
