package org.cru.soularium.ui.settings

import androidx.compose.ui.text.intl.Locale
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SettingsLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    @Test
    fun `SettingsLayout()`() = snapshot {
        SettingsLayout(
            state = SettingsPresenter.UiState(
                selectedLanguage = Locale("en"),
                supportedLanguages = listOf(Locale("en"), Locale("es"), Locale("fr"), Locale("pl"), Locale("zh-Hans")),
                eventSink = {},
            ),
        )
    }
}
