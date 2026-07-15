package org.cru.soularium.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.slack.circuit.overlay.OverlayEffect
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class HomeLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    @Test
    fun `HomeLayout() - menu closed`() = snapshot {
        HomeLayout(state = HomePresenter.UiState())
    }

    @Test
    @OptIn(ExperimentalMaterial3Api::class)
    fun `HomeLayout() - menu open`() = snapshot {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeLayout(state = HomePresenter.UiState())
            OverlayEffect {
                show(
                    HomeMenuOverlay(
                        sheetState = SheetState(
                            skipPartiallyExpanded = true,
                            positionalThreshold = { 0f },
                            velocityThreshold = { 0f },
                            initialValue = SheetValue.Expanded,
                            skipHiddenState = true,
                        ),
                    ),
                )
            }
        }
    }
}
