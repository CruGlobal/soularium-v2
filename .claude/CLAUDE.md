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

The repository root contains `.github/`, `docs/`, `crowdin.yml`, `README.md`, and the
`mobile/` directory. **The Gradle project lives in `mobile/`** — run all Gradle commands
from there.

```
mobile/
  domain/      → :domain  — pure KMP (jvm + iOS). No Android/Compose deps.
  data/        → :data    — KMP library (Android via com.android.kotlin.multiplatform.library
                            + iOS). Room, DataStore, repository impls.
  shared/      → :shared — KMP library (Android via com.android.kotlin.multiplatform.library
                              + iOS framework). Compose UI, ViewModels, navigation,
                              Koin wiring, Android-specific actuals.
  androidApp/  → :androidApp — Pure Android application (com.android.application). Hosts
                              MainActivity + SoulariumApplication + AndroidManifest;
                              depends on :shared.
  iosApp/      → Native iOS shell (SwiftUI) hosting the Compose framework.
  gradle/libs.versions.toml → Version catalog (single source of dependency versions).
.github/workflows/ → build.yml, crowdin-upload.yml, crowdin-download.yml, ai-review-auto-approve.yml
docs/superpowers/  → design spec, implementation plan, HANDOFF.md
```

There is **no** `build-logic/`, no Gradle convention plugins, and no `feature/`/`module/`/`ui/`
modules. Each module's `build.gradle.kts` configures itself directly using
`libs.versions.toml` aliases. AGP 9 forbids mixing the Kotlin Multiplatform plugin with
`com.android.application` (or `com.android.library`) in the same Gradle subproject — that
is why `:shared` is a KMP library and `:androidApp` is a separate Android-only shell.

## Build & Development Commands

All commands run from `mobile/`.

```bash
# Build
./gradlew :androidApp:assembleDebug                         # Android APK
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64   # iOS framework (no Xcode needed)

# Tests
./gradlew :domain:allTests :data:allTests :shared:allTests  # all unit tests
./gradlew :domain:jvmTest                                       # fast domain-only JVM subset (~2s)

# Lint & formatting
./gradlew ktlintCheck    # check Kotlin formatting (all modules)
./gradlew ktlintFormat   # auto-fix formatting
./gradlew lint           # Android lint (all modules)
```

**Java requirement**: Temurin JDK 17 (pinned as `temurin-17.0.19+10` in `.tool-versions`).
With asdf, export `JAVA_HOME` before any Gradle call:
`export JAVA_HOME=~/.asdf/installs/java/temurin-17.0.19+10`. Use the repo's Gradle wrapper
(`mobile/gradlew`, 8.10.2) — not a system Gradle.

## Technology Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin 2.4.0, Compose Multiplatform 1.11.1, Material3 |
| Build | Gradle (Kotlin DSL) 9.4.1, AGP 9.2.1, `libs.versions.toml` |
| DI | Koin 4.0.0 |
| Persistence | Room 2.8.4 (KMP, via KSP) + DataStore Preferences |
| Navigation | Navigation Compose (JetBrains KMP variant) |
| Async | kotlinx.coroutines 1.9.0 + Flow |
| Images | Coil 3 |
| Serialization | kotlinx.serialization 1.7.3 |
| Testing | `kotlin.test` + Kotest assertions + Turbine + `kotlinx-coroutines-test` |
| Lint | ktlint via `org.jlleitschuh.gradle.ktlint`, `intellij_idea` code style |
| Crash / analytics | Firebase Crashlytics + Analytics (no-op until config files land) |
| i18n | Crowdin (en, es, fr, pl, zh-rCN) |

> **KMP variant gotcha**: `androidx-navigation-compose` and
> `androidx-lifecycle-viewmodel-compose` must be the JetBrains multiplatform variants
> (`org.jetbrains.androidx.*`), not Google's AndroidX artifacts. The catalog is already
> correct — keep it that way.

## Architecture: 3-Layer Hexagonal

```
:androidApp  (Android-only shell: MainActivity, SoulariumApplication, manifest)
     │  depends on
     ▼
:shared  (Compose UI, ViewModels, navigation, Koin wiring) — KMP library
     │  depends on
     ▼
:domain  ◄── pure KMP: hexagonal "ports" + the pure session state machine
     ▲
     │  depends on
:data  (Room, DataStore, repository implementations) — KMP library
```

