package org.cru.soularium.ui.screens

import androidx.compose.runtime.mutableStateOf
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CardsAndQuestionsLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    @Test
    fun `CardsAndQuestionsLayout()`() = snapshot {
        CardsAndQuestionsLayout(state = CardsAndQuestionsPresenter.UiState(eventSink = {}))
    }

    @Test
    fun `CardsAndQuestionsLayout() - questions tab`() {
        val state = CardsAndQuestionsPresenter.UiState(
            selectedTab = mutableStateOf(TAB_QUESTIONS),
            eventSink = {},
        )
        snapshot { CardsAndQuestionsLayout(state = state) }
    }
}
