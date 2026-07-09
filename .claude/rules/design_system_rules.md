# Design System Rules — Soularium v2 (Compose Multiplatform)

This document defines the design-system conventions for Soularium v2 so that UI work —
whether translating a Figma frame or building a screen from the spec — stays consistent.

The shared design tokens live in `:shared` under
`shared/src/commonMain/kotlin/org/cru/soularium/ui/theme/` (`Theme.kt`, `Color.kt`,
`Typography.kt`) and are surfaced through Material3's `MaterialTheme.*` API. There is **no
white-label layer** — Soularium v2 is a single Cru-branded app.

---

## 1. Technology Stack

| Layer | Technology |
|---|---|
| UI framework | Compose Multiplatform 1.11.1 (shared `commonMain` for Android + iOS) |
| Component library | Material3 (`org.jetbrains.compose.material3`) |
| Iconography | `compose-material-icons-extended` (`androidx.compose.material.icons.Icons.*`) |
| Theme entry point | `SoulariumTheme(content)` in `ui/theme/Theme.kt` |
| Image loading | Coil 3 (`coil-compose`) for remote/large images; `painterResource` for bundled drawables |
| Resources | Compose Multiplatform resources under `shared/src/commonMain/composeResources/` |

Apply `SoulariumTheme { }` **once**, at the app root. Inside any `@Composable`, read
tokens from `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, and
`MaterialTheme.shapes.*`.

---

## 2. Theme Entry Point — `SoulariumTheme`

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

- `SoulariumTheme` is a thin Material3 wrapper. Feature screens must **not** call
  `MaterialTheme(...)` or `SoulariumTheme(...)` themselves.
- **Light and dark.** The app ships both `lightColorScheme` and `darkColorScheme`
  (in `object SoulariumTheme`) and selects between them via `isSystemInDarkTheme()` —
  it follows the system setting. There is no in-app theme toggle. Read
  `SoulariumTheme.isDarkThemeActive` inside a `@Composable` when behavior must branch on
  the active mode; prefer letting `MaterialTheme.colorScheme.*` tokens adapt automatically.
- `Shapes` are the Material3 defaults — no custom shape set is defined.

---

## 3. Color Tokens

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

### Question progression accent

`QuestionProgressColors` is a `List<Color>` of 5 teal→blue values used to tint
per-question progress UI (one color per question). Index into it by question number; do
not hardcode these hexes elsewhere.

### Adding new colors

1. **Preferred:** reuse the closest Material3 token — the scheme covers more than its
   names suggest (`surfaceVariant`, `outlineVariant`, `secondaryContainer`).
2. If a genuinely new brand color is needed, add it to `Color.kt` as a named `val` and,
   if it belongs in the scheme, wire it into both `SoulariumTheme.lightScheme` and
   `darkScheme`. Document why no existing token fit.
3. **Never** inline a `Color(0xFF…)` literal in a screen file — that is a code-review
   blocker. The only acceptable inline `Color` values are `Color.Transparent` and
   `Color.Unspecified` as sentinel arguments.

---

## 4. Typography

Soularium uses a **custom Open Sans** font family (8 weight/style TTFs in
`composeResources/font/`), assembled by `openSansFamily()` and applied via
`soulariumTypography()`. Use the Material3 typography slots — do not pass raw `fontSize`
/ `fontWeight` to `Text`.

| Slot | Weight | Size |
|---|---|---|
| `displayLarge` | Light | 48sp |
| `displayMedium` | Light | 36sp |
| `headlineLarge` | Bold | 32sp |
| `headlineMedium` | SemiBold | 24sp |
| `headlineSmall` | SemiBold | 20sp |
| `titleLarge` | SemiBold | 20sp |
| `titleMedium` | SemiBold | 16sp |
| `titleSmall` | SemiBold | 14sp |
| `bodyLarge` | Normal | 16sp |
| `bodyMedium` | Normal | 14sp |
| `bodySmall` | Normal | 12sp |
| `labelLarge` | SemiBold | 14sp |
| `labelMedium` | SemiBold | 12sp |
| `labelSmall` | SemiBold | 11sp |

```kotlin
Text(
    text = stringResource(Res.string.home_hero_title),
    style = MaterialTheme.typography.headlineLarge,
    color = MaterialTheme.colorScheme.onSurface,
)
```

If a new typographic intent appears, add it as a slot inside `soulariumTypography()` —
not as a raw `TextStyle` in a screen.

---

## 5. Spacing

Compose uses `dp` for layout, `sp` for text. Stay on a **4dp grid**.

| Value | Use |
|---|---|
| `4.dp` | icon ↔ inline label |
| `8.dp` | adjacent inline elements |
| `12.dp` | inside compact components |
| `16.dp` | screen padding, section spacing |
| `24.dp` | between unrelated sections |
| `32.dp` | hero / top-of-screen spacing |
| `48.dp` | minimum touch target |

Prefer `Arrangement.spacedBy(N.dp)` on a `Column`/`Row` over per-child padding —
controlled from the parent, easier to reason about. Per-element `Modifier.padding` is
fine when only one child needs an offset.

---

## 6. Shape & Elevation

- Shapes come from `MaterialTheme.shapes.*` (Material3 defaults: `extraSmall` 4dp,
  `small` 8dp, `medium` 12dp, `large` 16dp, `extraLarge` 28dp). Use `CircleShape` for
  fully circular elements. Avoid hand-rolling `RoundedCornerShape(...)` when a token fits.
- Prefer Material3 **tonal** elevation (`surfaceContainer*` / component defaults) over
  raw `shadowElevation`. Use `CardDefaults` / `TopAppBarDefaults` rather than supplying
  raw `dp` elevation values.

---

## 7. Components

Always reach for the Material3 component before building from primitives:

- Buttons: `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton`, `IconButton`.
- Containers: `Card`, `ElevatedCard`, `OutlinedCard`, `Surface`.
- Input: `OutlinedTextField` / `TextField`; `Checkbox`, `Switch`, `RadioButton`,
  `FilterChip`.
- Structure: `Scaffold`, `TopAppBar`, `LazyColumn`/`LazyRow`/`LazyVerticalGrid`.
- Feedback: `SnackbarHost` + `SnackbarHostState`, `AlertDialog`.

Card images are bundled drawables — load them with `painterResource(Res.drawable.card_NN)`.

---

## 8. Icons

Use `androidx.compose.material.icons.Icons.*` (`Icons.Default.*`, `Icons.Outlined.*`,
`Icons.Filled.*`, …). Every `Icon`/`Image` **must** have a `contentDescription`; pass
`null` only when the element is purely decorative and adjacent text already conveys the
meaning. Do not hand-roll `ImageVector` paths.

---

## 9. Layout & Screen Conventions

Soularium screens follow a consistent Circuit contract (see existing screens under
`ui/screens/` and `ui/conversation/`):

- Each screen is a `Presenter` + `Layout` pair, split across two files in the same
  package: `<Feature>Presenter.kt` (the Presenter class with nested `UiState` and
  `UiEvent`) and `<Feature>Layout.kt` (the Layout composable). The Layout is a
  **public, stateless** `@Composable fun <Feature>Layout(state: <Feature>Presenter.UiState,
  modifier: Modifier = Modifier)` — it reads fields off `state` and emits intent via
  `state.eventSink(...)`. Layouts never take a Presenter or repository directly.
- `modifier: Modifier = Modifier` is always the **last** parameter, and is the first
  thing applied to the layout's root composable.
- State is derived inside the Presenter's `@Composable present()` body (`remember`,
  `LaunchedEffect`, `collectAsState()` over repository flows) and exposed via the nested
  `UiState`. Layout-local `remember { mutableStateOf(...) }` is acceptable only for
  transient view-only state (e.g. a text-field draft, an expanded/collapsed toggle).
- Private helper composables within a screen file are `private`.

### Modifier order

```kotlin
Box(
    modifier = modifier          // 1. caller-provided, first
        .fillMaxWidth()          // 2. size
        .padding(16.dp)          // 3. spacing
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clip(MaterialTheme.shapes.medium)
        .clickable { onClick() } // 4. interaction
        .semantics { },          // 5. accessibility
)
```

### Lists

Use `LazyColumn`/`LazyRow` with a stable `key = { it.id }` whenever items can be
added/removed/reordered.

---

## 10. Accessibility

- Every `Icon`/`Image` has a meaningful `contentDescription` (or `null` with
  justification).
- Touch targets are ≥ 48dp — wrap small icons in `IconButton`, or use
  `Modifier.minimumInteractiveComponentSize()`.
- Composables that act as buttons but aren't a `Button` carry
  `Modifier.semantics { role = Role.Button }`.
- Heading-like text uses `Modifier.semantics { heading() }`.
- Form fields chain with `KeyboardOptions(imeAction = ImeAction.Next)` and `Done` on the
  last field.
- Live status changes use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`.

