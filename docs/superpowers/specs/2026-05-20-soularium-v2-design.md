# Soularium v2 — Design

**Status:** Draft for review
**Date:** 2026-05-20
**Author:** Daniel Bisgrove + Claude
**Scope:** Mobile app rebuild. Companion website (`mysoularium.com`) is a separate spec; the URL contract between them is defined here.

---

## 1. Goals, non-goals, scope

### Goal

Replace the discontinued iOS and Android Soularium apps with a single cross-platform rebuild — same product, modern execution — under Cru's existing accounts and bundle IDs (`org.cru.soularium`), so they can be republished to both stores. Rebuild `mysoularium.com` so share links work again (separate spec, separate cycle).

### Mobile app v1 must

- Reproduce the conversation flow: MySoularium (solo) and group (device-passing, up to 8 participants), 5 questions, image selection per question, "Life in Pictures" summary, optional contact collection.
- Persist conversations locally (offline-first), including past, bookmarked, and completed conversations.
- Ship with the 50 existing cards and 5 existing questions, verbatim, with the same selection rules (Q1–Q2: 3 images in 2 rounds; Q3–Q5: 1 image in 1 round).
- Restore SMS / email sharing via system intents using the same `?images=...&person=...` URL format.
- Light theme, Material 3, Soularium orange palette + Open Sans, accessibility built in (TalkBack/VoiceOver labels, large tap targets, sufficient contrast).
- Architecturally support i18n for the 6 iOS languages from day one (en, es, fr, pl, zh-Hans, plus Base), English shipping first, other locales filled in as translation work completes.
- Firebase Analytics + Crashlytics on the existing `soularium-985bf` project.
- Reproduce the secondary screens: Intro / Terms of Use, About, Resources, Images & Questions reference, Past Conversations (with completed + bookmarked tabs), Settings.

### Non-goals (v1)

- No accounts, auth, or cloud sync. Conversations stay on the device that created them.
- No backend API beyond Firebase and the static website.
- No AI features. Phase 2 candidate; architecture leaves a clean seam (`SuggestionService` interface) but no implementation.
- No real-time multi-device group mode. Device-passing only, as the original.
- No multi-deck content system. Data model is designed so this is a small future change.
- No dark mode (v1.1).
- No conversation-data migration from the old apps (no existing user data to migrate; the old apps never synced).
- No analytics dashboards or admin tooling beyond what Firebase Analytics gives out of the box.

### Out of scope for this spec

The `mysoularium.com` rebuild (Next.js + React + MUI + yarn + TS) is a separate spec. The contract between the two — the share URL format — is defined in §7 here and must be honored by both.

---

## 2. Stack and architecture

### Stack

- **Language / framework:** Kotlin Multiplatform (KMP) with Compose Multiplatform (CMP) for shared UI on iOS and Android.
- **Build:** Gradle (Kotlin DSL) with `libs.versions.toml` version catalog.
- **DI:** Koin.
- **Persistence:** Room (KMP).
- **Async:** kotlinx.coroutines + Flow.
- **Image loading:** Coil 3 (KMP).
- **Navigation:** Compose Navigation (KMP).
- **Testing:** `kotlin.test` + Turbine for Flows (JVM); Compose UI tests on Android; XCUITest for iOS-native polish.
- **Analytics & crash:** Firebase Analytics + Crashlytics on existing project `soularium-985bf`.
- **Distribution:** Fastlane for both stores; Firebase App Distribution for internal pre-release.

### Repo layout

Single monorepo. Web project lands as a sibling later.

```
soularium-v2/
├── mobile/
│   ├── composeApp/               # UI + platform glue (CMP)
│   │   ├── src/commonMain/
│   │   ├── src/androidMain/
│   │   ├── src/iosMain/
│   │   └── build.gradle.kts
│   ├── domain/                   # Pure Kotlin: entities, state machine, pure functions
│   ├── data/                     # Persistence, content loading, analytics impls
│   ├── iosApp/                   # Xcode wrapper project
│   ├── gradle/libs.versions.toml
│   └── settings.gradle.kts
├── web/                          # mysoularium.com (added in its own spec)
└── docs/superpowers/specs/
```

