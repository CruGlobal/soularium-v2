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

## Repository Layout

**The Gradle project lives at the repo root** — run all Gradle commands from there.

```
shared/        → :shared — KMP library (Android via com.android.kotlin.multiplatform.library
                          + iOS framework). Session state machine, Compose UI, Circuit
                          Presenters, navigation, Metro DI wiring, DeviceState (DataStore)
                          persistence, and Android/iOS actuals. Depends on :module:db and
                          :module:model.
module/model/  → :module:model — KMP library (Android + iOS). All @Serializable domain
                              models (org.cru.soularium.model): Session, Conversation,
                              CardPick, ContactInfo, and the persisted state machine state
                              (model.game.SessionState). Depends on nothing else in-repo.
module/db/     → :module:db — KMP library (Android + iOS). The Room persistence layer
                              (org.cru.soularium.db): the SessionRepository contract, the
                              Room database/DAOs/entities, the Room-backed repository, and
                              its Metro bindings. Depends on :module:model.
androidApp/    → :androidApp — Pure Android application (com.android.application). Hosts
                              MainActivity + SoulariumApplication + AndroidManifest;
                              depends on :shared.
iosApp/        → Native iOS shell (SwiftUI) hosting the Compose framework.
build-logic/   → Composite build with the convention plugins (soularium-kmp.module-conventions,
                              serialization-conventions, metro-conventions, plus ktlint/kover/paparazzi).
module/db/schemas/ → Exported Room schema JSON (one per @Database version).
gradle/libs.versions.toml → Version catalog (single source of dependency versions).
.github/workflows/ → build.yml, git-lfs-validation.yml, record-snapshots.yml,
                     crowdin-upload.yml, crowdin-download.yml
```

The app modules are `:shared`, `:module:model`, `:module:db`, and `:androidApp`. Cross-module
build logic lives in a `build-logic/` composite build. The three KMP library modules
(`:shared`, `:module:model`, `:module:db`) share their target/Android/iOS setup through the
`soularium-kmp.module-conventions` precompiled plugin (applies `kotlin("multiplatform")` +
`com.android.kotlin.multiplatform.library`, sets `compileSdk`/`minSdk`/JVM 17, `withHostTest`,
and the `iosArm64`/`iosSimulatorArm64` targets; applies `ktlint-conventions` + `kover-conventions`;
and wires the `test-framework` bundle into `commonTest` and the `android-test-framework` bundle
into `androidHostTest`). Two more composable conventions layer on top: `serialization-conventions`
(kotlin serialization plugin + `api(kotlinx-serialization-core)`) and `metro-conventions` (the
Metro plugin with `generateContributionProviders`, so `@ContributesTo`/`@ContributesBinding`
work across module boundaries). The remaining conventions are `ktlint-conventions`,
`kover-conventions`, and `paparazzi-conventions` (shared `Project.kt` helper). A module lists
`soularium-kmp.module-conventions` explicitly even when it also applies metro/serialization
conventions, so its KMP-library nature is obvious. Each module's `build.gradle.kts` still
declares its own `namespace`, extra plugins, and dependencies using `libs.versions.toml`
aliases, and inter-module dependencies use the type-safe project accessors
(`projects.module.model`, `projects.module.db`; `TYPESAFE_PROJECT_ACCESSORS` is enabled in
`settings.gradle.kts`). `:shared`, `:module:model`, and `:module:db` are KMP libraries (via
`com.android.kotlin.multiplatform.library`) and `:androidApp` is a separate Android-only
shell that depends on `:shared`.

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

Exact versions are pinned in `gradle/libs.versions.toml` — the single source of truth;
this table names the choices, not their versions.

| Concern | Choice |
|---|---|
| Language / UI | Kotlin, Compose Multiplatform, Material3 |
| Build | Gradle (Kotlin DSL), AGP, `libs.versions.toml` |
| DI | Metro (compile-time DI) |
| Persistence | Room (KMP, via KSP) + DataStore Preferences |
| UI architecture / navigation | Circuit (Presenter + UI, saveable back stack) |
| Async | kotlinx.coroutines + Flow |
| Images | Coil 3 |
| Serialization | kotlinx.serialization |
| Testing | `kotlin.test` + Kotest assertions + Turbine + `kotlinx-coroutines-test` |
| Lint | ktlint via `org.jlleitschuh.gradle.ktlint`, `android_studio` code style |
| Crash / analytics | Firebase Crashlytics + Analytics (no-op until config files land) |
| i18n | Crowdin (en, es, fr, pl, zh-rCN) |

