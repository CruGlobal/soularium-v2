# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Soularium v2 is a Cru-internal mobile rebuild of the discontinued **Soularium** and
**MySoularium** apps, built with Kotlin Multiplatform (KMP) and Compose Multiplatform
(CMP) for Android and iOS from a single codebase. It is an **offline-first**
conversation tool: a facilitator and one or more participants move through 5 questions,
selecting from 50 bundled card images, and the app generates a shareable summary link.

There are no accounts, no auth, no cloud sync, and no GraphQL/network API — content is
bundled and persistence is local. See `docs/superpowers/` for the design spec, the
implementation plan, and `HANDOFF.md` (current state).

## Repository Layout

**The Gradle project lives at the repo root** — run all Gradle commands from there.

```
shared/        → :shared — KMP library (Android via com.android.kotlin.multiplatform.library
                          + iOS framework). Domain models + session state machine, Room +
                          DataStore persistence, Compose UI, Circuit Presenters, navigation, Koin
                          wiring, and Android/iOS actuals — all in one module.
androidApp/    → :androidApp — Pure Android application (com.android.application). Hosts
                              MainActivity + SoulariumApplication + AndroidManifest;
                              depends on :shared.
iosApp/        → Native iOS shell (SwiftUI) hosting the Compose framework.
gradle/libs.versions.toml → Version catalog (single source of dependency versions).
.github/workflows/ → build.yml, crowdin-upload.yml, crowdin-download.yml
docs/superpowers/  → design spec, implementation plan, HANDOFF.md
```

There is **no** `build-logic/`, no Gradle convention plugins, and no `feature/`/`module/`/`ui/`
modules. Each module's `build.gradle.kts` configures itself directly using
`libs.versions.toml` aliases. AGP 9 forbids mixing the Kotlin Multiplatform plugin with
`com.android.application` (or `com.android.library`) in the same Gradle subproject — that
is why `:shared` is a KMP library (via `com.android.kotlin.multiplatform.library`) and
`:androidApp` is a separate Android-only shell that depends on it.

## Build & Development Commands

All commands run from the repo root.

```bash
# Build
./gradlew :androidApp:assembleDebug                         # Android APK
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64       # iOS framework (no Xcode needed)

# Tests
./gradlew :shared:allTests                # all unit tests (Android host + iOS simulator)
./gradlew :shared:testAndroidHostTest     # fast Android-host JVM subset

# Lint & formatting
./gradlew ktlintCheck    # check Kotlin formatting (all modules)
./gradlew ktlintFormat   # auto-fix formatting
./gradlew lint           # Android lint (all modules)
```

**Java requirement**: JDK is pinned via `.tool-versions` (currently
`temurin-25.0.3+9.0.LTS`). Use the repo's Gradle wrapper (`./gradlew`, 9.x) —
not a system Gradle. Source/target bytecode is JVM 17.

## Technology Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin 2.4.0, Compose Multiplatform 1.11.1, Material3 |
| Build | Gradle (Kotlin DSL) 9.x, AGP 9.2.1, `libs.versions.toml` |
| DI | Koin 4.0.0 |
| Persistence | Room 2.8.4 (KMP, via KSP) + DataStore Preferences |
| UI architecture / navigation | Circuit 0.34.0 (Presenter + UI, saveable back stack) |
| Async | kotlinx.coroutines 1.11.0 + Flow |
| Images | Coil 3 |
| Serialization | kotlinx.serialization 1.11.0 |
| Testing | `kotlin.test` + Kotest assertions + Turbine + `kotlinx-coroutines-test` |
| Lint | ktlint via `org.jlleitschuh.gradle.ktlint`, `intellij_idea` code style |
| Logging / crash | Kermit 2.1.0 → Firebase Crashlytics via GitLive `firebase-crashlytics` (inert until config files land) |
| Analytics | Firebase Analytics (no-op until config files land) |
| i18n | Crowdin (en, es, fr, pl, zh-rCN) |

## Architecture: Hexagonal (single shared module)

```
:androidApp  (Android-only shell: MainActivity, SoulariumApplication, manifest)
     │  depends on
     ▼
:shared      (KMP library — Android + iOS targets)
              ├── org.cru.soularium.domain — pure models, ports, session state machine
              ├── org.cru.soularium.data   — Room + DataStore + repository impls
              └── org.cru.soularium.ui     — Compose UI, Circuit Presenters, navigation, Koin
```