### Layer architecture

Strict one-direction dependency: `composeApp → domain ← data`.

- **`:domain`** — Pure Kotlin, no Android / iOS / Compose deps. Holds entities, the conversation state machine (§4), repository interfaces, and pure functions (image-pick validation, share-URL generation, summary composition). Runs and tests on JVM with zero mocks.
- **`:data`** — KMP. Implements `:domain` repository interfaces against Room (KMP), bundled resources, and Firebase SDKs (via `expect`/`actual`).
- **`:composeApp`** — KMP. CMP UI, ViewModels (`androidx.lifecycle.viewmodel.compose`, KMP), DI wiring, navigation, platform-specific intents in `androidMain`/`iosMain`.

### Why this shape

The single biggest structural improvement over the originals is pulling the conversation state machine into pure Kotlin. The originals entangled it with view controllers and activities, which made bookmark-and-resume fragile. Pure-domain + state-driven UI fixes that root cause.

---

## 3. Data model

### Entities

**`Session`** — a sit-down event.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `kind` | enum: `SOLO` \| `GROUP` | |
| `started_at` | Instant | |
| `ended_at` | Instant? | `null` until concluded |
| `bookmarked_at` | Instant? | `null` = not bookmarked; otherwise the sort key for the Bookmarked tab |
| `state_snapshot_json` | String | Serialized `SessionState` (see §4). Single JSON column rather than split fields, because the state is a tagged union with conditional members. Resuming = deserialize and render. |
| `selection_instructions_shown` | Boolean | One-time-per-session flag |

**`Conversation`** — one participant's record within a session.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `session_id` | UUID | FK → `Session.id` |
| `display_order` | Int | Turn order within session |
| `name` | String | Required |
| `surname` | String? | Optional, collected in contact step |
| `email` | String? | Optional |
| `phone` | String? | Optional, validated via libphonenumber |
| `notes` | String? | Optional |

**`CardPick`** — one card picked by one participant for one question.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `conversation_id` | UUID | FK → `Conversation.id` |
| `question_number` | Int | 1–5 |
| `card_id` | Int | 1–50, refers to bundled `CardImage` |
| `pick_order` | Int | Order within that question for that participant |
| `is_final` | Boolean | False = round-1 wide pick (Q1/Q2 only); true = final committed pick |

### Device state (DataStore / UserDefaults, not Room)

Per-device key/value store. Keys: `agreed_to_tos`, `has_seen_intro`, `device_owner_conversation_id`, `last_known_locale`.

### Bundled content (resources, not DB)

- 50 card images in `composeResources/drawable/` (sourced from the Android repo, which has the complete 50; iOS only had 48).
- `questions.json` in `composeResources/files/`: structural metadata (number, `selectionRounds`, `requiredImageCount`, string resource keys).
- All UI strings in `composeResources/values*/strings.xml`.

### Key design decisions

- **Card picks are normalized into a join table** instead of CSV strings on the conversation row. Enables aggregate queries ("most-picked card for Q3") in one line of SQL.
- **Solo conversations are still `Session`s** (with `kind=SOLO` and one `Conversation`). One model, not two. Uniform queries.
- **Device-owner identity in `device_state`**, not in the DB: `device_owner_conversation_id` points at the Conversation backing the "MySoularium" menu entry. Restarting MySoularium archives the old conversation (we keep it; the pointer moves). Improvement over the original's destructive overwrite.
- **Hard delete, not soft delete.** No undo requirement; add `deleted_at` later if a real reason emerges.
- **UUIDs for all primary keys** except `card_id`, which is the stable card number 1–50 (because it appears in the public share URL).
- **State machine snapshot lives on `Session`** as a single serialized JSON column. Resuming = deserialize back into `SessionState`. Avoids the schema-explosion of one nullable column per state-class variant.

---

## 4. Conversation state machine

The heart of the app. Lives in `:domain` as pure Kotlin.

