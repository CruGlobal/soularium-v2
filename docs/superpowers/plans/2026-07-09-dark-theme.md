# Material 3 Dark Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Material 3 dark color scheme to Soularium and restructure the theme composable after mpdx-kmp's `MpdxTheme`, so the app follows the system light/dark setting.

**Architecture:** Regenerate `Color.kt` with a complete M3 slot set (light + dark) produced from Soularium's brand seeds via `material-color-utilities` (color match: false). Restructure `Theme.kt` into an `object SoulariumTheme` that holds both schemes plus an `isDarkThemeActive` accessor, paired with a `fun SoulariumTheme(darkTheme = isSystemInDarkTheme(), content)` that selects the scheme. Update the docs that declared "light only," and re-record Paparazzi snapshots (existing `NightMode` params already emit dark variants).

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1 (Material3), Circuit, Paparazzi, kotlin.test + Kotest.

## Global Constraints

- Max line length: **120 characters**; ktlint `intellij_idea` code style; trailing commas encouraged.
- Theme tokens live only in `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/`. Raw `Color(0xFF…)` literals are allowed **only** in `Color.kt` (never in screen files).
- `commonMain` must contain no Android- or iOS-specific imports.
- Dark mode is **system-driven only** — no in-app toggle, no `ThemeOptions`.
- Palette is generated with **color match: false** (M3 tonal mapping); brand seeds are primary `#F05D2C`, secondary `#F27619`, tertiary generated (key color `#6B5E2F`).

---

## File Structure

- `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Color.kt` — **rewritten**: full light+dark M3 palette (`*Light`/`*Dark` internal vals) + `QuestionProgressColors` (kept). Old named brand vals (`SoulariumOrange`, …) and `SoulariumLightColors` removed (verified unreferenced outside this package).
- `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Theme.kt` — **rewritten**: `object SoulariumTheme` (schemes + `isDarkThemeActive`) + `fun SoulariumTheme(darkTheme, content)`.
- `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Typography.kt` — untouched.
- `shared/src/commonTest/kotlin/org/cru/soularium/ui/theme/SoulariumThemeTest.kt` — **new**: scheme-wiring test.
- `.claude/CLAUDE.md`, `.claude/rules/design_system_rules.md` — **edited**: reverse the light-only statements.
- Paparazzi golden images under `shared/src/androidHostTest/` — **re-recorded**.

---

## Task 1: New light+dark theme (Color.kt + Theme.kt + wiring test)

`Color.kt` and `Theme.kt` change together — removing `SoulariumLightColors` breaks `Theme.kt`, so they are one deliverable. TDD via a dependency-free scheme-wiring test (composition/`isSystemInDarkTheme()` selection is covered end-to-end by Paparazzi in Task 3, not here — the project has no Compose UI-test dependency and this plan does not add one).

**Files:**
- Rewrite: `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Color.kt`
- Rewrite: `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Theme.kt`
- Test: `shared/src/commonTest/kotlin/org/cru/soularium/ui/theme/SoulariumThemeTest.kt`

**Interfaces:**
- Produces: `object SoulariumTheme` with `internal val lightScheme: ColorScheme`, `internal val darkScheme: ColorScheme`, `internal val LocalDarkThemeActive`, and `val isDarkThemeActive: Boolean` (`@Composable @ReadOnlyComposable`); `@Composable fun SoulariumTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`.
- Produces: `internal val QuestionProgressColors: List<Color>` (unchanged, still consumed by `QuestionPromptLayout`).
- Consumes: `soulariumTypography()` from `Typography.kt` (unchanged).

- [ ] **Step 1: Confirm the generated palette (color match: false)**

The palette below was generated with `material-color-utilities` (the engine behind the M3 Theme Builder) from the brand seeds, color match **false**. The reproduction script is recorded at the end of this task. These are the values to bake into `Color.kt` in the next step — read them once so later steps are a straight transcription.

- [ ] **Step 2: Write the failing test**

Create `shared/src/commonTest/kotlin/org/cru/soularium/ui/theme/SoulariumThemeTest.kt`:

