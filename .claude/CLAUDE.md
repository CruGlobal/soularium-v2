# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Soularium v2 is a Cru-internal mobile rebuild of the discontinued **Soularium** and
**MySoularium** apps, built with Kotlin Multiplatform (KMP) and Compose Multiplatform
(CMP) for Android and iOS from a single codebase. It is an **offline-first**
conversation tool: a facilitator and one or more participants move through 5 questions,
selecting from 50 bundled card images, and the app generates a shareable summary link.

There are no accounts, no auth, no cloud sync, and no GraphQL/network API — content is
bundled and persistence is local.

## Repository Layout & Build

**The Gradle project lives at the repo root** — run all Gradle commands from there, using
the repo's Gradle wrapper (`./gradlew`), never a system Gradle. The JDK is pinned via
`.tool-versions`; source/target bytecode is JVM 17.

The app is `:androidApp` (an Android-only shell), `:shared` (domain ports, UI, DI), and
focused KMP library modules under `module/` (`com.android.kotlin.multiplatform.library`)
— `settings.gradle.kts` is the authoritative module list; `iosApp/` is the native SwiftUI
shell hosting the Compose framework. A module may have a sibling `test-fixtures` module
exposing its shared test doubles (e.g. `FakeSessionRepository`, `FakeGameEngine`) to other
modules' `commonTest` source sets. Shared build logic lives in the `build-logic/`
composite build's convention plugins. A module lists
`soularium-kmp.module-conventions` explicitly even when it also applies the
metro/serialization conventions, so its KMP-library nature is obvious; a `test-fixtures`
module instead applies `soularium-kmp.test-fixtures-conventions` (module-conventions with
Kover disabled). Exact dependency versions are pinned in `gradle/libs.versions.toml` — the
single source of truth.

## Architecture: Hexagonal

`:androidApp` → `:shared` → library modules; library modules depend only on other
library modules, never on `:shared`.

Layering is enforced by package convention: code in `org.cru.soularium.domain`
must not import from `data`, `ui`, or platform packages, and `org.cru.soularium.data`
must not import from `ui`. Domain and game logic that live in their own library module
(rather than under `:shared`) have their isolation from `data`/`ui` enforced by the
module dependency graph rather than by convention.

### Domain & game logic (`org.cru.soularium.domain`, `:module:game`)

- **Ports**: `domain/ports/` retains `DeviceStateRepository`. `AnalyticsTracker` lives in
  `:module:analytics`. (Crash/error reporting is not a port — code logs through the global
  Kermit `Logger`; see "Logging & crash reporting".)
- **Game engine** (`:module:game`, `org.cru.soularium.game`): `GameEngine` is an
  interface — instances come from the graph via the nested `GameEngine.Factory`
  (assisted-injected internally); tests construct the internal
  `GameEngineImpl` directly or script a `FakeGameEngine`. It owns the game
  loop — it exposes `StateFlow<GameState>` (persisted `SessionState` + volatile
  context), `dispatch(event)`, rehydration via `start()`, summary reads via
  `loadSummaries()`, and serialized effect execution through the engine-internal
  `GameEngineImpl.Host` port (implemented in `:module:game` over
  `SessionRepository`). The per-state transition logic is **pure** private methods on
  the engine — no I/O; side effects are returned as `Effect` data and executed by the
  engine's FIFO queue, which drains on `close()` so navigation can't drop writes. The
  persisted state (`SessionState`) lives in `:module:model`
  (`org.cru.soularium.model.game`). The content catalog the engine consumes (the
  `Question` enum) lives at `org.cru.soularium.game.content`.
- **Errors**: `GameError` sealed interface (`:module:game`). There is no `Result<T>`
  wrapper — an invalid transition logs a `transition_error` analytics event and leaves
  state unchanged.
- Domain code stays independent of the `data` and `ui` layers, but it **may** use platform
  APIs — a domain port can have Android/iOS `actual` implementations that use them (e.g.
  `domain.settings.AndroidLanguageRepository` uses `Context`). Domain avoids Compose UI, with
  one exception: lightweight multiplatform value types such as Compose's `Locale`
  (`androidx.compose.ui.text.intl.Locale`), treated as a data model.

### Model layer (`:module:model`, `org.cru.soularium.model`)

- `@SerialName` on `ContactInfo` and `SessionState` variants pins the JSON wire format —
  do not change an existing `@SerialName`; it would orphan persisted sessions.

### Data layer (`org.cru.soularium.data`, in `:shared`)