## Architecture: Hexagonal (extracted model + db modules)

```
:androidApp  (Android-only shell: MainActivity, SoulariumApplication, manifest)
     │  depends on
     ▼
:shared      (KMP library — Android + iOS targets)
     │        ├── org.cru.soularium.domain — ports, pure session state machine, content,
     │        │                              DomainError, settings/share
     │        ├── org.cru.soularium.data   — DeviceState (DataStore) + ContentRepositoryImpl
     │        └── org.cru.soularium.ui      — Compose UI, Circuit Presenters, navigation, Metro DI
     │  depends on
     ├──────────────► :module:db (KMP library)
     │                └── org.cru.soularium.db — SessionRepository contract + Room persistence
     │                                           (database, DAOs, entities, RoomBindings, the
     │                                           Room-backed repository).  Depends on :module:model.
     ▼
:module:model (KMP library — Android + iOS targets)
              └── org.cru.soularium.model — @Serializable domain models (Session, Conversation,
                                            CardPick, ContactInfo) and model.game.SessionState
```

`:androidApp` → `:shared` → `:module:db` → `:module:model`, and `:shared` also depends on
`:module:model` directly; `:module:model` depends on nothing else in this repo. Package roots:
`org.cru.soularium.app` (androidApp), `org.cru.soularium.model` (module:model),
`org.cru.soularium.db` (module:db), `org.cru.soularium` (shared, with sub-packages
`domain`, `data`, `ui`, `di`, `platform`, `analytics`).

Layering is enforced by package convention: code in `org.cru.soularium.domain`
must not import from `data`, `ui`, or platform packages, and `org.cru.soularium.data`
must not import from `ui`.

### Domain layer (`org.cru.soularium.domain`)

- **Ports** (`domain/ports/`): interfaces the rest of the app depends on —
  `ContentRepository`, `DeviceStateRepository`, `AnalyticsTracker`, `CrashReporter`,
  `Sharer`. The interfaces live here; implementations live in `org.cru.soularium.data`
  or in the platform Metro binding containers (`PlatformBindings`). (`SessionRepository`
  is **not** here — it moved to `:module:db`, package `org.cru.soularium.db.repository`.)
- **Session state machine** (`domain/session/`): `SessionEvent` (sealed), a **pure**
  `fun transition(state, event, ctx): TransitionResult`, `Effect` (sealed), and
  `SessionContext`. The state itself — `SessionState` (sealed, `@Serializable`, with the
  nested `SessionState.InQuestion.QuestionState` enum) — lives in `:module:model`
  (`org.cru.soularium.model.game`), so `transition()` operates over it. `transition()`
  performs no I/O — side effects are *returned as data* (`Effect`) for the Presenter to
  execute. Keep it pure and exhaustively tested.
- **Models** live in `:module:model` (see below), not here.
- **Errors**: `DomainError` sealed interface. There is no `Result<T>` wrapper —
  transition errors surface via `TransitionResult.error`.

Domain code stays independent of the `data` and `ui` layers, but it **may** use platform
APIs — a domain port can have Android/iOS `actual` implementations that use them (e.g.
`domain.settings.AndroidLanguageRepository` uses `Context`). Domain avoids Compose UI, with
one exception: lightweight multiplatform value types such as Compose's `Locale`
(`androidx.compose.ui.text.intl.Locale`), treated as a data model.

### Model layer (`:module:model`, `org.cru.soularium.model`)

- Holds every `@Serializable` domain model, dependency-free (only serialization + stdlib):
  `Session`, `Conversation`, `CardPick`, `ContactInfo`, and the persisted state machine
  state `model.game.SessionState`.
- **IDs are nested value classes**, one per owning model: `Session.Id`, `Conversation.Id`,
  `CardPick.Id` — each a `@Serializable @JvmInline value class` over a UUID string
  (serializes as a bare JSON string). `Session` also nests `Session.Kind`, and
  `SessionState.InQuestion` nests the `QuestionState` enum.