```kotlin
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
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "*SoulariumThemeTest*"`
Expected: FAIL — compilation error (unresolved `primaryLight`, `SoulariumTheme.lightScheme`, etc., not yet defined).

- [ ] **Step 4: Rewrite `Color.kt` with the full palette**

Replace the entire contents of `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Color.kt` with:

```kotlin
package org.cru.soularium.ui.theme

import androidx.compose.ui.graphics.Color

// Generated with the Material 3 Theme Builder engine (material-color-utilities).
// Seeds — primary: #F05D2C, secondary: #F27619, tertiary key color: #6B5E2F (generated).
// Color match: false (M3 tonal mapping). Regeneration script in
// docs/superpowers/plans/2026-07-09-dark-theme.md (Task 1).

internal val primaryLight = Color(0xFFAE3200)
internal val onPrimaryLight = Color(0xFFFFFFFF)
internal val primaryContainerLight = Color(0xFFFFDBD0)
internal val onPrimaryContainerLight = Color(0xFF852400)
internal val secondaryLight = Color(0xFF9A4600)
internal val onSecondaryLight = Color(0xFFFFFFFF)
internal val secondaryContainerLight = Color(0xFFFFDBC9)
internal val onSecondaryContainerLight = Color(0xFF763300)
internal val tertiaryLight = Color(0xFF6B5E2F)
internal val onTertiaryLight = Color(0xFFFFFFFF)
internal val tertiaryContainerLight = Color(0xFFF5E2A7)
internal val onTertiaryContainerLight = Color(0xFF52461A)
internal val errorLight = Color(0xFFBA1A1A)
internal val onErrorLight = Color(0xFFFFFFFF)
internal val errorContainerLight = Color(0xFFFFDAD6)
internal val onErrorContainerLight = Color(0xFF93000A)
internal val backgroundLight = Color(0xFFFFF8F6)
internal val onBackgroundLight = Color(0xFF231917)
internal val surfaceLight = Color(0xFFFFF8F6)
internal val onSurfaceLight = Color(0xFF231917)
internal val surfaceVariantLight = Color(0xFFF5DED7)
internal val onSurfaceVariantLight = Color(0xFF53433F)
internal val outlineLight = Color(0xFF85736E)
internal val outlineVariantLight = Color(0xFFD8C2BC)
internal val scrimLight = Color(0xFF000000)
internal val inverseSurfaceLight = Color(0xFF392E2B)
internal val inverseOnSurfaceLight = Color(0xFFFFEDE8)
internal val inversePrimaryLight = Color(0xFFFFB59E)
internal val surfaceDimLight = Color(0xFFE8D6D1)
internal val surfaceBrightLight = Color(0xFFFFF8F6)
internal val surfaceContainerLowestLight = Color(0xFFFFFFFF)
internal val surfaceContainerLowLight = Color(0xFFFFF1ED)
internal val surfaceContainerLight = Color(0xFFFCEAE5)
internal val surfaceContainerHighLight = Color(0xFFF7E4DF)
internal val surfaceContainerHighestLight = Color(0xFFF1DFDA)

internal val primaryDark = Color(0xFFFFB59E)
internal val onPrimaryDark = Color(0xFF5E1700)
internal val primaryContainerDark = Color(0xFF852400)
internal val onPrimaryContainerDark = Color(0xFFFFDBD0)
internal val secondaryDark = Color(0xFFFFB68D)
internal val onSecondaryDark = Color(0xFF532200)
internal val secondaryContainerDark = Color(0xFF763300)
internal val onSecondaryContainerDark = Color(0xFFFFDBC9)
internal val tertiaryDark = Color(0xFFD8C68D)
internal val onTertiaryDark = Color(0xFF3A3005)
internal val tertiaryContainerDark = Color(0xFF52461A)
internal val onTertiaryContainerDark = Color(0xFFF5E2A7)
internal val errorDark = Color(0xFFFFB4AB)
internal val onErrorDark = Color(0xFF690005)
internal val errorContainerDark = Color(0xFF93000A)
internal val onErrorContainerDark = Color(0xFFFFDAD6)
internal val backgroundDark = Color(0xFF1A110F)
internal val onBackgroundDark = Color(0xFFF1DFDA)
internal val surfaceDark = Color(0xFF1A110F)
internal val onSurfaceDark = Color(0xFFF1DFDA)
internal val surfaceVariantDark = Color(0xFF53433F)
internal val onSurfaceVariantDark = Color(0xFFD8C2BC)
internal val outlineDark = Color(0xFFA08C87)
internal val outlineVariantDark = Color(0xFF53433F)
internal val scrimDark = Color(0xFF000000)
internal val inverseSurfaceDark = Color(0xFFF1DFDA)
internal val inverseOnSurfaceDark = Color(0xFF392E2B)
internal val inversePrimaryDark = Color(0xFFAE3200)
internal val surfaceDimDark = Color(0xFF1A110F)
internal val surfaceBrightDark = Color(0xFF423734)
internal val surfaceContainerLowestDark = Color(0xFF140C0A)
internal val surfaceContainerLowDark = Color(0xFF231917)
internal val surfaceContainerDark = Color(0xFF271D1B)
internal val surfaceContainerHighDark = Color(0xFF322825)
internal val surfaceContainerHighestDark = Color(0xFF3D322F)

// Fixed teal→blue per-question accent sequence (one color per question).
// Not part of the M3 scheme — index into it by question number.
val QuestionProgressColors =
    listOf(
        Color(0xFF17BD97),
        Color(0xFF16A986),
        Color(0xFF1C9AA4),
        Color(0xFF25A9C4),
        Color(0xFF1680BD),
    )
```

