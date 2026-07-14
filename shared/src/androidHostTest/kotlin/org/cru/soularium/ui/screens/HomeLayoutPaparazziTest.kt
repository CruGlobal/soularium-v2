package org.cru.soularium.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class HomeLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // Default state: hero + primary/secondary CTAs, menu button in the top-right corner.
    @Test
    fun `HomeLayout() - menu closed`() = snapshot {
        HomeLayout(state = HomePresenter.UiState(eventSink = {}))
    }

    // Menu-open state: snapshot the menu rows directly. The production MenuBottomSheet
    // wraps this same content in an animated ModalBottomSheet, which Paparazzi cannot
    // render in a single frame.
    @Test
    fun `HomeLayout() - menu open`() = snapshot {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MenuBottomSheetContent(
                onMySoularium = {},
                onPastConversations = {},
                onAbout = {},
                onResources = {},
                onCardsAndQuestions = {},
                onSettings = {},
                onClose = {},
            )
        }
    }
}