- Applies `serialization-conventions` (so `@Serializable`/`@SerialName` are available).
  `@SerialName` on `ContactInfo` and `SessionState` variants pins the JSON wire format —
  do not change an existing `@SerialName`; it would orphan persisted sessions.

### Data layer (`org.cru.soularium.data`, in `:shared`)

- What remains in `:shared` is non-Room: the DeviceState `DataStore` code
  (`data/devicestate/`) and `ContentRepositoryImpl` (`data/repository/`, the bundled
  content). The Room database and the `SessionRepository` implementation moved to
  `:module:db`.
- Device flags (intro seen, ToS agreed) persist via DataStore Preferences, not Room. The
  app language is not stored here — it is the platform per-app language setting, read
  through `LanguageRepository`.
- Repositories map persistence rows ↔ domain models; the mapping must be total (no `!!`
  on optional columns).

### Persistence — Room (`:module:db`, `org.cru.soularium.db`)

- `db.repository.SessionRepository` is the persistence **contract** (the interface
  `:shared` depends on). `db.room` holds the Room implementation.
- `SoulariumDatabase` (`db.room`) is `@Database(version = 1)` with
  `@ConstructedBy(SoulariumDatabaseConstructor::class)` and an
  `expect object SoulariumDatabaseConstructor : RoomDatabaseConstructor<SoulariumDatabase>`.
  Its DAO/repository accessors are `internal abstract val`s. Room codegen runs through KSP
  for `kspAndroid` + each `kspIos*` target.
- Entities (`db.room.entities`) use FK cascades and indices on FK columns; DAOs
  (`db.room.dao`) use `@Upsert`, `@Query`, and `Flow` return types. **Foreign-key
  enforcement is on by default** with Room 2.8's drivers — there is no manual
  `PRAGMA foreign_keys = ON`.
- `SessionRoomRepository` is a Room `@Dao internal abstract class(db: SoulariumDatabase)`
  implementing `SessionRepository`: it reads its DAOs off the database and does the
  entity ↔ model mapping. `RoomBindings` exposes it to the graph as `SessionRepository`
  (see DI below).
- The database builder is provided per platform by `Android`/`Ios RoomBindings`
  (`AndroidSQLiteDriver` on Android, `BundledSQLiteDriver` on iOS) — not via an
  `expect`/`actual` function.
- **`SessionState` is persisted as a JSON snapshot string** (`state_snapshot_json`
  column). Renaming or removing a `@Serializable` field in the session-state hierarchy
  breaks already-persisted sessions — treat such changes as schema changes.
- Exported Room schema JSON lives in `module/db/schemas/`. A `@Database` version bump
  must ship a matching schema JSON and migration.

### UI layer (`org.cru.soularium.ui`)

- **Navigation**: `App.kt` builds a Circuit saveable back stack rooted at a start
  `Screen` and renders the active screen via `NavigableCircuitContent`. Screen
  destinations are `@Parcelize` `data object`/`data class` types. Most live together in
  `ui/nav/Screens.kt`, but a self-contained feature package may instead co-locate its own
  `Screen` next to its Presenter/Layout (e.g. `ui/terms/TermsScreen.kt`).
  Presenters and Layouts are wired to their Screen by `@CircuitInject(SomeScreen::class,
  AppScope::class)` — Metro generates the matching `Presenter.Factory` /
  `Ui.Factory` at compile time (enabled by `metro { enableCircuitCodegen.set(true) }` in
  `:shared`'s build script) and contributes them to multibindings consumed by
  `CircuitBindings.providesCircuit`. There is no hand-written switch table.
  Cross-screen navigation goes through `Navigator.goTo(SomeScreen(...))` from inside a
  Presenter.
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
  root. Provides light and dark Material3 color schemes, selected automatically via
  `isSystemInDarkTheme()`. See `.claude/rules/design_system_rules.md`.

### Dependency Injection — Metro

- The graph is `SoulariumAppGraph` (in `shared/.../di/SoulariumAppGraph.kt`), a
  `@DependencyGraph(AppScope::class)` interface created via
  `createSoulariumAppGraph(platformBindings)`. The Android shell builds it once in
  `SoulariumApplication`; iOS builds it in `MainViewController.kt`.
- App-wide bindings live in `@BindingContainer @ContributesTo(AppScope::class)` containers,
  and **contributions merge across modules** — every Metro module applies
  `metro-conventions` (`generateContributionProviders`), so `:module:db`'s bindings land in
  the `:shared` graph. In `:shared`: `DataBindings` (the `DeviceStateRepository`/DataStore),
  `CircuitBindings` (the assembled `Circuit`), `Placeholders` (no-op analytics/crash). In
  `:module:db`: `RoomBindings` builds the `SoulariumDatabase` and provides the
  `SessionRepository` (`= db.sessionRepository`), while `Android`/`Ios RoomBindings` provide
  the platform DB builder. `ContentRepositoryImpl` is `@Inject @ContributesBinding(AppScope::class)`
  so it's picked up automatically; the Room-backed `SessionRoomRepository` is a `@Dao` (Room
  constructs it) and is provided by `RoomBindings` rather than `@ContributesBinding`. Add new
  app-wide types by giving the implementation `@Inject` + `@ContributesBinding(AppScope::class)`,
  or by adding a `@Provides` to one of the binding containers.