### States

```kotlin
sealed interface SessionState {
    data object NotStarted : SessionState
    data object AddingParticipants : SessionState
    data class InQuestion(
        val questionNumber: Int,             // 1..5
        val activeParticipantIndex: Int,
        val activity: QuestionActivity,
    ) : SessionState
    data object Summary : SessionState
    data class CollectingContact(val participantIndex: Int) : SessionState
    data object Concluded : SessionState
}

enum class QuestionActivity {
    ShowingPrompt,        // "Your turn, Sarah" + question text
    ShowingInstructions,  // First-time-per-session help panel
    SelectingRound1,      // Wide selection
    SelectingRound2,      // Narrow to 3 (Q1/Q2 only)
    Finalizing,           // Confirm-or-change
    Discussing,           // Discussion screen for one participant's picks
}
```

### Events

```kotlin
sealed interface SessionEvent {
    data class StartSession(val kind: SessionKind) : SessionEvent
    data class AddParticipant(val name: String) : SessionEvent
    data class RemoveParticipant(val index: Int) : SessionEvent
    data object ConfirmParticipants : SessionEvent

    data object BeginSelection : SessionEvent
    data object DismissInstructions : SessionEvent
    data class PickCard(val cardId: Int) : SessionEvent
    data class UnpickCard(val cardId: Int) : SessionEvent
    data object ConfirmSelection : SessionEvent   // round1 -> round2 OR -> Finalizing
    data object ConfirmFinal : SessionEvent       // Finalizing -> Discussing
    data object EndDiscussion : SessionEvent      // next participant OR next question OR Summary

    data class CollectContact(val participantIndex: Int, val info: ContactInfo) : SessionEvent
    data object SkipContact : SessionEvent
    data object Conclude : SessionEvent

    data object Bookmark : SessionEvent
}
```

### Transition function

```kotlin
fun transition(state: SessionState, event: SessionEvent, ctx: SessionContext): TransitionResult
```

- `SessionContext` carries read-only data the function needs (participant list, question metadata, current draft selection).
- `TransitionResult` returns `next: SessionState` plus a list of `Effect`s — e.g., `PersistSelection`, `LogAnalytics(event, params)`, `EmitToast(msg)`.
- The transition is pure. The runtime layer in `:composeApp` interprets effects against real repositories.

### Invariants enforced in `transition`

1. No skipping questions. Q3 cannot be reached unless every participant completed Q2.
2. Selection counts are validated by the function, not the UI. Q1/Q2 require exactly 3 finals; Q3–Q5 require exactly 1.
3. One-round vs two-round selection is read from question metadata, not hardcoded in state names. State names reflect activity, not question number.
4. Bookmarking snapshots state exactly. Resuming reconstructs `SessionState` from the persisted snapshot.

### Testing

The entire conversation flow is example-tested in `:domain` on the JVM. Property-based tests cover invariant violations (attempts to over-pick, skip questions, etc.). All in milliseconds, no emulator. Target: ~100% line coverage of `:domain`.

---

## 5. Screens and navigation

### Screen inventory

| Screen | Purpose | Notes |
|---|---|---|
| Splash | OS-provided (Android 12+ splash API, iOS launch screen) | No custom Compose splash. |
| Intro | Two-page swipeable onboarding | Concept + Terms. Shown once per device. |
| Home | Brand hero + menu trigger | Menu lives in a `ModalBottomSheet`. |
| Conversation host | Hosts the entire 5-question flow | One destination, renders subscreens from `SessionState`. See below. |
| Past Conversations | List with two tabs: Completed, Bookmarked | **One row per `Session`** (not per participant). Group sessions show participant names inline (e.g. "Sarah, James, Priya"). Tap → opens Summary (completed) or Conversation host (bookmarked). Within Summary, navigate between participants. |
| About | Static content from the original | i18n string + markdown renderer. |
| Resources | Links to mysoularium.com, Cru, feedback, privacy, terms | External via system browser; `in-app://terms` routes internally. |
| Cards & Questions | Reference browser (all 50 cards + 5 questions) | Non-interactive. |
| Settings | Locale picker | Originally near-empty; locale picker is what's worth having. |

