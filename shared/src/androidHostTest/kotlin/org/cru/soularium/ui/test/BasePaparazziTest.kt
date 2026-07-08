package org.cru.soularium.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import java.io.File
import org.cru.soularium.ui.theme.SoulariumTheme
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect
import org.junit.Rule
import org.junit.experimental.categories.Category

@Category(BasePaparazziTest::class)
abstract class BasePaparazziTest(
    protected val deviceConfig: DeviceConfig = DeviceConfig.PIXEL_9_PRO,
    nightMode: NightMode = NightMode.NOTNIGHT,
) {
    protected class DeviceConfigProvider : TestParameterValuesProvider() {
        override fun provideValues(context: Context?) = listOf(
            value(DeviceConfig.NEXUS_5).withName("Nexus 5"),
            value(DeviceConfig.PIXEL_9_PRO).withName("Pixel 9 Pro"),
        )
    }

    // Adds the merged host-test assets dir so Compose resources (fonts, card drawables)
    // resolve at snapshot time — Paparazzi's own resources.json omits them.
    private val testAssetsDir = System.getProperty("paparazzi.project.dir")?.let { File(it) }
        ?.resolve("build/intermediates/assets/androidHostTest/mergeAndroidHostTestAssets")
        ?.takeIf { it.exists() }
        ?.absolutePath

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = deviceConfig.copy(nightMode = nightMode),
        maxPercentDifference = 0.0,
        environment = detectEnvironment().let { env ->
            env.copy(allModuleAssetDirs = env.allModuleAssetDirs + listOfNotNull(testAssetsDir))
        },
    )

    protected fun snapshot(name: String? = null, content: @Composable () -> Unit) {
        paparazzi.snapshot(name) { SnapshotContent(content) }
    }

    @Composable
    private fun SnapshotContent(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            PreviewContextConfigurationEffect()
        }
        SoulariumTheme(content = content)
    }
}
