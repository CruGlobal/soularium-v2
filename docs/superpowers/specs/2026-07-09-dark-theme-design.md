# Material 3 Dark Theme — Design

**Date:** 2026-07-09
**Status:** Approved (pending spec review)

## Goal

Add a Material 3 **dark theme** to Soularium v2 and restructure the theme composable to
mirror the pattern used in the [`mpdx-kmp`](https://github.com/CruGlobal/mpdx-kmp)
project (`ui/theme/MpdxTheme.kt`). Dark mode **follows the system setting** and goes live
immediately.

This reverses the previously documented "light theme only" non-goal — that reversal is
intentional and part of this work (see §5).

## Decisions

| Question | Decision |
|---|---|
| Where do dark + expanded color values come from? | **Generate the full M3 palette** (light + dark) from Soularium's brand seeds via `material-color-utilities` (the engine behind the M3 Theme Builder). |
| How does dark mode activate? | **Follows the system setting** — `darkTheme = isSystemInDarkTheme()` by default. Live now. |
| Tertiary seed (Soularium has none today) | **Generated / harmonized** toward the warm orange primary; exact hexes shown for approval during implementation. |
| How faithfully to mirror mpdx machinery? | **Trim to single-app needs** — adopt the `object`-holding-schemes structure + `isDarkThemeActive` accessor; **drop `ThemeOptions`** (no second consumer). |

## Architecture

Files stay in `shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/`.

### `Color.kt` — regenerated, full slot set

Replace the current partial hand-picked palette with a **complete** M3 scheme, both light
and dark, using mpdx's `*Light` / `*Dark` internal-val naming. Slots covered (each in
light and dark):

`primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `secondary`,
`onSecondary`, `secondaryContainer`, `onSecondaryContainer`, `tertiary`, `onTertiary`,
`tertiaryContainer`, `onTertiaryContainer`, `error`, `onError`, `errorContainer`,
`onErrorContainer`, `background`, `onBackground`, `surface`, `onSurface`,
`surfaceVariant`, `onSurfaceVariant`, `outline`, `outlineVariant`, `scrim`,
`inverseSurface`, `inverseOnSurface`, `inversePrimary`, `surfaceDim`, `surfaceBright`,
`surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`,
`surfaceContainerHigh`, `surfaceContainerHighest`.

**Seeds:**
- primary `#F05D2C` (SoulariumOrange)
- secondary `#F27619` (SoulariumOrangeLight)
- tertiary — generated/harmonized toward the orange primary (hexes surfaced for approval)

Neutrals derive from the primary hue. Generation is done with `material-color-utilities`
so values are real HCT output, not eyeballed. The exact generated hexes are shown to the
user before finalizing.

**Consequence (accepted):** the existing hand-picked light hexes (e.g. background
`#ECEAEB`) shift to generated equivalents. Brand *seeds* are preserved; derived slots are
regenerated. This is the intended effect of "generate the full palette."

`QuestionProgressColors` (the fixed teal→blue per-question accent list) is **kept as-is** —
it is not part of the M3 scheme. Follow-up: audit its contrast on dark surfaces (see
Open Items).

### `Theme.kt` — mpdx shape, trimmed

```kotlin
object SoulariumTheme {
    internal val lightScheme = lightColorScheme(/* all *Light slots */)
    internal val darkScheme  = darkColorScheme(/* all *Dark slots */)
    internal val LocalDarkThemeActive = staticCompositionLocalOf { false }
    val isDarkThemeActive: Boolean
        @Composable @ReadOnlyComposable get() = LocalDarkThemeActive.current
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

- No `ThemeOptions`.
- `Typography.kt` / `soulariumTypography()` untouched.
- **Call sites unchanged.** `App.kt`'s `SoulariumTheme { }` and `BasePaparazziTest`'s
  `SoulariumTheme(content = content)` still compile — the new `darkTheme` param defaults.
  The param exists so Paparazzi's night-mode config can drive dark rendering.
- iOS + Android both support `isSystemInDarkTheme()` in Compose Multiplatform.

## Docs update (§5 — reversing the non-goal)

- `.claude/CLAUDE.md` — UI-layer Theme bullet ("Light theme only (dark mode is a
  deliberate non-goal for v2)") updated to state dark mode is supported and follows the
  system.
- `.claude/rules/design_system_rules.md` — §2 light-only paragraph and translation rules
  #11 / #12 ("No dark-mode branching") updated to describe the new light+dark theme and
  the `isSystemInDarkTheme()`-driven selection.

## Testing

- **Theme unit tests** mirroring `MpdxThemeTest`: dark scheme applied when
  `darkTheme = true`, light scheme when `false`, and `isDarkThemeActive` reflects the
  active mode. Uses Compose UI test under Robolectric via `@RunOnAndroidWith(AndroidJUnit4::class)`,
  matching the existing presenter-test convention. Verify the `runComposeUiTest` (v2)
  dependency is available before committing to that exact form; fall back to the
  project's existing Compose-test entry point if not.
- **Paparazzi — no new variants needed.** `IntroLayoutPaparazziTest` and
  `TermsLayoutPaparazziTest` already parameterize over `NightMode` (`@TestParameter`),
  so NIGHT + NOTNIGHT snapshots already exist. Re-record all snapshots:
  - NOTNIGHT variants shift (palette regenerated).
  - NIGHT variants become genuinely dark (previously rendered light because
    `SoulariumTheme` ignored `isSystemInDarkTheme()`).

## Open Items (follow-up, not blocking)

- Audit `QuestionProgressColors` contrast on dark surfaces; adjust if any accent fails
  contrast against `surface`/`onSurface` in dark mode.

## Non-Goals

- No user-facing in-app theme toggle (system setting only).
- No `ThemeOptions` / multi-app override layer.
- No changes to typography or shapes.