- Device flags (intro seen, ToS agreed) persist via DataStore Preferences, not Room. The
  app language is not stored here — it is the platform per-app language setting, read
  through `LanguageRepository`.
- Repositories map persistence rows ↔ domain models; the mapping must be total (no `!!`
  on optional columns).

### Persistence — Room (`:module:db`, `org.cru.soularium.db`)

- **Foreign-key enforcement is on by default** with Room 2.8's drivers — there is no
  manual `PRAGMA foreign_keys = ON`.
- **`SessionState` is persisted as a JSON snapshot string** (`state_snapshot_json`
  column). Renaming or removing a `@Serializable` field in the session-state hierarchy
  breaks already-persisted sessions — treat such changes as schema changes.
- Exported Room schema JSON lives in `module/db/schemas/`. A `@Database` version bump
  must ship a matching schema JSON and migration.

### UI layer (`org.cru.soularium.ui`) — Circuit conventions

- **Navigation**: screens are `@Parcelize` `data object`/`data class` `Screen` types —
  most in `ui/nav/Screens.kt`, but a self-contained feature package may co-locate its own
  `Screen` next to its Presenter/Layout (e.g. `ui/terms/TermsScreen.kt`). Presenters and
  Layouts are wired to their Screen by `@CircuitInject(SomeScreen::class, AppScope::class)`
  — Metro generates the matching `Presenter.Factory` / `Ui.Factory` at compile time; there
  is no hand-written switch table. Cross-screen navigation is
  `navigator.goTo(SomeScreen(...))` from inside a Presenter; back is `navigator.pop()`.
- **Presenters** implement Circuit's `Presenter<UiState>`. The default shape is a
  nested `data class UiState(... val eventSink: (UiEvent) -> Unit) : CircuitUiState`
  paired with a `sealed interface UiEvent : CircuitUiEvent`. When a single Presenter
  drives several visually distinct pages (e.g. the conversation flow), `UiState` may
  instead be a `sealed interface UiState : CircuitUiState` with one `data class`
  subtype per page; each subtype carries only the props its page renders and exposes
  the shared `eventSink` (and any other cross-page fields) via interface properties.
  In that case, `UiEvent` may nest page-specific events under sealed sub-interfaces
  named after their owning `UiState` subtype (e.g. `UiEvent.Selection.ToggleCard`),
  with global events at the top level — see `ConversationPresenter` for the
  canonical example. The `@Composable present()` body uses
  `remember { mutableStateOf(...) }` + `LaunchedEffect`/`produceState` to derive state
  from repositories (collected via `collectAsState()`); user intent flows in through
  `state.eventSink(...)`. Each Presenter lives in its own file named
  `<Feature>Presenter.kt`. Presenters are `@AssistedInject` classes with `@Assisted`
  `Navigator` (and `@Assisted` `Screen` when the screen instance carries arguments);
  remaining constructor parameters are normal injected dependencies from the graph.
  A nested `@CircuitInject(<Feature>Screen::class, AppScope::class) @AssistedFactory
  fun interface Factory { fun create(navigator: Navigator): <Feature>Presenter }`
  drives codegen. Direct construction (e.g. from tests) is still allowed.
- **Layouts** are public, stateless `@Composable` functions named `<Feature>Layout`,
  paired one-to-one with a Presenter and living in their own file named
  `<Feature>Layout.kt`. The signature is
  `fun <Feature>Layout(state: <Feature>Presenter.UiState, modifier: Modifier = Modifier)`;
  `modifier` is the **last** parameter and is applied first on the root composable. The
  Layout reads fields off `state` and emits events via `state.eventSink(...)` — it owns
  no business logic. Private sub-composables within the file are `private`. The Layout
  carries `@CircuitInject(<Feature>Screen::class, AppScope::class)` directly on its
  `@Composable` declaration so Metro generates the matching `Ui.Factory`.
- **Theme**: `SoulariumTheme { }` (a thin Material3 wrapper) is applied once at the app
  root; light/dark is selected automatically via `isSystemInDarkTheme()`. See
  `.claude/rules/design_system_rules.md`.

### Dependency Injection — Metro