### Conversation host (the key one)

One Compose Navigation destination. Internally renders a different subscreen based on `SessionState`:

```kotlin
@Composable
fun ConversationHost(sessionId: SessionId, vm: ConversationViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    AnimatedContent(state, transitionSpec = ::stepCrossfade) { s ->
        when (s) {
            AddingParticipants -> AddParticipantsScreen(...)
            is InQuestion -> when (s.activity) {
                ShowingPrompt -> QuestionPromptScreen(...)
                ShowingInstructions -> InstructionPanelScreen(...)
                SelectingRound1, SelectingRound2 -> SelectionScreen(...)
                Finalizing -> FinalizingScreen(...)
                Discussing -> DiscussingScreen(...)
            }
            Summary -> SummaryScreen(...)
            is CollectingContact -> ContactCollectionScreen(...)
            Concluded -> ConclusionScreen(...)
            NotStarted -> error("invalid")
        }
    }
}
```

Why: the back stack can't fight the state machine, bookmark+resume "just works" by rendering from persisted state, and there's no segue logic to drift out of sync.

### Top-level nav graph

```
SplashRoute
 ├─→ IntroRoute (first-launch only)
 │    └─→ TermsRoute → HomeRoute
 └─→ HomeRoute
      ├─→ ConversationHost (new or resumed)
      ├─→ PastConversationsRoute
      │    ├─→ ConversationHost (resume bookmarked)
      │    └─→ SummaryRoute (read-only view of completed)
      ├─→ AboutRoute
      ├─→ ResourcesRoute
      ├─→ CardsAndQuestionsRoute
      └─→ SettingsRoute
```

### Platform-specific bits (`expect`/`actual`)

- **Share:** `UIActivityViewController` (iOS) / `Intent.ACTION_SEND` (Android), behind a `Sharer` interface.
- **Phone validation:** libphonenumber (Kotlin port if available, otherwise `expect`/`actual`).
- **Back gesture:** intercepted via Compose `BackHandler` (KMP); offers "bookmark and exit" or "discard" during a conversation.
- **External links:** system browser via platform URI handler.

### Not in v1

- No deep links (`soularium://`).
- No tab bar.
- No tablet / multi-window layout.

---

## 6. Content and localization

### Where content lives

- **Card images** in `composeResources/drawable/`. Migrated from the **Android repo** (which has all 50; iOS only had 48).
- **Question metadata** as Kotlin constants in `:domain` (structural data: number, rounds, required count, string resource keys).
- **UI strings** in `composeResources/values*/strings.xml`, one file per locale.
- **About copy** as i18n strings rendered via a small markdown-to-Compose helper (no WebView).
- **Resources links** as Kotlin data: `ResourceLink(titleKey, url)`. External URIs open in system browser.

### Locales

Six supported: `en` (default), `es`, `fr`, `pl`, `zh-Hans`. Plus Base for the fallback strings.

- Default: OS locale if supported, otherwise English.
- Override: Settings → Language picker, persisted in `device_state.last_known_locale`.
- Live switch: changing locale recomposes the UI immediately, no restart.

### Translation workflow

- **Source of truth:** `values/strings.xml` (English) in the repo. You edit this.
- **Tooling:** Crowdin via GitHub Actions (Option B in the brainstorm).
  - `crowdin.yml` config in the repo maps source to target paths.
  - A scheduled GitHub Action (e.g. weekly) runs `crowdin push` then `crowdin pull`, opens a PR with translation updates.
  - You review and merge.
- **Seed translations:** the existing iOS `Localizable.strings` files are imported into Crowdin as Translation Memory during initial setup. Translators get pre-filled suggestions for the ~80% of strings kept verbatim.
- **Deployment:** translations are bundled with the app at build time. New translations ship in the next app release. No OTA translation system.

### Ship strategy