`:androidApp` depends on `:shared`; `:shared` depends on `:domain` and `:data`;
`:data` depends on `:domain`; `:domain` depends on nothing in this repo. Package roots:
`org.cru.soularium.app` (androidApp), `org.cru.soularium` (shared),
`org.cru.soularium.data`, `org.cru.soularium.domain`.

### `:domain` — pure KMP

- **Ports** (`domain/.../ports/`): interfaces the rest of the app depends on —
  `ContentRepository`, `SessionRepository`, `DeviceStateRepository`, `AnalyticsTracker`,
  `CrashReporter`, `Sharer`. The interfaces live here; implementations live in `:data`
  or in the platform Koin modules.
- **Session state machine** (`domain/.../session/`): `SessionState` (sealed,
  `@Serializable`), `SessionEvent` (sealed), a **pure** `fun transition(state, event,
  ctx): TransitionResult`, and `Effect` (sealed). `transition()` performs no I/O — side
  effects are *returned as data* (`Effect`) for the ViewModel to execute. Keep it pure
  and exhaustively tested.
- **Models**: `Session`, `Conversation`, `CardPick`, `ContactInfo`, etc. — all
  `@Serializable`. IDs (`SessionId`, `ConversationId`, `CardPickId`) are
  `@Serializable @JvmInline value class` wrappers over UUID strings.
- **Errors**: `DomainError` sealed interface. There is no `Result<T>` wrapper —
  transition errors surface via `TransitionResult.error`.

`:domain` must not reference Compose, Android, or iOS APIs.

### `:data` — Room + DataStore

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
- Exported Room schema JSON lives in `mobile/data/schemas/`. A `@Database` version bump
  must ship a matching schema JSON and migration.
- Device flags (intro seen, ToS agreed, locale) persist via DataStore Preferences, not
  Room.
- Repositories map Room entities ↔ domain models; the mapping must be total (no `!!` on
  optional columns).

### `:shared` — UI

- **Navigation**: `Routes` (an `object` of string route constants) + `NavGraph.kt`
  wiring a `NavHost`. Cross-screen navigation goes through `Routes`.
- **ViewModels** extend `androidx.lifecycle.ViewModel`. They expose UI state as
  `StateFlow` (private `MutableStateFlow` backing field, public `.asStateFlow()`) and
  receive user intent through public methods (e.g. `dispatch(event: SessionEvent)`).
  Side effects run in `viewModelScope`.
- **Screens** are public, stateless `@Composable` functions. Each takes its data as
  parameters plus `on*` callback lambdas, and `modifier: Modifier = Modifier` as the
  **last** parameter. Screens collect ViewModel state via `collectAsState()`; they hold
  no business logic. Private sub-composables within a screen file are `private`.
- **Theme**: `SoulariumTheme { }` (a thin Material3 wrapper) is applied once at the app
  root. Light theme only (dark mode is a deliberate non-goal for v2). See
  `.claude/rules/design_system_rules.md`.

### Dependency Injection — Koin

- `initKoin()` (in `shared/.../di/KoinInit.kt`) starts Koin with `appModule` +
  `platformModule`. It is idempotent (safe to call twice).
- `appModule` (commonMain) registers `single { }` for the database, DAOs, and
  repositories, and `viewModel { }` for ViewModels. ViewModels that need a runtime
  argument use `viewModel { (arg) -> ... }` and are resolved with
  `koinViewModel { parametersOf(arg) }`.
- `platformModule` is `expect val platformModule: Module`. The Android actual provides
  `AndroidSharer` (+ no-op analytics/crash); the iOS actual provides `IosSharer`.
- Add a new app-wide dependency to `appModule`; add a new platform-specific dependency
  to the matching `platformModule` actual.

### Platform abstraction — expect/actual

KMP platform seams use `expect`/`actual`: `platformModule`, `Sharer`
(`AndroidSharer` → `Intent.ACTION_SEND`; `IosSharer` → `UIActivityViewController`),
`PlatformBackHandler` (Android → `BackHandler`; iOS → no-op), `getDatabaseBuilder()`,
`createDeviceStateDataStore()`, `SoulariumDatabaseConstructor`. Every `expect`
declaration needs an `actual` for **both** `androidMain` and `iosMain`, with matching
signatures. `commonMain` must contain no Android- or iOS-specific imports.