`:androidApp` depends on `:shared`; `:shared` depends on nothing else in this repo.
Package roots: `org.cru.soularium.app` (androidApp), `org.cru.soularium` (shared, with
sub-packages `domain`, `data`, `ui`, `di`, `platform`, `analytics`).

Layering is enforced by package convention: code in `org.cru.soularium.domain`
must not import from `data`, `ui`, or platform packages, and `org.cru.soularium.data`
must not import from `ui`.

### Domain layer (`org.cru.soularium.domain`)

- **Ports** (`domain/ports/`): interfaces the rest of the app depends on —
  `ContentRepository`, `SessionRepository`, `DeviceStateRepository`, `AnalyticsTracker`,
  `Sharer`. The interfaces live here; implementations live in
  `org.cru.soularium.data` or in the platform Koin modules. (Crash/error reporting is not
  a port — code logs through the global Kermit `Logger`; see "Logging & crash reporting".)
- **Session state machine** (`domain/session/`): `SessionState` (sealed,
  `@Serializable`), `SessionEvent` (sealed), a **pure** `fun transition(state, event,
  ctx): TransitionResult`, and `Effect` (sealed). `transition()` performs no I/O — side
  effects are *returned as data* (`Effect`) for the Presenter to execute. Keep it pure
  and exhaustively tested.
- **Models**: `Session`, `Conversation`, `CardPick`, `ContactInfo`, etc. — all
  `@Serializable`. IDs (`SessionId`, `ConversationId`, `CardPickId`) are
  `@Serializable @JvmInline value class` wrappers over UUID strings.
- **Errors**: `DomainError` sealed interface. There is no `Result<T>` wrapper —
  transition errors surface via `TransitionResult.error`.

Code under `org.cru.soularium.domain` must not reference Compose, Android, or iOS APIs.

### Data layer (`org.cru.soularium.data`)

- `SoulariumDatabase` is `@Database(version = 1, exportSchema = true)` with
  `@ConstructedBy(SoulariumDatabaseConstructor::class)` and an
  `expect object SoulariumDatabaseConstructor : RoomDatabaseConstructor<SoulariumDatabase>`.
  Room codegen runs through KSP for `kspAndroid` + each `kspIos*` target.
- Entities (`SessionEntity`, `ConversationEntity`, `CardPickEntity`) use FK cascades and
  indices on FK columns. DAOs use `@Upsert`, `@Query`, and `Flow` return types.
- The database is opened through an `expect fun getDatabaseBuilder()` with Android/iOS
  actuals; `BundledSQLiteDriver` is the driver on both platforms.
- **`SessionState` is persisted as a JSON snapshot string** (`state_snapshot_json`
  column). Renaming or removing a `@Serializable` field in the session-state hierarchy
  breaks already-persisted sessions — treat such changes as schema changes.
- Exported Room schema JSON lives in `shared/schemas/`. A `@Database` version
  bump must ship a matching schema JSON and migration.
- Device flags (intro seen, ToS agreed, locale) persist via DataStore Preferences, not
  Room.
- Repositories map Room entities ↔ domain models; the mapping must be total (no `!!` on
  optional columns).

### UI layer (`org.cru.soularium.ui`)