- [ ] **Step 5: Rewrite `Theme.kt` with the object + composable**

Replace the entire contents of `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Theme.kt` with:

```kotlin
package org.cru.soularium.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

object SoulariumTheme {
    internal val lightScheme = lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

    internal val darkScheme = darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )

    internal val LocalDarkThemeActive = staticCompositionLocalOf { false }

    val isDarkThemeActive: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalDarkThemeActive.current
}

@Composable
fun SoulariumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = if (darkTheme) SoulariumTheme.darkScheme else SoulariumTheme.lightScheme,
    typography = soulariumTypography(),
) {
    CompositionLocalProvider(
        SoulariumTheme.LocalDarkThemeActive provides darkTheme,
        content = content,
    )
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "*SoulariumThemeTest*"`
Expected: PASS (3 tests).

- [ ] **Step 7: Verify ktlint + iOS compile**

Run: `./gradlew ktlintCheck :shared:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL. (Confirms no line >120, and `commonMain` compiles for iOS — `isSystemInDarkTheme` resolves on both targets. Existing call sites `App.kt` and `BasePaparazziTest` still compile because `darkTheme` defaults.)

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Color.kt \
        shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/Theme.kt \
        shared/src/commonTest/kotlin/org/cru/soularium/ui/theme/SoulariumThemeTest.kt
git commit -m "feat(ui): add Material 3 dark theme and mpdx-style theme object

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

**Regeneration script (reference — for future palette changes):**

`npm install @material/material-color-utilities esbuild`, then bundle+run this ESM (the package ships ESM with extensionless imports, so bundle with esbuild rather than running directly):

```js
import { Hct, TonalPalette, DynamicScheme, Variant, SchemeTonalSpot,
         MaterialDynamicColors, hexFromArgb, argbFromHex } from '@material/material-color-utilities';
const PRIMARY = argbFromHex('F05D2C'), SECONDARY = argbFromHex('F27619');
const base = new SchemeTonalSpot(Hct.fromInt(PRIMARY), false, 0);
const build = (isDark) => new DynamicScheme({
  sourceColorHct: Hct.fromInt(PRIMARY), variant: Variant.TONAL_SPOT, contrastLevel: 0, isDark,
  primaryPalette: TonalPalette.fromInt(PRIMARY), secondaryPalette: TonalPalette.fromInt(SECONDARY),
  tertiaryPalette: base.tertiaryPalette, neutralPalette: base.neutralPalette,
  neutralVariantPalette: base.neutralVariantPalette,
});
const roles = ['primary','onPrimary','primaryContainer','onPrimaryContainer','secondary','onSecondary',
  'secondaryContainer','onSecondaryContainer','tertiary','onTertiary','tertiaryContainer','onTertiaryContainer',
  'error','onError','errorContainer','onErrorContainer','background','onBackground','surface','onSurface',
  'surfaceVariant','onSurfaceVariant','outline','outlineVariant','scrim','inverseSurface','inverseOnSurface',
  'inversePrimary','surfaceDim','surfaceBright','surfaceContainerLowest','surfaceContainerLow','surfaceContainer',
  'surfaceContainerHigh','surfaceContainerHighest'];
