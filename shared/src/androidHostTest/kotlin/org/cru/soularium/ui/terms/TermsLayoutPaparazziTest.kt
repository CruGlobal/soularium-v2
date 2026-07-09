package org.cru.soularium.ui.terms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class TermsLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // The Intro -> Terms gate: user has not yet agreed, so the Agree call-to-action shows.
    @Test
    fun `TermsLayout() - not yet agreed`() = snapshot {
        InspectableTerms(showAgree = true)
    }

    // Reached from Resources after agreeing: only Back is offered.
    @Test
    fun `TermsLayout() - already agreed`() = snapshot {
        InspectableTerms(showAgree = false)
    }

    // TermsLayout loads its Markdown asynchronously, which never resolves in Paparazzi's
    // single-frame render. LocalInspectionMode switches it to a synchronous load; the base
    // fixture only scopes that to resource setup, so provide it around the content here.
    @Composable
    private fun InspectableTerms(showAgree: Boolean) {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            TermsLayout(state = TermsPresenter.UiState(showAgree = showAgree, eventSink = {}))
        }
    }
}