## Testing

- Frameworks: `kotlin.test` (`@Test`, `@BeforeTest`, `@AfterTest`), Kotest assertions
  (`kotest-assertions-core`), Turbine for `Flow` assertions, `kotlinx-coroutines-test`
  (`runTest`, `TestDispatcher`, `advanceUntilIdle`).
- All tests live in `commonTest`. There is **no** Robolectric, no `@RunOnAndroidWith`,
  no Android instrumentation, and no Compose-UI instrumented tests. Compose/UI logic is
  verified through ViewModel and pure-function tests.
- Test doubles are plain in-memory classes defined in the test sources (e.g.
  `InMemorySessionRepository`, `FakeSessionRepository`, `RecordingSharer`). There are no
  `test-fixtures` modules.
- Coroutine tests use `runTest { }` with an injected `TestDispatcher` — never
  `runBlocking`. Flow tests use Turbine (`flow.test { awaitItem() }`).
- Test functions use backtick-quoted descriptive names, e.g.
  `` `solo session completes from start through summary` ``.
- The pure session state machine (`transition()`) and pure utilities (e.g. share-URL
  generation) should have exhaustive tests; ViewModels should have behavior tests.

## CI & Workflows

- `ci.yml` — build + test on every PR and push to `main` (macos-14, JDK 17). Runs
  `ktlintCheck`, all module tests, the Android APK build, and the iOS framework link.
  No secrets required.
- `crowdin-upload.yml` — pushes source strings to Crowdin on every push to `main`.
- `crowdin-download.yml` — weekly pull of translations from Crowdin, opens a PR.
  Both need the `CROWDIN_PERSONAL_TOKEN` repository secret (inert until set). The
  Crowdin project ID is hardcoded in `crowdin.yml`.
- `release.yml` — tag-triggered (`v*`); produces unsigned Android artifacts; iOS release
  is manual until signing/Fastlane is configured.
- `ai-review-auto-approve.yml` — see Code Review below.

## Code Review

PRs may be reviewed by `/agent-review` (see `.claude/commands/agent-review.md`), a
multi-agent AI review with smart agent selection, debate, and consensus.

When `/agent-review` produces a CLEAN or APPROVED_WITH_SUGGESTIONS verdict on a LOW or
MEDIUM-risk PR with **zero blockers and zero Important findings**, it dispatches
`.github/workflows/ai-review-auto-approve.yml`, which posts an APPROVE review as
`github-actions[bot]`. Branch protection still gates the merge on CI. Findings of
severity ≥ 7 (the "Important" floor) cannot be dismissed via `/dismiss`; only the PR
author may dismiss severity < 7 findings.

## Code Style

- Max line length: 120 characters.
- ktlint with the `intellij_idea` code style (set in `mobile/.editorconfig`).
- `@Composable` functions are exempt from function-naming rules; other functions are
  camelCase.
- Trailing commas are used and encouraged.
- ktlint must not lint generated sources — `mobile/build.gradle.kts` already excludes
  anything under a `build/` directory.

## Key Conventions

- Package structure: `org.cru.soularium.<area>` (e.g. `org.cru.soularium.ui.conversation`,
  `org.cru.soularium.domain.session`, `org.cru.soularium.data.db`).
- Android: `minSdk 24`, `compileSdk`/`targetSdk 36`, JVM target 17, application id
  `org.cru.soularium` (debug builds add a `.dev` suffix). The application id and build
  types live in `:androidApp` — `:shared` is a KMP library with no application id.
- iOS: bundle id `org.cru.soularium`; the Compose framework is embedded via an Xcode
  run-script phase.
- User-visible strings come from Compose Multiplatform resources
  (`stringResource(Res.string.*)`), never inline literals. Source strings live in
  `mobile/shared/src/commonMain/composeResources/values/strings.xml`.
- Firebase config files (`google-services.json`, `GoogleService-Info.plist`) and
  `local.properties` are gitignored — never commit them.