- `PlatformBindings` is `expect class PlatformBindings` with Android/iOS actuals.
  The Android actual exposes the `Context` and pulls in `AndroidSharer`; the iOS actual
  pulls in `IosSharer`. Both `Sharer` impls are `@Inject @ContributesBinding(AppScope::class)`.
- **Adding a screen**: declare a `Screen` — either in `ui/nav/Screens.kt` or, for a
  self-contained feature package, co-located in that package (e.g.
  `ui/terms/TermsScreen.kt`) — then create
  `<Feature>Presenter.kt` and `<Feature>Layout.kt` annotated with `@CircuitInject(...)`
  (see above). Metro generates the matching `Presenter.Factory` + `Ui.Factory` and
  contributes them to the multibindings consumed by `CircuitBindings.providesCircuit`
  — no factory registration is required.

### Platform abstraction — expect/actual

KMP platform seams use `expect`/`actual`: `PlatformBindings`, `Sharer`
(`AndroidSharer` → `Intent.ACTION_SEND`; `IosSharer` → `UIActivityViewController`),
`PlatformBackHandler` (Android → `BackHandler`; iOS → no-op), and — in `:module:db` —
`SoulariumDatabaseConstructor` (its `actual`s are KSP-generated per platform). The Room
database **builder** is *not* `expect`/`actual`: it's a platform Metro `@Provides` in
`Android`/`Ios RoomBindings`. The device-state `DataStore` is likewise not `expect`/`actual`
— the common `preferenceDataStoreAt(producePath)` helper builds it and each platform's
`@Provides providesDeviceStateDataStore` supplies the path. Every `expect` declaration needs
an `actual` for **both** `androidMain` and `iosMain`, with matching signatures. `commonMain`
must contain no Android- or iOS-specific imports.

## Testing

- Frameworks: `kotlin.test` (`@Test`, `@BeforeTest`, `@AfterTest`), Kotest assertions
  (`kotest-assertions-core`), Turbine for `Flow` assertions, `kotlinx-coroutines-test`
  (`runTest`, `TestDispatcher`, `advanceUntilIdle`).
- Unit tests (domain, data, presenter) live in `commonTest`; Paparazzi screenshot tests
  live in `androidHostTest`. There is no on-device Android instrumentation — everything
  runs host-side. Presenters are exercised via Circuit's `circuit-test` library
  (`FakeNavigator`, `presenter.test { awaitItem().eventSink(...) }`). Presenter tests are
  annotated `@RunOnAndroidWith(AndroidJUnit4::class)` so the Android-host variant runs
  them under Robolectric — required because the Compose Runtime's Android artifact
  touches `android.util.Log` from its error path. The iOS-simulator variant runs the
  same tests unannotated. Pure domain code (no Compose) has no such requirement.
- **Tests live in each module's `commonTest`** — `:module:model`, `:module:db`, and
  `:shared`. `soularium-kmp.module-conventions` wires `kotlin.test` (+ the multiplatform
  `@RunOnAndroidWith` runner) into every `commonTest` via the `test-framework` catalog
  bundle, and Robolectric + androidx-test into every `androidHostTest` via the
  `android-test-framework` bundle.
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
- Test doubles are plain in-memory classes defined in the test sources (e.g.
  `InMemorySessionRepository`, `FakeSessionRepository`, `RecordingSharer`). There are no
  `test-fixtures` modules.
