---
name: pr-review
description: Review a pull request against Soularium v2 project conventions. Use when asked to review a PR, check code quality, or audit changes.
argument-hint: [pr-number]
allowed-tools: Bash, Read, Grep, Glob, Write, Edit
---

Review pull request $ARGUMENTS against the Soularium v2 project conventions.

## Steps

1. Check for dismissed issues by reading `.claude/skills/pr-review/dismissed-issues.md` if it exists.
   Load all dismissed entries — each has a **Pattern** and **Reason**. You will use these to suppress matching findings later.

2. Fetch the PR diff and metadata. If `$ARGUMENTS` is provided, use it as the PR number:
```
gh pr diff $ARGUMENTS
gh pr view $ARGUMENTS
```
If no PR number is given (or the above fails because no upstream PR exists), fall back to reviewing the current branch against `main`:
```
git diff main...HEAD
git log main...HEAD --oneline
```
Use the branch name and commit log as the "title" in the review header.

3. Identify all changed files and categorize them (domain, data, ui, platform actuals, build config, tests, resources).

4. Pre-flight checks — run ktlint and lint, recording results for the review:
```
./gradlew ktlintCheck
./gradlew lint
```
Any failures are reported as **❌ Must Fix** items in the review output. They do not stop the rest of the review.

5. Review each category using the checklist below. When reviewing Compose UI code, also load `.claude/rules/design_system_rules.md` — it defines the authoritative conventions for color tokens, typography, spacing, icons, components, and accessibility.

6. Before outputting, cross-reference every finding against dismissed patterns. A finding matches a dismissed pattern when it describes the same class of issue (not necessarily the exact file/line — match by concept). Move matched findings to a separate suppressed list.

7. Output a structured review (format below).

8. Post inline comments to the PR for every ⚠️ and ❌ finding that references a specific file and line number. **Skip this step entirely when reviewing a branch with no PR — there is nowhere to post.** Otherwise, before posting, deduplicate against all existing comments (resolved or not) to avoid re-posting anything already raised:

```bash
# Get the head SHA, repo, and all existing review comments (resolved and unresolved)
HEAD_SHA=$(gh pr view $ARGUMENTS --json headRefOid -q .headRefOid)
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
EXISTING=$(gh api repos/$REPO/pulls/$ARGUMENTS/comments --jq '[.[] | select(.in_reply_to_id == null) | {path:.path, line:.line, body:.body}]')
```

For each finding, check whether any existing comment (resolved or not) already covers the same file + line (or contains substantially the same text). Skip any finding that is already covered. Then bundle the remaining new comments into a single review submission:

```bash
gh api repos/$REPO/pulls/$ARGUMENTS/reviews \
  --method POST \
  --field commit_id="$HEAD_SHA" \
  --field event="COMMENT" \
  --field "comments[][path]=<file path>" \
  --field "comments[][line]=<line number>" \
  --field "comments[][side]=RIGHT" \
  --field "comments[][body]=<finding text>

🤖 Posted by [Claude Code](https://claude.ai/code)" \
  # repeat --field "comments[]..." for each new finding
```

Use the exact file path from the diff and the line number in the current version of the file (RIGHT side). Each comment body should contain the full finding description. Always append the attribution footer `\n\n🤖 Posted by [Claude Code](https://claude.ai/code)` to each comment. If no new actionable findings exist (only ✅ items or all already commented), skip this step.

