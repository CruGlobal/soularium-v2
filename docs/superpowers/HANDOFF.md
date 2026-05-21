# Soularium v2 — Session Handoff

Last updated: 2026-05-21

## TL;DR for the next Claude

Read these in order:

1. `docs/superpowers/specs/2026-05-20-soularium-v2-design.md` — the spec (what we're building, decisions, contracts)
2. `docs/superpowers/plans/2026-05-20-soularium-v2-mobile.md` — the implementation plan (task-by-task)
3. This file — current state and what's next

## What's done (Phases 0–5)

### Phase 0 — Project bootstrap (Tasks 1–4 ✅, Task 5 partial)

- KMP project at `mobile/` with three modules: `:domain`, `:data`, `:composeApp`
- Gradle wrapper pinned at 8.10.2
- `mobile/composeApp/` produces a working Android APK rendering "Soularium v2 — bootstrap OK"
- iOS Xcode project at `mobile/iosApp/iosApp.xcodeproj` exists (Daniel set this up). **Bundle ID concern still open** — see below.

### Phase 1 — Domain types (Tasks 6–9) ✅

- `mobile/domain/src/commonMain/kotlin/org/cru/soularium/domain/`:
  - `Ids.kt` (`SessionId`, `ConversationId`, `CardPickId` — value classes, UUID-v4 generation)
  - `SessionKind.kt`, `ContactInfo.kt`, `Session.kt`, `Conversation.kt`, `CardPick.kt`, `DomainError.kt`
  - `content/` — `Question.kt`, `CardImage.kt`, `Questions.kt` (5-question metadata with selection rules)
- Tests passing in `commonTest/`

### Phase 2 — State machine (Tasks 10–13) ✅

- `domain/session/`:
  - `SessionState.kt` (sealed: NotStarted, AddingParticipants, InQuestion, Summary, CollectingContact, Concluded)
  - `SessionEvent.kt`, `SessionContext.kt`, `Effect.kt`, `TransitionResult.kt`
  - `Transition.kt` — the pure `transition(state, event, ctx)` function
- **22 transition tests, all green** in `TransitionTest.kt`

### Phase 3 — Pure utilities + ports (Tasks 14–15) ✅

- `domain/share/ShareUrl.kt` — `shareUrlFor(conversation, picks)` producing the exact format the discontinued web app expected (`?images=...&person=...`). 4 tests passing.
- `domain/ports/` — `SessionRepository`, `ContentRepository`, `AnalyticsTracker`, `CrashReporter`, `Sharer` interfaces.

### Phase 4–5 — Data layer (Tasks 16–19) ✅

- `data/db/entities/` — `SessionEntity`, `ConversationEntity`, `CardPickEntity`. Foreign keys + indexes.
- `data/db/` — `SessionDao`, `ConversationDao`, `CardPickDao`, `SoulariumDatabase` (Room v1)
- `data/db/DatabaseBuilder.kt` (expect) + `.android.kt` / `.ios.kt` actuals
- `data/repository/SessionRepositoryImpl.kt` — full impl backed by Room, serializing `SessionState` as JSON
- `data/repository/ContentRepositoryImpl.kt` — in-memory bundled content
- **Compiles cleanly for Android AND iOS** (with one harmless Beta warning about expect/actual classes)

### Phase 6 — Theme, resources, DI, ViewModel, navigation (Tasks 20–25) ✅

- Task 20: 50 card images (`card_NN.jpg`, 2208×1468 landscape) + thumbnails (`card_NN_thumb.png`) in `composeApp/src/commonMain/composeResources/drawable/`.
- Task 21: Open Sans TTFs + `ui/theme/` (`Color.kt` with a completed M3 light scheme, `Typography.kt`, `Theme.kt`).
- Task 22: full English `values/strings.xml`; empty `<resources/>` stubs for `values-es/fr/pl/zh-rCN/`.
- Task 23: Koin — `appModule` + `platformModule` (expect/actual), no-op Analytics/Crash/Sharer placeholders, `SoulariumApplication` (Android) / `initKoin()` (iOS).
- Task 24: `ConversationViewModel` (6 coroutines/Turbine tests).
- Task 25: `Routes` + `NavGraph` + `ConversationHost` (the single conversation destination, `AnimatedContent` over `SessionState`).

### Phase 7 — All 14 screens (Tasks 26–39) ✅

Built via parallel subagent waves with two-stage (spec + code-quality) review:
- Top-level (`ui/screens/`): Intro, Terms, Home (+ `MenuBottomSheet`), Past Conversations (+ `PastConversationsViewModel`), About, Resources, Cards & Questions, Settings.
- Conversation subscreens (`ui/conversation/`): AddParticipants, QuestionPrompt, InstructionPanel, Selection, Finalizing, Discussing, Summary, ContactCollection — all wired into `ConversationHost`.
- `ConversationViewModel` extended: `ensureStarted(kind)` bootstraps a new session, draft picks survive through Finalizing/Discussing, `loadSummaries()` + `shareSummary()` (via the `Sharer` port + `shareUrlFor`).
- Helpers added: `ui/content/CardImages.kt` (card id → drawable), `domain/Session.kt` `newSession()`, `domain/DateFormatting.kt`.

### Infrastructure note — ktlint

`ktlintCheck` had never passed (the bundled `ktlint_official` ruleset flagged ~1000 violations and crashed on rule-disable). Fixed in `mobile/.editorconfig` (switched to `intellij_idea` code style) + `mobile/build.gradle.kts` (excludes generated sources). `ktlintCheck` is now green for all three modules — keep it that way.

## What's next (Phases 8–11 — pick up here)

The plan (`docs/superpowers/plans/2026-05-20-soularium-v2-mobile.md`) has these in detail:

- **Task 40** — `Sharer` expect/actual: Android `Intent.ACTION_SEND`, iOS `UIActivityViewController`. Currently a `NoOpSharer` placeholder in `di/Placeholders.kt`; replace its Koin binding in `PlatformModule.android.kt` / `.ios.kt`.
- **Tasks 41–42** — Firebase analytics + Crashlytics. **BLOCKED on Cru:** needs `google-services.json` + `GoogleService-Info.plist` (gitignored, not in repo). Ship Android-side no-op stubs until the files land.
- **Task 43** — `BackHandler` in `ConversationHost` ("bookmark and exit" / "discard" dialog).
- **Tasks 44–45** — Crowdin `.yml` + GitHub Action. Needs a Cru Crowdin account decision.
- **Tasks 46–47** — CI workflow + release workflow stub.
- **Tasks 48–51** — Accessibility audit, E2E smoke tests, real-device testing, Firebase App Distribution.

### Known follow-ups / loose ends

- `NavGraph` starts at `HOME`; first-launch Intro→Terms routing waits on device-state (DataStore) persistence — not yet built. Settings locale + Terms agreement are in-memory only for the same reason.
- The Summary → contact-collection handoff is wired minimally; the `CollectContact` state-machine semantics around advancing/concluding deserve a closer look during E2E testing (Task 49).
- No `ConclusionScreen` — `Concluded` state auto-exits the conversation destination.
- Asset bundle is ~37 MB (50 full JPGs + 50 PNG thumbnails). The PNG thumbnails are oversized; converting them to JPG would save ~12 MB if binary size matters.

## Critical environment / setup notes

### Java + Gradle

- Daniel uses **asdf 0.18** for version management. `.tool-versions` at repo root pins `java temurin-17.0.19+10`.
- **`JAVA_HOME` is not automatically set by asdf shims for non-interactive shells.** Every Bash command that calls Gradle should prefix:
  ```
  export JAVA_HOME=~/.asdf/installs/java/temurin-17.0.19+10
  ```
- Gradle wrapper is at `mobile/gradlew`, version `8.10.2`. Don't run system `gradle` (currently 9.5.1) directly.

### Local SDK / secrets

- `mobile/local.properties` (gitignored) sets `sdk.dir=/Users/danielbisgrove/Library/Android/sdk`. Already set up.
- **Firebase config files are gitignored** (`google-services.json`, `GoogleService-Info.plist`). When Phase 8 (Firebase integration) lands, Daniel will need to drop the real files in. There's no working Firebase Analytics/Crashlytics output until then; the Android side of Task 41 should ship stubs that no-op until the file is present.

### Library variant gotcha

- For Compose Multiplatform, **navigation-compose** and **lifecycle-viewmodel-compose** must be the JetBrains KMP variants, not Google's androidx ones:
  ```toml
  androidx-lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
  androidx-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "androidx-navigation" }
  ```
  Google's `androidx.navigation:navigation-compose` is Android-only and won't resolve for iOS targets. (Already correct in `mobile/gradle/libs.versions.toml`.)

### iOS Xcode bundle ID — STILL OPEN

Daniel ran the Xcode wizard with **Product Name: `iosApp`** + **Organization Identifier: `org.cru.soularium`**, which produces bundle ID `org.cru.soularium.iosApp`.

The desired bundle ID is **`org.cru.soularium`** (matching the Android side and the discontinued iOS app, so App Store restoration maps cleanly).

To fix: open the project in Xcode → Project Navigator → iosApp target → **General** → change **Bundle Identifier** from `org.cru.soularium.iosApp` to `org.cru.soularium`. Then update any matching references in the Compose framework embed step.

Not load-bearing until TestFlight submission; can wait.

### iOS Compose framework embed — also still open

The iosApp project doesn't yet have the Run Script Build Phase that calls `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`. Plan Task 5, Step 4 covers this. Until it's wired in, you can build the Kotlin side via `./gradlew :composeApp:linkPodDebugFrameworkIosSimulatorArm64` but you can't run the iOS app from Xcode. Phase 7 screens can be developed Android-first; iOS verification comes when this is wired.

### Room expect/actual warnings

The Room KMP generated code emits two `expect`/`actual` Beta warnings (`SoulariumDatabaseConstructor`). Harmless; ignore until KT-61573 stabilizes. Don't try to suppress with `-Xexpect-actual-classes` unless you also want to opt the entire module into beta semantics.

## Test/build commands worth remembering

```bash
# JAVA_HOME setup (every shell)
export JAVA_HOME=~/.asdf/installs/java/temurin-17.0.19+10

# Domain JVM tests — the fast feedback loop, runs in ~2s
cd mobile && ./gradlew :domain:jvmTest

# Android APK build
cd mobile && ./gradlew :composeApp:assembleDebug
# → mobile/composeApp/build/outputs/apk/debug/composeApp-debug.apk

# iOS framework build (no Xcode needed)
cd mobile && ./gradlew :composeApp:linkPodDebugFrameworkIosSimulatorArm64

# All data targets compile-check
cd mobile && ./gradlew :data:compileDebugKotlinAndroid :data:compileKotlinIosSimulatorArm64
```

## How the layers fit together

```
:composeApp (KMP+CMP UI, ViewModels, navigation)
        |
        v
:domain (pure KMP) <— pure transitions, pure shareUrlFor, ports
        ^
        |
:data (KMP)    Room + repository implementations + DataStore (later)
```

`:composeApp` depends on both `:domain` (for state machine, types) and `:data` (for repository impls). `:data` depends on `:domain` (for entity/value types and port interfaces).

## Mental model for the next phase

The next big chunk is **UI**. Two facts to hold in mind:

1. **The conversation flow is one Compose Navigation destination.** Plan Task 25 establishes `ConversationHost` which internally renders subscreens from `SessionState`. Don't push subscreens onto the back stack — that's the trap the original apps fell into. The state machine + Compose `AnimatedContent(state)` is the model.

2. **All UI strings go through `Res.string.*`.** No string literals in composables. The Crowdin pipeline (Task 44/45) depends on this.

That's it. Pick up at Task 40 (Sharer expect/actual).