- Coroutine tests use `runTest { }` with an injected `TestDispatcher` — never
  `runBlocking`. Flow tests use Turbine (`flow.test { awaitItem() }`).
- Test functions use backtick-quoted names. **Presenter tests** name each case by the
  thing under test, `<subject> - <name> - <behavior>`: `UiEvent - <Event> - <behavior>`
  for event handling and `UiState - <field> - <behavior>` for state derivation — e.g.
  `` `UiEvent - Back - pops the navigator` `` and
  `` `UiState - selectedLanguage - reflects stored language` ``. Other tests use a
  descriptive sentence, e.g. `` `solo session completes from start through summary` ``.
- The pure session state machine (`transition()`) and pure utilities (e.g. share-URL
  generation) should have exhaustive tests; Presenters should have behavior tests.
- **Paparazzi screenshot tests** (in `androidHostTest`) cover each `<Feature>Layout` by
  rendering its stateless composable with a hand-built `UiState`. They extend
  `BasePaparazziTest` and run a device × light/dark (`nightMode`) matrix. Because
  layoutlib and Robolectric can't share a JVM, they are excluded from
  `testAndroidHostTest` unless `-Ppaparazzi` is passed (`:shared:verifyPaparazzi`).
  Snapshot PNGs are tracked in Git LFS and recorded via the record-snapshots GitHub
  Actions workflow — not recorded locally.

## CI & Workflows

- `build.yml` — build + test on every PR and push to `main`/`feature/*`/`release/*`.
  Runs the Android APK build (ubuntu-latest), the iOS framework link
  (`linkDebugFrameworkIosSimulatorArm64`, macos-26), `ktlintCheck` (which also lints the
  `build-logic` composite build via a root-task dependency),
  Android `lint`, Android host tests (`:shared:testAndroidHostTest`), Paparazzi
  screenshot verification (`:shared:verifyPaparazzi -Ppaparazzi`), and iOS simulator
  tests (`:shared:iosSimulatorArm64Test`). The host-test and Paparazzi jobs also produce
  Kover XML coverage that is uploaded to Codecov. JDK is read from `.tool-versions`. No
  secrets required (Codecov uses an org-level token).
- `git-lfs-validation.yml` — runs `git lfs fsck --pointers` on every PR and push to
  `main`/`feature/*`/`release/*` to catch Paparazzi snapshots committed without Git LFS.
- `record-snapshots.yml` — manual (`workflow_dispatch`) job that regenerates Paparazzi
  snapshots (`:shared:cleanRecordPaparazzi -Ppaparazzi`) and commits them back to the
  branch. This is how snapshots are recorded — never locally.
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
- ktlint with the `android_studio` code style (set in `.editorconfig`).
- `@Composable` functions are exempt from function-naming rules; other functions are
  camelCase.
- Trailing commas are used and encouraged.
- ktlint must not lint generated sources — the root `build.gradle.kts` already excludes
  anything under a `build/` directory.
- New `CompositionLocal`s must be added to `compose_allowed_composition_locals` in a
  scoped `.editorconfig` (see `di/.editorconfig`, `settings/.editorconfig`); otherwise the
  `compose-rules` ktlint `compositionlocal-allowlist` rule fails the build.

## Key Conventions

- Package structure: `org.cru.soularium.<area>` (e.g. `org.cru.soularium.ui.conversation`,
  `org.cru.soularium.domain.session`, `org.cru.soularium.model.game`,
  `org.cru.soularium.db.room`). The Compose-resources
  `Res` accessor is generated at `org.cru.soularium.generated.resources` (configured via
  `compose.resources.packageOfResClass` in `:shared`).
- Android: `minSdk 24`, `compileSdk`/`targetSdk 37`, JVM target 17, application id
  `org.cru.soularium` (debug builds add a `.dev` suffix). The application id and build
  types live in `:androidApp` — `:shared` is a KMP library with no application id.
- iOS: bundle id `org.cru.soularium`; the Compose framework is embedded via an Xcode
  run-script phase.
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