9. **Resolve inline comment threads that have been addressed.** On a re-review, close out threads whose findings are now fixed. **Skip this step entirely when reviewing a branch with no PR — there are no threads to resolve.** A previously-posted thread counts as *addressed* when all of the following hold:
   - it is still **unresolved**, and
   - its first comment was posted by **this reviewer** — the body contains the `🤖 Posted by [Claude Code]` (or `🤖 [Claude Code]`) attribution footer — never touch a human reviewer's thread, and
   - the thread has **no other comments** — only the reviewer's original comment, with no replies (`comments.totalCount == 1`). If anyone has replied, there is an active discussion; leave the thread open even if the code looks fixed, and
   - **no finding in the current pass matches it** (the issue it described is no longer present in the code at that path). If the current review still raises the same finding, leave the thread open.

   Fetch unresolved threads (reuse the GraphQL shape from the dismissal section) — `totalCount` reveals whether anyone replied:
   ```bash
   REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
   OWNER=${REPO%%/*}; REPONAME=${REPO##*/}
   gh api graphql -f query="
   {
     repository(owner: \"$OWNER\", name: \"$REPONAME\") {
       pullRequest(number: $ARGUMENTS) {
         reviewThreads(first: 100) {
           nodes {
             id
             isResolved
             comments(first: 1) { totalCount nodes { id author { login } body path line } }
           }
         }
       }
     }
   }"
   ```

   For each addressed thread, reply noting it was fixed, then resolve it:
   ```bash
   gh api repos/$REPO/pulls/$ARGUMENTS/comments \
     --method POST \
     --field in_reply_to=<comment_id> \
     --field body="✅ Addressed in <short SHA> — resolving.

   🤖 [Claude Code](https://claude.ai/code)"

   gh api graphql -f query="
   mutation {
     resolveReviewThread(input: { threadId: \"<thread_node_id>\" }) {
       thread { id isResolved }
     }
   }"
   ```

   Only resolve findings you can confirm are fixed by reading the current code — when in doubt, leave the thread open. List the threads you resolved in the review output.

10. If the review has **no ❌ or ⚠️ findings** (only ✅ and/or ⏭️ items), ask the user whether to post the full review. **Skip this step entirely when reviewing a branch with no PR — branch review mode is local-only.** Otherwise, if they say yes:
   - Check whether the PR author matches the current git user (`gh pr view $ARGUMENTS --json author -q .author.login` vs `gh api user -q .login`)
   - If it is a **self-review**, post with `--comment` (GitHub does not allow self-approval)
   - If it is **someone else's PR**, ask whether to approve or just comment, then post with `--approve` or `--comment` accordingly
   - Always append `\n\n🤖 Posted by [Claude Code](https://claude.ai/code)` to the body

11. After the review output, print:

```
---
To dismiss a finding so it won't appear in future reviews, say:
  dismiss: <short title> — <reason>
```

---

## Review Checklist

### Architecture & Hexagonal Layering

The project enforces layering by package convention inside the single `:shared` module:

- [ ] Code in `org.cru.soularium.domain` does NOT import from `data`, `ui`, `platform`, `analytics`, or `di` packages — domain is pure Kotlin only
- [ ] Code in `org.cru.soularium.data` does NOT import from `ui`
- [ ] No Android (`android.*`, `androidx.*`) or iOS (`platform.*`, `kotlinx.cinterop`) imports in `commonMain` — bridge via `expect`/`actual`
- [ ] `:androidApp` depends on `:shared`; `:shared` has no dependency on `:androidApp`
- [ ] New top-level packages stay under `org.cru.soularium.*` — no other roots

### Domain Layer (`org.cru.soularium.domain`)

- [ ] Models are `@Serializable` (kotlinx.serialization) — required for state-snapshot persistence and share-link generation
- [ ] ID wrappers (`SessionId`, `ConversationId`, etc.) are `@Serializable @JvmInline value class` over UUID strings
- [ ] Ports (`ContentRepository`, `SessionRepository`, `DeviceStateRepository`, `AnalyticsTracker`, `CrashReporter`, `Sharer`) are defined as interfaces in `domain/ports/` — cross-platform implementations live in `data`; platform-specific implementations live in `androidMain`/`iosMain` (e.g. `AndroidSharer`, `IosSharer`) and are bound via `@ContributesBinding(AppScope::class)`
- [ ] `transition(state, event, ctx)` in `domain/session/` is **pure** — no I/O, no suspending calls, no `Dispatchers.*`. Side effects are returned as `Effect` data for the Presenter to execute
- [ ] New `SessionEvent` variants are added to the sealed hierarchy and handled exhaustively in `transition()` (no `else ->` swallowing)
- [ ] Errors surface via `TransitionResult.error` (`DomainError` sealed interface) — no `Result<T>` wrapper, no thrown exceptions for control flow