- **Navigation**: `NavGraph.kt` builds a Circuit saveable back stack rooted at a start
  `Screen` and renders the active screen via `NavigableCircuitContent`. Screen
  destinations are `@Parcelize` `data object`/`data class` types in `ui/nav/Screens.kt`.
  Presenters and Layouts are wired to their Screen by `@CircuitInject(SomeScreen::class,
  AppScope::class)` — Metro generates the matching `Presenter.Factory` /
  `Ui.Factory` at compile time (enabled by `metro { enableCircuitCodegen.set(true) }` in
  `:shared`'s build script) and contributes them to multibindings consumed by
  `CircuitBindings.providesCircuit`. There is no hand-written switch table.
  Cross-screen navigation goes through `Navigator.goTo(SomeScreen(...))` from inside a
  Presenter.
- **Presenters** implement Circuit's `Presenter<UiState>`. Each defines a nested
  `data class UiState(... val eventSink: (UiEvent) -> Unit) : CircuitUiState` and a
  `sealed interface UiEvent : CircuitUiEvent`. The `@Composable present()` body uses
  `remember { mutableStateOf(...) }` + `LaunchedEffect`/`produceState` to derive state
  from repositories (collected via `collectAsState()`); user intent flows in through
  `state.eventSink(...)`. Cross-screen navigation is `navigator.goTo(SomeScreen(...))`;
  back is `navigator.pop()`. Each Presenter lives in its own file named
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
  root. Light theme only (dark mode is a deliberate non-goal for v2). See
  `.claude/rules/design_system_rules.md`.

### Dependency Injection — Metro

- The graph is `SoulariumAppGraph` (in `shared/.../di/SoulariumAppGraph.kt`), a
  `@DependencyGraph(AppScope::class)` interface created via
  `createSoulariumAppGraph(platformBindings)`. The Android shell builds it once in
  `SoulariumApplication`; iOS builds it in `MainViewController.kt`.
- App-wide bindings live in `@BindingContainer @ContributesTo(AppScope::class)`
  interfaces (`DataBindings` for the database/DAOs/`DeviceStateRepository`,
  `CircuitBindings` for the assembled `Circuit`, `LoggingBindings` for the Kermit
  `LogWriter` multibinding). The no-op `AnalyticsTracker` lives in
  `NoOpAnalyticsTracker.kt`. Repository implementations (`SessionRepositoryImpl`,
  `ContentRepositoryImpl`) are `@Inject @ContributesBinding(AppScope::class)` so they're
  picked up automatically. Add new app-wide types by giving the implementation
  `@Inject` + `@ContributesBinding(AppScope::class)`, or by adding a `@Provides` to one
  of the binding containers.
- `PlatformBindings` is `expect class PlatformBindings` with Android/iOS actuals.
  The Android actual exposes the `Context` and pulls in `AndroidSharer`; the iOS actual
  pulls in `IosSharer`. Both `Sharer` impls are `@Inject @ContributesBinding(AppScope::class)`.
- **Adding a screen**: add a `Screen` to `ui/nav/Screens.kt`, then create
  `<Feature>Presenter.kt` and `<Feature>Layout.kt` annotated with `@CircuitInject(...)`
  (see above). Metro generates the matching `Presenter.Factory` + `Ui.Factory` and
  contributes them to the multibindings consumed by `CircuitBindings.providesCircuit`
  — no factory registration is required.

### Logging & crash reporting

There is no `CrashReporter` port. Code logs through the **global Kermit `Logger`** (each
file keeps a `private val logger = Logger.withTag("<Name>")`); error paths call
`logger.e(throwable) { "breadcrumb" }`. The global logger is bootstrapped once at startup —
`SoulariumApplication.onCreate` on Android, `MainViewController` on iOS — by
`LoggingBindings.Accessors.configureLogging()`, which sets the global minimum severity
(`logMinSeverity`, default `Severity.Error` — so only `Error`/`Assert` are emitted) and
installs the Metro-assembled `Set<LogWriter>` onto `Logger`. Writers come from
multibindings: `CrashlyticsLogWriter`
(`org.cru.soularium.logging`, `@ContributesIntoSet`) forwards messages + non-fatals to
Firebase Crashlytics through the GitLive `firebase-crashlytics` KMP SDK, and the platform
console writer is contributed per-target (`AndroidLoggingBindings` — logcat, debug builds
only; `IosLoggingBindings` — NSLog). `CrashlyticsLogWriter` is inert (its Firebase calls
are wrapped defensively) until the `google-services.json` / `GoogleService-Info.plist`
config files land. Tests exercise presenters without configuring the logger, so log calls
hit only the default platform writer.

### Platform abstraction — expect/actual

KMP platform seams use `expect`/`actual`: `PlatformBindings`, `Sharer`
(`AndroidSharer` → `Intent.ACTION_SEND`; `IosSharer` → `UIActivityViewController`),
`PlatformBackHandler` (Android → `BackHandler`; iOS → no-op), `getDatabaseBuilder()`,
`createDeviceStateDataStore()`, `SoulariumDatabaseConstructor`. Every `expect`
declaration needs an `actual` for **both** `androidMain` and `iosMain`, with matching
signatures. `commonMain` must contain no Android- or iOS-specific imports.

## Testing

- Frameworks: `kotlin.test` (`@Test`, `@BeforeTest`, `@AfterTest`), Kotest assertions
  (`kotest-assertions-core`), Turbine for `Flow` assertions, `kotlinx-coroutines-test`
  (`runTest`, `TestDispatcher`, `advanceUntilIdle`).
- All tests live in `commonTest`. There is no Android instrumentation and no Compose-UI
  instrumented tests. Presenters are exercised via Circuit's `circuit-test` library
  (`FakeNavigator`, `presenter.test { awaitItem().eventSink(...) }`). Presenter tests are
  annotated `@RunOnAndroidWith(AndroidJUnit4::class)` so the Android-host variant runs
  them under Robolectric — required because the Compose Runtime's Android artifact
  touches `android.util.Log` from its error path. The iOS-simulator variant runs the
  same tests unannotated. Pure domain code (no Compose) has no such requirement.
- Test doubles are plain in-memory classes defined in the test sources (e.g.
  `InMemorySessionRepository`, `FakeSessionRepository`, `RecordingSharer`). There are no
  `test-fixtures` modules.
- Coroutine tests use `runTest { }` with an injected `TestDispatcher` — never
  `runBlocking`. Flow tests use Turbine (`flow.test { awaitItem() }`).
- Test functions use backtick-quoted descriptive names, e.g.
  `` `solo session completes from start through summary` ``.
- The pure session state machine (`transition()`) and pure utilities (e.g. share-URL
  generation) should have exhaustive tests; Presenters should have behavior tests.

## CI & Workflows

- `build.yml` — build + test on every PR and push to `main`/`feature/*`/`release/*`.
  Runs the Android APK build (ubuntu-latest), the iOS framework link
  (`linkDebugFrameworkIosSimulatorArm64`, macos-26), `ktlintCheck`, Android `lint`,
  Android host tests (`:shared:testAndroidHostTest`), and iOS simulator tests
  (`:shared:iosSimulatorArm64Test`). JDK is read from `.tool-versions`. No secrets
  required.
- `crowdin-upload.yml` — pushes source strings to Crowdin on every push to `main`.
- `crowdin-download.yml` — weekly pull of translations from Crowdin, opens a PR.
  Both need the `CROWDIN_PERSONAL_TOKEN` repository secret (inert until set). The
  Crowdin project ID is hardcoded in `crowdin.yml`.

## Code Review

PRs may be reviewed by `/agent-review` (see `.claude/commands/agent-review.md`), a
multi-agent AI review with smart agent selection, debate, and consensus. Findings of
severity ≥ 7 (the "Important" floor) cannot be dismissed via `/dismiss`; only the PR
author may dismiss severity < 7 findings.

## Code Style

- Max line length: 120 characters.
- ktlint with the `intellij_idea` code style (set in `.editorconfig`).
- `@Composable` functions are exempt from function-naming rules; other functions are
  camelCase.
- Trailing commas are used and encouraged.
- ktlint must not lint generated sources — the root `build.gradle.kts` already excludes
  anything under a `build/` directory.

## Key Conventions

- Package structure: `org.cru.soularium.<area>` (e.g. `org.cru.soularium.ui.conversation`,
  `org.cru.soularium.domain.session`, `org.cru.soularium.data.db`). The Compose-resources
  `Res` accessor is generated at `org.cru.soularium.generated.resources` (configured via
  `compose.resources.packageOfResClass` in `:shared`).
- Android: `minSdk 24`, `compileSdk`/`targetSdk 36`, JVM target 17, application id
  `org.cru.soularium` (debug builds add a `.dev` suffix). The application id and build
  types live in `:androidApp` — `:shared` is a KMP library with no application id.
- iOS: bundle id `org.cru.soularium`; the Compose framework is embedded via an Xcode
  run-script phase.
- User-visible strings come from Compose Multiplatform resources
  (`stringResource(Res.string.*)`), never inline literals. Source strings live in
  `shared/src/commonMain/composeResources/values/strings.xml`.
- Firebase config files (`google-services.json` in `androidApp/`,
  `GoogleService-Info.plist` in `iosApp/`) are committed to the repo — they carry the
  `soularium-985bf` project's client keys, which are not secrets (they ship inside the
  distributed app and are guarded by Firebase security rules / App Check). `local.properties`
  and signing keystores (`*.jks`, `*.keystore`) remain gitignored — never commit those.