- The graph is `SoulariumAppGraph` (`@DependencyGraph(AppScope::class)`), built once per
  platform (`SoulariumApplication` on Android, `MainViewController.kt` on iOS).
  **Contributions merge across modules** — every Metro module applies `metro-conventions`
  (`generateContributionProviders`), so `:module:db`'s bindings land in the `:shared`
  graph. Add new app-wide types by giving the implementation `@Inject` +
  `@ContributesBinding(AppScope::class)`, or by adding a `@Provides` to one of the
  `@BindingContainer @ContributesTo(AppScope::class)` containers. (Exception: the
  Room-backed `SessionRoomRepository` is a `@Dao` — Room constructs it — so it is
  provided by `RoomBindings` rather than `@ContributesBinding`.)
- **Adding a screen**: declare a `Screen` — either in `ui/nav/Screens.kt` or co-located
  in a self-contained feature package — then create `<Feature>Presenter.kt` and
  `<Feature>Layout.kt` annotated with `@CircuitInject(...)` (see above). Metro generates
  and contributes the factories — no factory registration is required.

### Logging & crash reporting

There is no `CrashReporter` port. Code logs through the **global Kermit `Logger`** (each
file keeps a `private val logger = Logger.withTag("<Name>")`); error paths call
`logger.e(throwable) { "breadcrumb" }`. The global logger is bootstrapped once at startup —
`SoulariumApplication.onCreate` on Android, `MainViewController` on iOS — by
`LoggingBindings.Accessors.configureLogging()`, which sets the global minimum severity
(`logMinSeverity`, default `Severity.Error` — so only `Error`/`Assert` are emitted) and
installs the Metro-assembled `Set<LogWriter>` onto `Logger`. Writers come from
multibindings: `CrashlyticsLogWriter`
(`org.cru.soularium.firebase`, `@ContributesIntoSet`) forwards messages + non-fatals to
Firebase Crashlytics through the GitLive `firebase-crashlytics` KMP SDK, and the platform
console writer is contributed per-target (`AndroidLoggingBindings` — logcat, debug builds
only; `IosLoggingBindings` — NSLog). `CrashlyticsLogWriter` is inert (its Firebase calls
are wrapped defensively) until the `google-services.json` / `GoogleService-Info.plist`
config files land. Tests exercise presenters without configuring the logger, so log calls
hit only the default platform writer.

### Platform abstraction — expect/actual

KMP platform seams use `expect`/`actual`: `PlatformBindings`,
`PlatformBackHandler` (Android → `BackHandler`; iOS → no-op), and — in `:module:db` —
`SoulariumDatabaseConstructor` (its `actual`s are KSP-generated per platform). The Room
database **builder** is *not* `expect`/`actual`: it's a platform Metro `@Provides` in
`Android`/`Ios RoomBindings`. The device-state `DataStore` is likewise not `expect`/`actual`
— the common `preferenceDataStoreAt(producePath)` helper builds it and each platform's
`@Provides providesDeviceStateDataStore` supplies the path. Every `expect` declaration needs
an `actual` for **both** `androidMain` and `iosMain`, with matching signatures. `commonMain`
must contain no Android- or iOS-specific imports.

## Testing

- Unit tests (domain, data, presenter) live in `commonTest`; Paparazzi screenshot tests
  live in `androidHostTest`. There is no on-device Android instrumentation — everything
  runs host-side. Presenters are exercised via Circuit's `circuit-test` library
  (`FakeNavigator`, `presenter.test { awaitItem().eventSink(...) }`). Presenter tests are
  annotated `@RunOnAndroidWith(AndroidJUnit4::class)` so the Android-host variant runs
  them under Robolectric — required because the Compose Runtime's Android artifact
  touches `android.util.Log` from its error path. The iOS-simulator variant runs the
  same tests unannotated. Pure domain code (no Compose) has no such requirement.
- **Tests live in each module's `commonTest`**. `soularium-kmp.module-conventions` wires
  `kotlin.test` (+ the multiplatform `@RunOnAndroidWith` runner) into every `commonTest`
  via the `test-framework` catalog bundle, and Robolectric + androidx-test into every
  `androidHostTest` via the `android-test-framework` bundle.
- **Repository / Room tests** use an abstract-contract pattern: a persistence-agnostic
  contract test (e.g. `db.repository.SessionRepositoryTest`, asserting against an
  `abstract val repository`) plus a thin Room subclass (`SessionRoomRepositoryTest`,
  `@RunOnAndroidWith(AndroidJUnit4::class)`) that wires `db.sessionRepository` in. It runs
  on **both** Android host (Robolectric) and iOS via an `expect fun
  buildInMemorySoulariumDatabase()` with android/ios actuals (Android `ApplicationProvider`,
  iOS `BundledSQLiteDriver`); `module/db/src/androidHostTest/resources/robolectric.properties`
  pins the Robolectric SDK.