const emit = (s, sfx) => roles.forEach(r =>
  console.log(`internal val ${r}${sfx} = Color(0xFF${hexFromArgb(MaterialDynamicColors[r].getArgb(s)).slice(1).toUpperCase()})`));
emit(build(false), 'Light'); emit(build(true), 'Dark');
```

---

## Task 2: Reverse the "light theme only" docs

**Files:**
- Modify: `.claude/CLAUDE.md`
- Modify: `.claude/rules/design_system_rules.md`

**Interfaces:** none (documentation only).

- [ ] **Step 1: Update the Theme bullet in `CLAUDE.md`**

Edit `.claude/CLAUDE.md`, replacing:

```
- **Theme**: `SoulariumTheme { }` (a thin Material3 wrapper) is applied once at the app
  root. Light theme only (dark mode is a deliberate non-goal for v2). See
  `.claude/rules/design_system_rules.md`.
```

with:

```
- **Theme**: `SoulariumTheme { }` (a thin Material3 wrapper) is applied once at the app
  root. Provides light and dark Material3 color schemes, selected automatically via
  `isSystemInDarkTheme()`. See `.claude/rules/design_system_rules.md`.
```

- [ ] **Step 2: Update `SoulariumTheme` snippet in design_system_rules.md §2**

Edit `.claude/rules/design_system_rules.md`, replacing the §2 code block:

```kotlin
@Composable
fun SoulariumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SoulariumLightColors,
        typography = soulariumTypography(),
        content = content,
    )
}
```

with:

```kotlin
@Composable
fun SoulariumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = MaterialTheme(
    colorScheme = if (darkTheme) SoulariumTheme.darkScheme else SoulariumTheme.lightScheme,
    typography = soulariumTypography(),
) { /* provides SoulariumTheme.isDarkThemeActive */ }
```

- [ ] **Step 3: Replace the "Light theme only" bullet in §2**

In `.claude/rules/design_system_rules.md` §2, replace:

```
- **Light theme only.** Dark mode is a deliberate non-goal for v2 — there is no
  `darkColorScheme`, and the theme does not branch on `isSystemInDarkTheme()`. Do not add
  dark-mode branching; if dark mode is ever needed it will be a project-level decision.
```

with:

```
- **Light and dark.** The app ships both `lightColorScheme` and `darkColorScheme`
  (in `object SoulariumTheme`) and selects between them via `isSystemInDarkTheme()` —
  it follows the system setting. There is no in-app theme toggle. Read
  `SoulariumTheme.isDarkThemeActive` inside a `@Composable` when behavior must branch on
  the active mode; prefer letting `MaterialTheme.colorScheme.*` tokens adapt automatically.
```

- [ ] **Step 4: Update §3 palette description**

In `.claude/rules/design_system_rules.md` §3, replace the "Brand palette" intro sentence and table (the block starting `` `Color.kt` defines the brand palette `` through the end of the hex table ending at the `SoulariumError` row) with:

```
`Color.kt` holds a **full Material 3 palette** — every scheme slot in both light and
dark (`*Light` / `*Dark` internal vals) — generated with the Material 3 Theme Builder
engine (`material-color-utilities`, color match: false) from Soularium's brand seeds:

| Role seed | Hex |
|---|---|
| primary | `#F05D2C` |
| secondary | `#F27619` |
| tertiary (generated) | key color `#6B5E2F` |