English ships in v1. Other locales fill in as translations complete; first batch shipped in v1.x. Polish and Chinese specifically may need a fresh pass given the 5+ year gap.

### Right-to-left

Not in scope for v1 (no RTL locales). Compose handles RTL correctly if added later.

---

## 7. Cross-cutting

### Share URL contract (with `mysoularium.com`)

Lock now, never break.

**Format:**
```
https://mysoularium.com/my-life-in-pictures/?images=<id1>,<id2>,...,<id9>&person=<urlencoded-name>
```

**Encoding rules:**
- `images` is exactly **9 card IDs**, ordered: `Q1_final[3], Q2_final[3], Q3_final[1], Q4_final[1], Q5_final[1]`. Total = 9. Order is positional.
- Card IDs are integers `1–50`.
- `person` is URL-encoded UTF-8. First name only.
- No locale parameter in v1.

**Generation lives in `:domain`:**
```kotlin
fun shareUrlFor(conversation: Conversation, picks: List<CardPick>): String
```
Pure function, unit-tested, single source of URL format.

**Website-side requirement:** if `images` is missing, malformed, or fewer than 9 IDs, the site must still render gracefully (show whatever's there, no 500).

### Analytics

Firebase Analytics, project `soularium-985bf`. Event taxonomy:

```
screen_view            { screen_name }
session_started        { kind: solo|group, participant_count }
question_completed     { question_number, participant_index, picks_count }
session_completed      { kind, participant_count, duration_seconds }
session_bookmarked     { question_number }
session_resumed        { from_bookmark: bool }
session_deleted        { was_completed: bool }
share_initiated        { channel: sms|email|other }
contact_saved          { has_email, has_phone, has_notes }   ← booleans only
locale_changed         { from, to }
```

**Privacy invariants** (enforced in the analytics adapter, with tests):
- No participant names in events.
- No contact field values (emails, phones, notes) in events — only booleans of "did they fill this in."
- No `card_id` values in analytics — only counts.
- Crash reports get the same scrubbing.

Implementation: `AnalyticsTracker` interface in `:domain`; `expect`/`actual` Firebase wrappers in `:data`; runtime layer in `:composeApp` consumes effects from the state machine and dispatches.

### Error handling

Offline-first; small error surface area:

| Where | What can go wrong | Handling |
|---|---|---|
| Persistence (Room) | Write fails (full disk, corruption) | Snackbar: "Couldn't save your conversation. Try again." Log non-fatal to Crashlytics. State machine stays at last successful state. |
| Share intent | No SMS/email app installed | Snackbar: "No app available for sharing." Don't crash. |
| Phone validation | Invalid phone | Inline field error. Save anyway if user confirms. |
| Locale switch | Translation file corrupted/missing | Fall back to English silently. Log non-fatal. |
| Card asset load | Shouldn't happen — bundled | Placeholder card; log non-fatal. |

Domain functions return `Result<T, DomainError>` where they can fail. UI translates `DomainError` to user-facing copy (i18n'd). No global error boundary beyond Crashlytics for uncaught exceptions.

### Testing

| Layer | Where | What |
|---|---|---|
| `:domain` | JVM | State machine transitions (exhaustive), `shareUrlFor`, content selection rules. **Goal: ~100% line coverage.** |
| `:data` | Android instrumentation | Room queries, ContentRepository, analytics fakes. |
| `:composeApp` | Android emulator + iOS simulator | Smoke tests for key flows: complete a solo session end-to-end, bookmark + resume, delete a past conversation. Compose UI tests on Android; XCUITest for any iOS-native polish. |

Not in v1: visual regression (Paparazzi / Roborazzi), exhaustive snapshot tests, automated localization screenshots. v1.x candidates.

### Build, CI, distribution

- **CI:** GitHub Actions.
  - `ci.yml` — on PR: ktlint, unit tests, Compose UI tests on Android emulator. Gates merge.
  - `crowdin.yml` — scheduled: `crowdin push` + `crowdin pull`, opens translation update PR.
  - `release.yml` — on tag: builds release artifacts, uploads to Firebase App Distribution, then Play + App Store via Fastlane.
- **Internal distribution:** Firebase App Distribution.
- **Store distribution:** Fastlane lanes for both stores, Cru's existing developer accounts.
- **Versioning:** semantic `versionName`; `versionCode` = CI build number; iOS uses `CFBundleShortVersionString` + `CFBundleVersion`.

### Repo hygiene

- ktlint pre-commit + check in CI.
- `.editorconfig`.
- Renovate enabled (continuing the existing Android repo pattern).

---

## 8. Open questions / migration notes

These are explicitly NOT decisions for this spec — they're flagged for the implementation phase or for confirmation with Cru.

- **Card asset migration:** Android repo has 50 cards; iOS had 48. Confirm during the asset-migration task that the 50-card Android set is authoritative and there isn't a card on iOS missing from Android.
- **iOS bundle ID:** confirm whether the original iOS bundle ID matches `org.cru.soularium` or differs slightly. Affects whether we can claim back the App Store listing or need a new SKU.
- **Firebase project ownership:** confirm Cru still has admin access to `soularium-985bf`. If not, we may need a new project (and a coordinated change to package IDs is harder).
- **mysoularium.com domain ownership:** confirm Cru still controls the domain registration. Without it, the share URL contract is moot.
- **Crowdin tier:** confirm Cru has (or is willing to acquire) a Crowdin plan. Lokalise / Phrase / self-hosted Weblate are functionally equivalent alternatives if not.
- **App Store / Play Store accounts:** confirm Cru still has active developer accounts in good standing for both stores. If accounts have lapsed, restoration may take time.
- **Existing assets to import:** OpenSans TTF files (all 11 weights) — confirmed in both old repos, license-clean for re-use.
- **Translation re-pass for `pl` and `zh-Hans`:** plan for a translator review before re-shipping these locales, since iOS strings are 5+ years stale.

---

## 9. Phase 2 / future work (informational)

Not part of this spec; recorded so the v1 architecture leaves clean room:

- **Dark mode** — v1.1. Material 3 theming is already structured to support it; only color tokens need to be added.
- **AI assistance** — phase 2. The seam is a `SuggestionService` interface in `:domain` with no implementation in v1. When added, it becomes an `expect`/`actual` Claude API client; the state machine emits `RequestSuggestion` effects.
- **Multi-deck content** — phase 2. The data model already separates content from state; adding a `Deck` table and a deck selector is a small change.
- **Cloud sync / accounts** — undecided. Would require a backend and a major rethink of the data model. Out of scope.
- **Real-time multi-device group mode** — undecided. Major change; would require a backend.
- **Deep links** (`soularium://`) — small future addition; the nav graph supports it.

---

## 10. Acceptance criteria for v1

The rebuild is "v1 done" when:

1. A user can install the app on iOS and Android via TestFlight / Internal Track.
2. A user can complete a full **solo** session: start, see Intro/Terms once, accept, complete all 5 questions, see the summary, share via SMS or email, view the conversation in Past Conversations.
3. A user can complete a full **group** session of at least 4 participants, with turn-taking working across all 5 questions.
4. A user can **bookmark** mid-session and **resume** to the exact step + activity + participant they left off at.
5. A user can **delete** a past conversation.
6. The app runs in English with the i18n pipeline fully wired (Crowdin GitHub Action live, `values-*/strings.xml` populated by Crowdin, locale switching works at runtime). Spanish, French, Polish, and Chinese files exist as stubs and can be filled in via Crowdin without code changes. Polish and Chinese ship after translator review in v1.x.
7. Analytics events fire for the taxonomy in §7, scrubbed of PII per the privacy invariants.
8. Crash reports flow to Crashlytics for uncaught exceptions.
9. The share URL generated matches the contract in §7.
10. CI is green: ktlint, all unit tests, Compose UI smoke tests.
11. The app is accessible: VoiceOver / TalkBack labels on all interactive elements, contrast meets WCAG AA, tap targets ≥ 44pt / 48dp.