- **Compose UI interaction tests** live in `commonTest` and use `runComposeUiTest` (the
  `androidx.compose.ui.test.v2` API, from the `compose-ui-test` catalog entry) with
  `@RunOnAndroidWith(AndroidJUnit4::class)`. They render a composable, drive it
  (`onNode(...).performClick()`, `mainClock` for animation control) and assert. See
  `HomeMenuOverlayTest`.
- Test doubles: reusable fakes live in a sibling `test-fixtures` module (modeled after
  mpdx-kmp) — `:module:db:test-fixtures` provides `FakeSessionRepository`, a full
  in-memory `SessionRepository` with seeding, interaction recording, and fault
  injection, and `:module:game:test-fixtures` provides `FakeGameEngine`, a scripted
  `GameEngine` for isolated presenter tests. Single-use doubles (e.g. `RecordingAnalytics`
  in `GameEngineFlowTest`) stay as plain private classes in the test sources.
- Coroutine tests use `runTest { }` with an injected `TestDispatcher` — never
  `runBlocking`. Flow tests use Turbine (`flow.test { awaitItem() }`).
- Test functions use backtick-quoted names. **Presenter tests** name each case by the
  thing under test, `<subject> - <name> - <behavior>`: `UiEvent - <Event> - <behavior>`
  for event handling and `UiState - <field> - <behavior>` for state derivation — e.g.
  `` `UiEvent - Back - pops the navigator` `` and
  `` `UiState - selectedLanguage - reflects stored language` ``. Other tests use a
  descriptive sentence, e.g. `` `solo session completes from start through summary` ``.
- The `GameEngine`'s game rules should have exhaustive coverage (driven through
  `dispatch` in `GameEngineTest`); Presenters should have behavior tests.
- **Paparazzi screenshot tests** (in `androidHostTest`) cover each `<Feature>Layout` by
  rendering its stateless composable with a hand-built `UiState`. They extend
  `BasePaparazziTest` and run a device × light/dark (`nightMode`) matrix. Because
  layoutlib and Robolectric can't share a JVM, they are excluded from
  `testAndroidHostTest` unless `-Ppaparazzi` is passed (`:shared:verifyPaparazzi`).
  Snapshot PNGs are tracked in Git LFS and recorded via the record-snapshots GitHub
  Actions workflow — not recorded locally.

## CI & Workflows

CI lives in `.github/workflows/`. Paparazzi snapshots are regenerated only by the manual
`record-snapshots.yml` workflow, which commits them back to the branch — never record them
locally. The Crowdin workflows need the `CROWDIN_PERSONAL_TOKEN` repository secret (inert
until set); the Crowdin project ID is hardcoded in `crowdin.yml`.

## Code Review

PRs may be reviewed by `/agent-review` (see `.claude/commands/agent-review.md`), a
multi-agent AI review with smart agent selection, debate, and consensus. Findings of
severity ≥ 7 (the "Important" floor) cannot be dismissed via `/dismiss`; only the PR
author may dismiss severity < 7 findings.

## Code Style

- ktlint (`android_studio` code style, 120-char lines, trailing commas) enforces
  formatting mechanically — run `./gradlew ktlintFormat` to auto-fix.
- New `CompositionLocal`s must be added to `compose_allowed_composition_locals` in a
  scoped `.editorconfig` (see `di/.editorconfig`, `settings/.editorconfig`); otherwise the
  `compose-rules` ktlint `compositionlocal-allowlist` rule fails the build.

## Key Conventions

- iOS `Info.plist` is generated (`GENERATE_INFOPLIST_FILE = YES`), with keys set via
  `INFOPLIST_KEY_*`. A physical `Info.plist` (e.g. to declare `CFBundleLocalizations`) must
  live at `iosApp/Info.plist` — **outside** the file-system-synchronized source group
  `iosApp/iosApp/` — and be referenced via `INFOPLIST_FILE`. A plist placed inside that
  folder is auto-added as a bundle resource and fails the build with "Multiple commands
  produce Info.plist".
- User-visible strings come from Compose Multiplatform resources
  (`stringResource(Res.string.*)`), never inline literals. Source strings live in
  `shared/src/commonMain/composeResources/values/strings.xml`.
- Firebase config files (`google-services.json`, `GoogleService-Info.plist`) and
  `local.properties` are gitignored — never commit them.