The generated slots are assembled into `SoulariumTheme.lightScheme` / `darkScheme` in
`Theme.kt`. To change the brand, edit the seeds and regenerate (script in
`docs/superpowers/plans/2026-07-09-dark-theme.md`) — do not hand-tune individual slots.
```

- [ ] **Step 5: Update rule #11 in the §13 translation-rules list**

In `.claude/rules/design_system_rules.md` §13, replace:

```
11. **No dark-mode branching** — the app is light-only by design.
```

with:

```
11. **Dark mode follows the system** — the theme selects light/dark via
    `isSystemInDarkTheme()`; do not add an in-app toggle.
```

(Leave rule #12 "Accessibility is required, not optional." unchanged — it is a distinct
rule. Step 6's grep catches any other stale "no dark-mode branching" phrasing.)

- [ ] **Step 6: Verify no stale light-only claims remain**

Run: `grep -rni "light theme only\|light-only\|dark mode is a deliberate non-goal\|no dark-mode branching\|SoulariumLightColors" .claude/`
Expected: no matches.

- [ ] **Step 7: Commit**

```bash
git add .claude/CLAUDE.md .claude/rules/design_system_rules.md
git commit -m "docs: document light+dark theme, retire light-only rule

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Re-record Paparazzi snapshots

Existing tests (`IntroLayoutPaparazziTest`, `TermsLayoutPaparazziTest`) already parameterize over `NightMode` (`@TestParameter`), so NIGHT + NOTNIGHT goldens already exist. After Task 1 the NOTNIGHT images shift (new palette) and the NIGHT images become genuinely dark (previously light because the theme ignored `isSystemInDarkTheme()`). Golden images must be re-recorded, then verification must pass.

**Files:**
- Re-record: Paparazzi golden PNGs under `shared/src/androidHostTest/` (generated, committed by the record flow).

**Interfaces:** none.

- [ ] **Step 1: Record updated snapshots**

Use the **record-snapshots skill** (pushes the branch, runs the `record-snapshots.yml`
workflow — `./gradlew :shared:cleanRecordPaparazzi -Ppaparazzi` — and folds the resulting
snapshot commit back onto this branch). If recording locally instead, run:

Run: `./gradlew :shared:cleanRecordPaparazzi -Ppaparazzi`
Expected: BUILD SUCCESSFUL; golden PNGs under `shared/src/androidHostTest/` updated.

- [ ] **Step 2: Spot-check that NIGHT goldens are actually dark**

Open one regenerated NIGHT golden (a filename containing `NIGHT`, e.g. the Intro or Terms
night variant) and confirm the background is dark (`#1A110F`-ish), not light. Also open a
NOTNIGHT golden and confirm it renders with the new light palette.
Expected: NIGHT variants are visibly dark; NOTNIGHT variants light.

- [ ] **Step 3: Verify snapshots pass**

Run: `./gradlew :shared:verifyPaparazzi -Ppaparazzi`
Expected: BUILD SUCCESSFUL — recorded goldens match rendering.

- [ ] **Step 4: Commit (if the record flow left goldens uncommitted)**

```bash
git add shared/src/androidHostTest
git commit -m "test(ui): re-record Paparazzi snapshots for light+dark theme

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

(The record-snapshots skill may have already committed these; skip if `git status` is clean.)

---

## Open Items (follow-up, not blocking this plan)

- Audit `QuestionProgressColors` contrast against dark `surface`/`onSurface`; adjust if any
  accent fails contrast in dark mode. Tracked separately from this plan.

## Self-Review Notes

- **Spec coverage:** Color generation (Task 1), theme composable mpdx-shape trimmed of
  `ThemeOptions` (Task 1), `isDarkThemeActive` retained (Task 1), system-driven activation
  (Task 1 `isSystemInDarkTheme()` default + Task 3 NIGHT snapshots), docs reversal (Task 2),
  Paparazzi re-record with no new variants (Task 3), `QuestionProgressColors` kept + audit
  deferred (Task 1 + Open Items). Testing fallback (no `runComposeUiTest` dep) is honored
  by the dependency-free wiring test + Paparazzi.
- **Type consistency:** `lightScheme`/`darkScheme`/`isDarkThemeActive`/`LocalDarkThemeActive`
  and the `*Light`/`*Dark` color-val names are identical across the test, `Color.kt`, and
  `Theme.kt`.