---

## 11. Resources & Strings

- User-visible strings come from Compose resources: `stringResource(Res.string.<key>)`.
  Never inline user-facing English in a screen.
- The generated resource accessor is `org.cru.soularium.generated.resources.Res`
  (set via `compose.resources.packageOfResClass` in `:shared`'s build script).
  Import `Res` plus the specific keys you use.
- Source strings: `shared/src/commonMain/composeResources/values/strings.xml`.
  Translations live in `values-es/`, `values-fr/`, `values-pl/`, `values-zh-rCN/` and are
  managed through Crowdin — do not hand-edit translated files.
- Drawables (50 card images + thumbnails) live under `composeResources/drawable/`;
  reference via `painterResource(Res.drawable.<name>)`. Fonts live under
  `composeResources/font/`.

---

## 12. Loading / Error / Empty States

Every screen that loads data must show distinct UI for loading, error, and empty cases —
not just the populated state. Since this app is offline-first, "error" is rare but
persistence failures (`DomainError.PersistenceFailed`) still need a user-facing path.

---

## 13. Translation Rules (Figma / spec → Compose)

1. Use Material3 **components** first; build from primitives only when none fits.
2. Use Material3 **color tokens** (`MaterialTheme.colorScheme.*`) — raw `Color(0xFF…)`
   in a screen is a code-review blocker.
3. Use Material3 **typography slots** (`MaterialTheme.typography.*`) — no raw `fontSize`
   in a screen.
4. Use Material3 **shape tokens**; `CircleShape` for circular elements.
5. Spacing on the **4dp grid**; prefer `Arrangement.spacedBy`.
6. Icons from `compose-material-icons-extended`; no hand-rolled vector paths.
7. Layouts use Compose primitives (`Column`/`Row`/`Box`/`Lazy*`/`Scaffold`) — no XML
   views, no `AndroidView` in `commonMain`.
8. `modifier` is the last parameter, applied first; every public composable forwards it.
9. State is derived in the Presenter and exposed via `UiState`; Layouts stay stateless.
10. Loading / error / empty states are first-class.
11. **Dark mode follows the system** — the theme selects light/dark via
    `isSystemInDarkTheme()`; do not add an in-app toggle.
12. Accessibility is required, not optional.
13. Strings come from `stringResource(Res.string.*)`.
14. No platform-specific imports in `commonMain` UI — bridge via `expect`/`actual`.