### Data Layer (`org.cru.soularium.data`)

- [ ] New Room entity columns are nullable-correct — repository mapping is total (no `!!` on optional columns)
- [ ] DAOs use `suspend fun` for single-shot queries and `Flow<T>` for reactive queries; `@Upsert` for inserts that may collide
- [ ] FK columns have `@ColumnInfo(index = true)` and FK declarations use cascade semantics consistent with existing entities
- [ ] **`SessionState` snapshot compatibility** — renaming or removing a `@Serializable` field in the `SessionState` sealed hierarchy (or any type reachable from it) breaks already-persisted sessions. Treat such changes as a schema change and call them out as **❌ Must Fix** unless a migration path is documented
- [ ] A `@Database(version = N)` bump ships a matching exported schema JSON in `shared/schemas/` AND a migration registered on the database builder
- [ ] `BundledSQLiteDriver` is used on both Android and iOS — no platform-specific driver swaps
- [ ] Device flags (intro seen, ToS agreed, locale) persist via DataStore Preferences — not Room
- [ ] Repositories map Room entities ↔ domain models; domain models never leak Room types out of `data`

### UI Layer — Circuit Presenter / Layout

**Presenter**
- [ ] Presenter class is `public` (Metro's Circuit codegen requires it) and annotated `@AssistedInject`
- [ ] `Navigator` (and `Screen`, when the screen carries args) injected via `@Assisted`; remaining constructor params are ordinary graph-injected deps
- [ ] Nested `@CircuitInject(<Feature>Screen::class, AppScope::class) @AssistedFactory fun interface Factory { fun create(navigator: Navigator[, screen: <Feature>Screen]): <Feature>Presenter }` drives codegen — no hand-written factory or graph accessor
- [ ] The `Factory` interface is placed at the bottom of the class (after `present()`), and its `create()` params match the constructor's `@Assisted` order (`navigator` first, then `screen`)
- [ ] Implements Circuit's `Presenter<UiState>`
- [ ] `UiState` is a nested `data class` implementing `CircuitUiState`, exposing `val eventSink: (UiEvent) -> Unit`
- [ ] `UiEvent` is a nested `sealed interface` implementing `CircuitUiEvent`
- [ ] State derived in `@Composable present()` via `remember { mutableStateOf(...) }`, `LaunchedEffect`, `produceState`, and repository `Flow`s collected with `collectAsState()`
- [ ] User intent flows in through `state.eventSink(...)` — no direct method calls from the Layout
- [ ] Cross-screen navigation goes through `navigator.goTo(SomeScreen(...))`; back is `navigator.pop()` — no ad-hoc navigation handles
- [ ] `navigator.pop()` (or any navigation call) placed *inside* the `launch { }` block when it follows an async persistence write — `rememberCoroutineScope()` is canceled on composition disposal and can cancel an in-flight write before it completes
- [ ] Coroutine launches inside `present()` are Compose-aware (`LaunchedEffect`, inside an `eventSink` callback, etc.) — bare `scope.launch { }` at the top level of `present()` re-runs on every recomposition
- [ ] Presenter contains no UI logic — pure state derivation and event handling

**Layout**
- [ ] Public, stateless `@Composable fun <Feature>Layout(state: <Feature>Presenter.UiState, modifier: Modifier = Modifier)`, carrying `@CircuitInject(<Feature>Screen::class, AppScope::class)` on the declaration (generates the `Ui.Factory`)
- [ ] `modifier` is the **last** parameter and is the first thing applied to the root composable
- [ ] Reads fields off `state` and emits intent via `state.eventSink(...)` — owns no business logic
- [ ] Layout-local `remember { mutableStateOf(...) }` only for transient view-only state (text-field drafts, expanded/collapsed toggles)
- [ ] Private sub-composables in the file are marked `private`

**Screen & wiring**
- [ ] New `Screen` destinations are `@Parcelize` `data object`/`data class` types in `ui/nav/Screens.kt`
- [ ] Presenter + Layout wired via `@CircuitInject(<Feature>Screen::class, AppScope::class)` codegen — Metro generates the `Presenter.Factory` / `Ui.Factory` and contributes them to the multibindings consumed by `CircuitBindings.providesCircuit`. There is no hand-written factory or switch table
- [ ] Loading / error / empty states are first-class on every screen that loads data (the app is offline-first, but `DomainError.PersistenceFailed` still needs a path)

### Compose / Design System

Cross-reference `.claude/rules/design_system_rules.md` while reviewing UI code.

- [ ] No raw `Color(0xFF...)` literals in screen files — use `MaterialTheme.colorScheme.*`. Sentinels `Color.Transparent` and `Color.Unspecified` are allowed
- [ ] No raw `fontSize` / `fontWeight` on `Text` — use `MaterialTheme.typography.*` slots from `soulariumTypography()`
- [ ] No per-screen `MaterialTheme(...)` or `SoulariumTheme(...)` wrappers — `SoulariumTheme { }` is applied once at the app root
- [ ] **No dark-mode branching** — no `isSystemInDarkTheme()` checks, no `darkColorScheme(...)`. The app is light-only by design
- [ ] Spacing stays on the 4dp grid; `Arrangement.spacedBy(N.dp)` is preferred over per-child padding for sibling spacing
- [ ] Material3 components used before reaching for primitives (`Button`/`Card`/`Scaffold`/`TopAppBar`, etc.)
- [ ] Icons come from `androidx.compose.material.icons.Icons.*` — no hand-rolled `ImageVector` paths
- [ ] Every `Icon`/`Image` has a meaningful `contentDescription`, or `null` only when adjacent text already conveys the meaning
- [ ] Touch targets ≥ 48dp (wrap small icons in `IconButton` or use `Modifier.minimumInteractiveComponentSize()`)
- [ ] Modifier order: caller `modifier` → size → padding → background/clip → interaction → semantics
- [ ] `LazyColumn`/`LazyRow` use `key = { it.id }` whenever items can be added/removed/reordered — and the key is non-null
- [ ] Bundled card drawables loaded via `painterResource(Res.drawable.card_NN)` — not file paths or resource IDs
- [ ] When a new brand color is genuinely needed, it is added to `ui/theme/Color.kt` and (if appropriate) wired into `SoulariumLightColors` — not inlined in a screen

### Kotlin Multiplatform — expect / actual

- [ ] Every `expect` declaration has an `actual` for **both** `androidMain` and `iosMain` with matching signatures
- [ ] Android-specific APIs live in `androidMain`; iOS-specific in `iosMain`; `commonMain` stays platform-neutral
- [ ] New platform seams (`Sharer`, `PlatformBackHandler`, database/datastore builders, etc.) follow the `expect val`/`expect fun` + per-platform `actual` pattern — not `if (Platform.isAndroid)` branching
- [ ] `iosArm64` and `iosSimulatorArm64` are both covered when adding iOS-specific code (no single-target actuals). `iosX64` is intentionally NOT a configured target (Compose Multiplatform 1.11.x stopped publishing its `iosX64` binaries) — flag any attempt to re-add it without a documented reason

### Dependency Injection — Metro

DI is compile-time via [Metro](https://github.com/ZacSweers/metro). The graph is `org.cru.soularium.di.SoulariumAppGraph` (a `@DependencyGraph(AppScope::class)` interface) and bindings come from three sources: `@Inject`-annotated impl classes that self-contribute via `@ContributesBinding`, `@BindingContainer @ContributesTo(AppScope::class)` interfaces with `@Provides` for non-injectable types (`DataBindings`, `CircuitBindings`), and an `expect class PlatformBindings @BindingContainer` passed via `@Includes` on the graph factory.

- [ ] New impl classes use `@Inject` (constructor or class) and bind to their port via `@ContributesBinding(AppScope::class)`; app-lifetime singletons are scoped with `@SingleIn(AppScope::class)`
- [ ] Non-constructor-injectable types (Room database/DAOs, factory-built domain services like `createDeviceStateRepository()`) go through `@Provides` inside a `@BindingContainer @ContributesTo(AppScope::class)` interface — see `DataBindings.kt`
- [ ] Platform-specific bindings live in `expect class PlatformBindings` actuals. Android actual takes `(context: Context)` and provides it as `@Provides @SingleIn(AppScope::class) internal val context`; iOS actual is empty (Sharer/AnalyticsTracker/CrashReporter impls are common with `@ContributesBinding`)
- [ ] Set multibindings (`Set<Presenter.Factory>`, `Set<Ui.Factory>`) declared with `@Multibinds(allowEmpty = true)` in `CircuitBindings`; new factories contributed via `@Provides @IntoSet` or `@ContributesIntoSet(AppScope::class)`
- [ ] New graph accessors on `SoulariumAppGraph` are added ONLY when the consumer is the Compose root (`App.kt` is the only call site today) — everything else takes deps via `@Inject` constructor params, not via the graph
- [ ] New screens add a `Screen` to `ui/nav/Screens.kt`; the Presenter+Layout `@CircuitInject` annotations drive factory codegen — no manual factory registration. Presenter deps come from the graph via `@Inject` constructor params
- [ ] `createSoulariumAppGraph(PlatformBindings(...))` is called once per entry point — `SoulariumApplication.onCreate()` on Android, `MainViewController()` on iOS — and the graph is passed into `App(graph)` rather than rebuilt per recomposition
- [ ] No use of Koin, Hilt, Dagger, or Anvil annotations — DI is Metro-only

### Module Build Files

- [ ] `:shared` stays a KMP library via `com.android.kotlin.multiplatform.library` — do NOT introduce `com.android.application` or `com.android.library` into `:shared` (AGP 9 forbids mixing with the KMP plugin)
- [ ] `:androidApp` stays a pure `com.android.application` shell — does NOT apply the Kotlin Multiplatform plugin
- [ ] Cross-module Gradle conventions live in `build-logic/src/main/kotlin/*-conventions.gradle.kts` and are applied via `id("<name>-conventions")` (currently `ktlint-conventions`). Module-specific config stays in each module's `build.gradle.kts` using `libs.versions.toml` aliases. Convention plugins must be reusable across modules — flag a new convention plugin used by only one module as a **❌ Must Fix**
- [ ] All dependency coordinates use `libs.*` aliases from `gradle/libs.versions.toml` — no inline `"group:artifact:version"` strings
- [ ] New dependencies add entries to `libs.versions.toml` and follow existing naming
- [ ] `minSdk 24`, `compileSdk 36`, `targetSdk 36`, JVM target 17 — version bumps need explicit justification
- [ ] Application id stays `org.cru.soularium` (debug builds get the `.dev` suffix automatically); the application id and build types live in `:androidApp`, NOT `:shared`
- [ ] iOS bundle id stays `org.cru.soularium`
- [ ] ktlint excludes anything under `build/` (already configured at the root) — no per-module re-enabling of generated-source linting

### Resources & Strings

- [ ] User-visible strings come from `stringResource(Res.string.<key>)` — no inline literals in composables, including `contentDescription` values
- [ ] Source strings added to `shared/src/commonMain/composeResources/values/strings.xml`
- [ ] No hand-edits to translated string files in `values-es/`, `values-fr/`, `values-pl/`, `values-zh-rCN/` — these come from Crowdin
- [ ] `Res` imports come from `org.cru.soularium.generated.resources` — the configured `packageOfResClass`
- [ ] Drawables placed under `composeResources/drawable/`; fonts under `composeResources/font/`

### Testing

- [ ] Tests live in `commonTest` — no Android instrumented tests, no Compose-UI instrumented tests
- [ ] Frameworks: `kotlin.test` (`@Test`, `@BeforeTest`, `@AfterTest`), Kotest assertions (`io.kotest.matchers.*`), Turbine for `Flow` assertions, `kotlinx-coroutines-test` (`runTest`, `TestDispatcher`, `advanceUntilIdle`) — no JUnit4, no `runBlocking`, no manual `collect` + coroutine coordination
- [ ] Presenter tests are written with Circuit's `circuit-test` (`FakeNavigator`, `presenter.test { awaitItem().eventSink(...) }`)
- [ ] Presenter tests are annotated `@RunOnAndroidWith(AndroidJUnit4::class)` so the Android-host variant runs them under Robolectric — required because the Compose Runtime's Android artifact touches `android.util.Log` on its error path. Pure domain tests are unannotated
- [ ] Test doubles are plain in-memory classes in the test sources (e.g. `InMemorySessionRepository`, `RecordingSharer`) — no `mockk`, no `test-fixtures` modules
- [ ] Coroutine tests use `runTest { }` with an injected `TestDispatcher`; Flow tests use Turbine (`flow.test { awaitItem() }`)
- [ ] Test function names are backtick-quoted descriptive sentences (e.g. `` `solo session completes from start through summary` ``)
- [ ] The pure session state machine (`transition()`) has exhaustive case coverage; share-URL generation and other pure utilities have explicit edge-case tests

### Kotlin Code Quality

- [ ] Logging goes through the injected `CrashReporter` / `AnalyticsTracker` ports — no `println`, no Android `Log.*`, no `System.out.println`
- [ ] Exception handling catches specific types — bare `catch (e: Exception)` / `catch (t: Throwable)` is flagged unless catching all is intentional
- [ ] Multi-branch conditionals on sealed types use `when` (exhaustive, no `else ->` swallowing additions)
- [ ] Visibility is intentional: `internal` for module-scoped symbols, `private` where possible — sealed UI/Domain types in particular should not leak public when `internal` suffices
- [ ] Non-cancellable critical sections use `launch(start = CoroutineStart.UNDISPATCHED) { withContext(NonCancellable) { … } }` — NOT `launch(NonCancellable) { … }` (passing `NonCancellable` to `launch` replaces the parent `Job`, breaking structured concurrency)
- [ ] `Bundle`/`Intent` extra keys, if any are added in `:androidApp`, are `const val` shared between producer and consumer

### Code Style

Ktlint with the `android_studio` code style (plus `.editorconfig`) enforces most rules — line length, formatter rules, trailing newline — and step 4's pre-flight already covers those. Manual checks:

- [ ] **Trailing commas** — ktlint's trailing-comma rules are disabled in `.editorconfig` (it is a deliberately not-one-size-fits-all choice), so the pre-flight catches none of these; check by hand. The convention:
  - **Only touch a trailing comma on a line you are already modifying.** If the surrounding code is otherwise unchanged, leave its comma (or lack of one) as-is — do not churn it either direction.
  - A call or signature flattened onto a **single line** takes **no** trailing comma.
  - A multi-line **non-`Modifier`** call or signature **does** take a trailing comma on its last argument — this avoids churn when a later argument is added.
  - A **`Modifier` chain** is placed as the **last** parameter of a composable call and takes **no** trailing comma after its final `.foo()` — this keeps adding a new modifier to the end from churning the previous line. A single-modifier chain may sit on one line (`modifier = Modifier.height(52.dp)`), with or without a comma; a chain of two or more goes one modifier per line with no trailing comma:
    ```kotlin
    modifier = Modifier
        .background(color)
        .padding(16.dp)
    ```
- [ ] Package prefix: `org.cru.soularium` (with `org.cru.soularium.app` reserved for `:androidApp`)
- [ ] `@Composable` functions may use capitalized names (ktlint rule exempt); other functions are camelCase
- [ ] Files end with a trailing newline; no trailing whitespace

### Deprecated API Usage

Scan changed files for `@Deprecated` usages. Flag each as a **Minor Issue** (⚠️) with a suggested replacement. To surface deprecated usages introduced or touched in the diff:

```bash
./gradlew :shared:testAndroidHostTest 2>&1 | grep -i "deprecat"
```

### PR Hygiene

- [ ] No unrelated auto-formatter whitespace changes mixed into the diff (check with `git diff main...HEAD --stat` — flag files with churn that don't match the stated PR scope)
- [ ] `google-services.json`, `GoogleService-Info.plist`, and `local.properties` are NOT committed — these are gitignored for a reason
- [ ] Crowdin-managed translation files are not hand-edited (changes will be overwritten by the next download)
- [ ] Schema export JSONs in `shared/schemas/` are committed alongside the corresponding `@Database` version bump

---

## Output Format

Structure the review as (use `## PR Review: <title> (#<number>)` when reviewing a PR, or `## Review: <branch-name>` when reviewing a branch locally):

```
## PR Review: <title> (#<number>)

### Summary
<1–2 sentence summary of what the PR does>

### Checklist Findings

#### ✅ Looks Good
- <item>

#### ⚠️ Minor Issues
- <file:line> — <issue> — <suggested fix>

#### ❌ Must Fix
- <file:line> — <issue> — <suggested fix>

#### ⏭️ Suppressed
- <short title> — dismissed: <reason>
(omit this section entirely if nothing was suppressed)

### Overall Verdict
APPROVE / REQUEST CHANGES / COMMENT
<brief rationale>
```

Be specific. Reference file paths and line numbers. Cite the relevant convention from CLAUDE.md or `.claude/rules/design_system_rules.md` when flagging an issue.

---

## Handling Dismissals

When the user says `dismiss: <title> — <reason>` (in any form — "dismiss the X issue because Y", etc.):

1. Read `.claude/skills/pr-review/dismissed-issues.md` if it exists (create it if not).
2. Run `git config user.name` to get the current user's name.
3. Append a new entry in this format:

```markdown
## <title>
**Pattern**: <describe the class of issue broadly enough to match future occurrences>
**Reason**: <reason the user gave>
**Dismissed**: <today's date as YYYY-MM-DD>
**Dismissed by**: <git user.name>
```

4. If the current session reviewed a PR, find any open (unresolved) comment thread on that PR matching the dismissed issue. Use the GraphQL API to locate threads and resolve the matching one, replying with the dismissal reason first:

```bash
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
OWNER=${REPO%%/*}
REPONAME=${REPO##*/}

# Find unresolved review threads
gh api graphql -f query="
{
  repository(owner: \"$OWNER\", name: \"$REPONAME\") {
    pullRequest(number: $PR_NUMBER) {
      reviewThreads(first: 100) {
        nodes {
          id
          isResolved
          comments(first: 1) {
            nodes { id body path line }
          }
        }
      }
    }
  }
}"
```

Match the thread by file path, line number, or substantial text overlap with the dismissed finding. Then reply to the thread and resolve it:

```bash
# Reply to the thread's first comment explaining the dismissal
gh api repos/$REPO/pulls/$PR_NUMBER/comments \
  --method POST \
  --field in_reply_to=<comment_id> \
  --field body="Dismissed: <reason given by user>

🤖 [Claude Code](https://claude.ai/code)"

# Resolve the thread via GraphQL
gh api graphql -f query="
mutation {
  resolveReviewThread(input: { threadId: \"<thread_node_id>\" }) {
    thread { id isResolved }
  }
}"
```

5. Confirm to the user what was added and that it will be suppressed in future reviews.
