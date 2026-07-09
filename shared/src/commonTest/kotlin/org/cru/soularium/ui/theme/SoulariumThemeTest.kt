package org.cru.soularium.ui.theme

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class SoulariumThemeTest {
    @Test
    fun `light scheme wires the generated light brand colors`() {
        SoulariumTheme.lightScheme.primary shouldBe primaryLight
        SoulariumTheme.lightScheme.surface shouldBe surfaceLight
        SoulariumTheme.lightScheme.secondary shouldBe secondaryLight
    }

    @Test
    fun `dark scheme wires the generated dark brand colors`() {
        SoulariumTheme.darkScheme.primary shouldBe primaryDark
        SoulariumTheme.darkScheme.surface shouldBe surfaceDark
        SoulariumTheme.darkScheme.secondary shouldBe secondaryDark
    }

    @Test
    fun `dark scheme differs from light scheme`() {
        SoulariumTheme.darkScheme.primary shouldNotBe SoulariumTheme.lightScheme.primary
        SoulariumTheme.darkScheme.surface shouldNotBe SoulariumTheme.lightScheme.surface
    }
}
