---
name: agent-review
description: Multi-agent PR review with smart selection, debate rounds, and consensus
allowed-tools: Bash(gh pr view:*), Bash(gh pr diff:*), Bash(gh api:*), Bash(gh workflow run:*), Bash(gh auth status:*), Bash(git diff:*), Bash(git branch:*), Bash(git log:*), Bash(git status:*), Bash(git add:*), Bash(git commit:*), Bash(date:*), Bash(jq:*), Bash(cat > .ai-review.json:*), Bash(rm .ai-review.json), Bash(./gradlew:*), Bash(gradlew:*), Read, Grep, Glob, Write, Edit
---

# Multi-Agent PR Code Review

AI-powered code review with smart agent selection, cross-examination debate, and consensus-driven findings.

**Usage**:

```bash
/agent-review           # Standard mode (smart selection, recommended)
/agent-review quick     # Quick feedback for simple PRs
/agent-review deep      # Comprehensive analysis for critical changes
```

---

## Stage 0A — Parse Review Mode & Initialize

### Determine Review Mode

Check command argument to determine mode:

- **quick**: 3 agents (Testing, Standards, Architecture), model: sonnet
- **standard** (default): Smart agent selection based on changes + coverage gap review, model: opus
- **deep**: All 6 agents + coverage gap review (expanded), model: opus

Print the mode banner:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[MODE BANNER based on selection]

quick:
  🏃 QUICK REVIEW MODE
  • 3 agents (Testing, Standards, Architecture)
  • Model: sonnet (fast, cost-effective)
  • Estimated time: ~2-3 minutes

standard:
  ⚡ STANDARD REVIEW MODE (Recommended)
  • Smart agent selection based on changes + coverage gap review
  • Model: Opus
  • Estimated time: ~6-11 minutes

deep:
  🔬 DEEP REVIEW MODE
  • All 6 agents + expanded coverage gap review
  • Model: Opus (maximum quality)
  • Estimated time: ~12-18 minutes

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

MODE: REVIEW ONLY of the current PR diff. Do NOT modify existing files or stage/commit.

---

## Stage 0B — Context Gathering & Risk Assessment

### Gather PR Context

```bash
# Check if we're in a PR branch
gh pr view --json number,title,baseRefName,headRefName,additions,deletions,changedFiles 2>/dev/null || echo "Not in a PR branch, using main as base"

# Get the day of week for reviewer recommendations
date +%A
```

Get the diff using PR refs (with fallback):

```bash
BASE_REF=$(gh pr view --json baseRefOid -q .baseRefOid 2>/dev/null)
HEAD_REF=$(gh pr view --json headRefOid -q .headRefOid 2>/dev/null)

if [ -n "$BASE_REF" ] && [ -n "$HEAD_REF" ]; then
  git diff $BASE_REF..$HEAD_REF --name-only
  git diff $BASE_REF..$HEAD_REF --stat
  git diff $BASE_REF..$HEAD_REF
else
  # Fallback: use gh pr diff (handles stacked PRs correctly)
  # IMPORTANT: Do NOT use git diff main...HEAD — it includes parent branch changes for stacked PRs
  gh pr diff --name-only 2>/dev/null || git diff main...HEAD --name-only
  gh pr diff 2>/dev/null || git diff main...HEAD
fi
```

**No PR / no remote:** This repo may have no git remote configured and no open PR. If `gh pr view` fails, fall back to `git diff main...HEAD` against the local `main` branch and run the review in terminal-only mode (Stage 7 delivers to the terminal; the auto-approve trigger is skipped). All `gh` commands in later stages auto-detect the repository from the current checkout — never hardcode an `owner/repo` slug.

Store the changed file list and diff content for use by all agents.

### Read Project Standards

Read `.claude/CLAUDE.md` to understand the project's coding standards and conventions — it is the single authoritative description of the architecture (module map, layering, Circuit UI, Metro DI, persistence, testing); wherever this command's stage descriptions and CLAUDE.md disagree, CLAUDE.md wins. `.claude/skills/pr-review/SKILL.md` carries the detailed per-category review checklist and is kept current — prefer citing those two files over restating architecture from memory. This context will be shared with all agents. Additionally, if any UI files change in this PR (anything under `shared/src/commonMain/kotlin/**/ui/**` or files containing `@Composable`), read `.claude/rules/design_system_rules.md` for Figma → Compose translation conventions, Material3 token mapping, and accessibility requirements — this is mandatory context for the UX agent and informational for the others.

### Calculate Risk Score

Start with a base score of 0, then add points.

**Dedup rule:** For each changed file, match against the highest-risk pattern first (Critical, then High, then Medium). Each file contributes points from at most one risk tier — do not double-count.

**Critical File Patterns (+3 points each):**
- `shared/src/commonMain/kotlin/org/cru/soularium/di/**` — Metro DI graph (`SoulariumAppGraph`, binding containers); a misconfigured graph fails the whole app
- `module/db/src/**` — Room database, entities, DAOs, repository implementations (`SoulariumDatabase`)
- `module/db/schemas/**` — Room exported schema JSON (production data shape)
- `androidApp/src/main/AndroidManifest.xml` — Android manifest (exported components, permissions)
- `iosApp/**` — iOS app shell wiring (native SwiftUI host for the Compose framework)
- `gradle/libs.versions.toml` — Version catalog (lib/plugin upgrades)
- `settings.gradle.kts` — Module graph (adding/removing modules)
- `build.gradle.kts` (root) and `build-logic/**` — top-level build configuration and the shared convention plugins
- Each module's `build.gradle.kts` (`shared/`, `androidApp/`, `module/*/`) — module build configuration
- `local.properties`, `**/*.keystore`, signing configs — secrets/credentials
- `google-services.json`, `GoogleService-Info.plist` — Firebase config (should be gitignored)
- `.env*` — Environment files (automatic senior review)
- `.github/workflows/ai-review-auto-approve.yml` — AI auto-approval workflow (controls which PRs bypass human review)
- `.claude/commands/*.md` — Review process definitions (controls how AI reviews behave)
- `.claude/rules/*.md` — Repo-specific rule references loaded by review commands

**High-Risk File Patterns (+2 points each):**
- `module/game/src/**` — the pure session state machine (`transition`, `Effect`, `GameError`) and the `Question` content catalog
- `module/model/src/**` — domain models, including the persisted `@Serializable` `SessionState` hierarchy (snapshot compatibility)
- `shared/src/commonMain/**/domain/**` — hexagonal ports (`domain/ports/`) and domain services
- `shared/src/commonMain/**/data/**` — DataStore-backed device flags and repository glue
- Files containing `expect class`/`expect fun`/`expect val`/`expect object` (or their `actual` counterparts) — KMP platform contracts
- `shared/src/iosMain/**`, `shared/src/androidMain/**`, `module/*/src/iosMain/**`, `module/*/src/androidMain/**` — platform-specific wiring (`PlatformBindings`, the platform `RoomBindings`)
- `shared/src/commonMain/kotlin/**/ui/nav/**` — Circuit `Screen` declarations (navigation destinations)
- `.github/workflows/*` (not already counted) — CI/CD workflows
- `.github/workflows/crowdin-upload.yml`, `.github/workflows/crowdin-download.yml` — i18n sync workflows (handle `CROWDIN_*` secrets)

**Medium-Risk File Patterns (+1 point each):**
- `shared/src/commonMain/kotlin/**/ui/**` — Circuit Presenters/Layouts, theme (excluding `ui/nav/**` which is High)
- `module/analytics/src/**`, `shared/src/commonMain/kotlin/**/analytics/**` — the `AnalyticsTracker`/`CrashReporter` ports and `scrubAnalyticsParams`

**Low-Risk Files (0 points):**
- `**/commonTest/**`, `**/androidHostTest/**`, `**/test-fixtures/**` — test sources and shared test fakes
- `*.md` (not `.claude/commands/*.md` and not `.claude/rules/*.md`) — Documentation
- `**/composeResources/**` — strings, drawables, fonts
- `shared/src/androidHostTest/snapshots/**` — Paparazzi snapshot PNGs (validated by CI's `verifyPaparazzi`)
- `**/generated/**`, `**/build/**`, `**/.kotlin/**` — generated/build output (should not be committed)

**Change Volume Modifier** (exclude test sources — `**/commonTest/**`, `**/androidHostTest/**` — and snapshot PNGs from the line count and ignore whitespace-only changes with `--ignore-all-space`):
- <50 lines: +0
- 50-199 lines: +1
- 200-299 lines: +2
- 300+ lines: +3

**Scope Multiplier** (apply after base score):
- Single domain (e.g., only test sources, or only one module): ×1.0
- Multiple layers (e.g., `ui` + `data` repository + `domain` model packages): ×1.3
- Cross-cutting (e.g., DI wiring + Room schema + a `@Serializable` domain rename + CI workflow): ×1.7

**Special Pattern Detection (additional points):**
- New library or plugin entry in `gradle/libs.versions.toml`: +2
- Version bump for a critical lib in `gradle/libs.versions.toml` (`kotlin`, `agp`, `compose-multiplatform`, `room`, `metro`, `circuit`, `coroutines`, `ksp`): +3
- New Room migration / `@Database` version bump without a regenerated exported schema JSON under `module/db/schemas/`: +3 (production data shape change without the schema artifact — red flag)
- Room `module/db/schemas/**/*.json` edited without a matching `@Database` version bump in this PR: +3 (likely a manual edit — red flag)
- New `expect`/`actual` pair: +1 (sets a KMP contract — verify every active target has an `actual`)
- New Circuit Presenter without a corresponding test under `commonTest`: +1
- `@Serializable` field rename or removal in `module/model/src/**`: +2 (`SessionState` is persisted as a JSON snapshot string in the database — renaming or removing a serialized field breaks already-persisted sessions)

Cap the final score at 10.

**Risk Level Mapping:**
- **0–3**: LOW
- **4–6**: MEDIUM
- **7–8**: HIGH
- **9–10**: CRITICAL

Display the risk assessment:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 PR RISK ASSESSMENT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Risk Score: [X]/10
Risk Level: [LOW | MEDIUM | HIGH | CRITICAL]

Files Changed: [N]
Lines Changed: +[X] -[Y]

Risk Factors Detected:
• [specific factors with point values]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
👥 REVIEW RECOMMENDATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Required Reviewer Level: [ANY | MID-LEVEL/SENIOR | SENIOR]
Reasoning: [1-2 sentence explanation]

[Day-of-week warnings if applicable]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Stage 0C — Smart Agent Selection

### Available Agents

This review system uses 6 specialized agents (no Financial agent — this codebase has no financial domain):

1. **Security** — DI wiring integrity, the exported `MainActivity`, `INTERNET`-only permission surface, PII in analytics/share URLs/logs, Room SQL-injection (`@RawQuery`), CI/CD workflow integrity, Firebase/signing config
2. **Architecture** — module dependency graph + package-layer direction (`ui` → `domain`/`data`; `data` → `domain`; nothing flows back into `domain`), hexagonal ports, the pure session state machine (`:module:game`), Metro DI scoping, Circuit Presenter/Layout discipline, expect/actual correctness, source-set discipline, Compose recomposition, structured concurrency
3. **Data Integrity** — Room migrations and schema (`:module:db`, `SoulariumDatabase`), repository implementations, the persisted `SessionState` JSON snapshot, kotlinx.serialization compatibility, threading
4. **Testing** — kotlin.test, Kotest assertions, Turbine, `kotlinx-coroutines-test` (`runTest`), Circuit `circuit-test` Presenter tests, pure-function tests, test fakes (`test-fixtures` modules + local doubles), Paparazzi screenshot coverage
5. **UX** — Compose Multiplatform composition, stateless Circuit Layouts, Material3 token usage from the theme, accessibility (`Modifier.semantics`), state hoisting, dark mode
6. **Standards** — `.claude/CLAUDE.md` compliance, ktlint (`android_studio` style), module-build conventions, package naming

### Selection Logic

**Quick mode**: Testing, Standards, Architecture (always these 3)

**Deep mode**: All 6 agents

**Standard mode**: Smart selection based on changed files:

Always include: Architecture, Testing, Standards

Conditionally include:

- **Security Agent** — if any of these patterns appear in changed files:
  - `shared/src/commonMain/kotlin/org/cru/soularium/di/**` (Metro DI wiring)
  - `androidApp/src/main/AndroidManifest.xml` (exported components, permissions)
  - `.github/workflows/**` (CI/CD security controls, especially auto-approve and the Crowdin secrets)
  - `.claude/commands/`, `.claude/rules/` (review process definitions that control AI review behavior)
  - `module/analytics/src/**`, `shared/src/commonMain/kotlin/**/analytics/**` (analytics — PII scrubbing)
  - Any share-URL/share-text generation code
  - `local.properties`, `**/*.keystore`, signing configs, `google-services.json`, `GoogleService-Info.plist`
  - `.env*` files

- **Data Integrity Agent** — if any of these patterns appear:
  - `module/db/src/**` (Room entities, DAOs, repository implementations)
  - `module/db/schemas/**` (Room exported schema JSON)
  - `module/model/src/**` `@Serializable` models (the persisted `SessionState` contract surface)
  - `shared/src/commonMain/**/data/**` (DataStore device flags)
  - Files with `expect class`/`expect fun`/`expect val`/`expect object` declarations or their `actual` counterparts
  - Room migrations or any `@Database` version change

- **UX Agent** — if any of these patterns appear:
  - `shared/src/commonMain/kotlin/**/ui/**` (Circuit Presenters/Layouts, theme, `Screen` declarations)
  - Files containing `@Composable` declarations
  - Compose resource files under `**/composeResources/**`

Display selection results:

```
🤖 Analyzing PR to select relevant agents...

✅ Architecture Agent — Always included
✅ Testing Agent — Always included
✅ Standards Agent — Always included
[✅/❌] Security Agent — [reason]
[✅/❌] Data Integrity Agent — [reason]
[✅/❌] UX Agent — [reason]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Selected: [N] of 6 agents
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Stage 0D — Load Dismissed Findings

Check for findings that the developer has previously dismissed via `/dismiss` replies on review comments. This only applies when re-running the review on a PR that already has a previous agent review.

**Step 1: Fetch PR review comments and replies**

```bash
PR_NUM=$(gh pr view --json number --jq '.number' 2>/dev/null)
PR_AUTHOR=$(gh pr view --json author --jq '.author.login' 2>/dev/null)

# Fetch all review comments (includes replies) on this PR.
# gh resolves {owner}/{repo} from the current checkout — do NOT hardcode a slug.
gh api "repos/{owner}/{repo}/pulls/${PR_NUM}/comments" --paginate
```

**Error handling:** If the `gh api` call fails (rate limiting, auth failure, network error, or no remote configured), display a warning banner: "⚠️ Could not load dismissed findings — all findings will be treated as new." Proceed with an empty dismissed list and set a flag so Stage 5 can note that dismissal matching was skipped.

**Note:** This only checks review comment threads (replies to inline code comments), NOT standalone PR comments. The `/dismiss` command must be used as a reply to the specific finding comment.

**Step 2: Identify dismissed findings**

For each review comment that is a reply (`in_reply_to_id` is set):
1. Check if its `body` starts with `/dismiss` (case-insensitive)
2. Verify the reply author (`user.login`) matches the PR author — only the PR author can dismiss findings
3. Look up the parent comment (by `in_reply_to_id`) to get the original finding
4. Parse the parent comment's `<!-- severity:X -->` tag
5. **Only allow dismissal if severity < 7** — findings with severity ≥ 7 (Important, High, Critical) cannot be dismissed. If someone tries to dismiss a severity ≥ 7 finding, ignore the dismissal and note it in the output
6. Extract the dismiss reason from the reply body (everything after `/dismiss:` or `/dismiss`)

**Step 3: Build the dismissed findings list**

For each valid dismissal, store:
- `path`: The file path from the parent comment
- `line`: The line number from the parent comment
- `body`: The finding body text (stripped of the `<!-- severity:X -->` tag)
- `reason`: The developer's dismiss reason
- `dismissed_by`: The developer's GitHub username

**Step 4: Display results**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📝 DISMISSED FINDINGS CHECK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[If dismissals found]:
Found [N] previously dismissed finding(s):
• shared/.../FooPresenter.kt:42 — "Consider hoisting..." (dismissed: "Intentional design choice")
• module/db/.../FooRepository.kt:15 — "Add test for edge case" (dismissed: "Covered by state-machine test")

[If severity ≥ 7 dismissals attempted]:
⚠️  Ignored [N] invalid dismissal(s) (severity ≥ 7 findings cannot be dismissed):
• shared/.../SessionStateMachine.kt:88 — Severity 8.5/10 — cannot be dismissed

[If no dismissals found]:
No previously dismissed findings found.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Pass the dismissed findings list to Stage 5 for verdict calculation.

---

## Stage 1 — Launch Specialized Review Agents (Parallel)

**IMPORTANT:** Use a SINGLE message with multiple Task tool invocations to launch all selected agents in parallel. Each agent runs as a separate subagent.

Display: "🚀 Launching [N] specialized review agents in parallel..."

### Shared Context for All Agents

Every agent prompt MUST include:
1. The full diff content (from Stage 0B)
2. The list of changed files
3. The risk score and level
4. Instruction to read `.claude/CLAUDE.md` for project conventions
5. Instruction to read FULL file content (not just diff) for context
6. Instruction to search codebase before flagging issues (avoid false positives)
7. The scope rule below
8. The search boundary rule below
9. The noise filter rule below
10. The "prove don't speculate" rule below — every finding must be verified against actual source code

**SEARCH BOUNDARY RULE — Excluded directories:**
Do NOT search in any of these directories: `**/build/**`, `**/.gradle/**`, `**/.kotlin/**`, `**/generated/**` (Room KSP and Compose Resources codegen output), `**/.idea/**`, `**/.vscode/**`, `**/docs/**`, `node_modules/`, vendor/dependency caches under `~/.gradle/caches/`, `~/.konan/`, `~/.m2/`. Search everything else in the project. If you need to understand how a library (Metro, Circuit, Room, Compose) behaves, rely on the project's own usage patterns and `.claude/CLAUDE.md` — do not read library source from the dependency cache.

**NOISE FILTER RULE — Ignore system artifacts:**
Strings matching `toolu_[a-zA-Z0-9]+` are internal system tool-call identifiers. They are NOT part of the codebase or the PR. Do NOT attempt to Read, Glob, Grep, or open any path containing `toolu_`. Do not reference, analyze, or comment on them in your review output. If you see a `toolu_` string anywhere in your context, skip it entirely — it is never a file, never a code reference, and never relevant to the review.

**SCOPE RULE — PR-only findings:**
Only flag issues in code that was **added or modified** in this PR diff — lines starting with `+` in the diff (added) or lines that were deleted/replaced (starting with `-`). Context lines (unchanged, starting with space) can be referenced only if the issue is directly caused by an adjacent change. You may READ surrounding code and the rest of the codebase for context, but every finding MUST reference a line that appears in the diff. The only exception is a HIGH or CRITICAL severity (8+) issue in a file that appears in the PR's changed file list (from Stage 0B) that is directly related to the changes — in that case, flag it but clearly label it as `[Pre-existing]` and do NOT count it as a blocker. Pre-existing issues are informational only; the developer is expected to address them in a separate PR. Cap at 2 pre-existing issues per file — if more exist, note "Additional pre-existing issues exist in this file — consider a dedicated cleanup PR" without listing them individually.

**PROVE DON'T SPECULATE RULE — verification before reporting:**

Stages 4–6-style pattern matching catches what's wrong *in* the diff. This rule catches what's wrong *about* the diff — assumptions the code makes about the rest of the system that may not hold. Apply it to every potential issue: read the actual source code to confirm or disprove it before reporting. A finding is only valid if you can cite the exact file and line that proves the problem.

When investigating multiple areas in parallel, dispatch subagents (Task tool with `subagent_type=Explore`).

Apply these five verification categories to every finding:

1. **Symbol & dependency integrity** — for every class, function, top-level val, or import added or modified in the PR:
   - Verify the target symbol actually exists by reading its source file. Don't trust the import path; open the file.
   - For `expect class`/`expect fun`/`expect val`/`expect object`: verify there is an `actual` for every active KMP target of that module (`androidMain` AND `iosMain`). Open each `actual` source file and confirm signatures match (parameter names, types, nullability, generic constraints, default values, `@OptIn` annotations).
   - For Metro bindings: verify a new injectable type is actually reachable from the graph — `@Inject` + `@ContributesBinding(AppScope::class)` on the implementation, or a `@Provides` in a `@BindingContainer @ContributesTo(AppScope::class)` container. Metro validates the graph at compile time, but a *missing contribution* (a port with no bound implementation) surfaces as a confusing compile error in `:shared` — flag it explicitly.
   - For Circuit wiring: verify a new Presenter and Layout each carry `@CircuitInject(<Feature>Screen::class, AppScope::class)` (the Presenter via its nested `@AssistedFactory`), so Metro generates the matching factories. A `Screen` with no injected Presenter/Layout renders nothing at runtime.
   - For hexagonal ports: verify a port interface (e.g. in `domain/ports/`, `db.repository`, or `:module:analytics`) has a bound implementation — a port with no binding fails the graph compile in `:shared`.
   - **Example catch:** PR adds a method to `SessionRepository` but only implements it in `SessionRoomRepository` — `FakeSessionRepository` in `:module:db:test-fixtures` no longer compiles, breaking every consuming test.

2. **Room & migration correctness** — for every `@Entity`, `@Dao`, `@Database`, or migration in the PR:
   - **Migration ↔ schema match:** if the `SoulariumDatabase` `@Database(version = N)` was bumped, the exported schema JSON under `module/db/schemas/` must be regenerated and committed, with a migration path (while the app is unreleased, `fallbackToDestructiveMigration(dropAllTables = true)` on the builder is the accepted path; after release, a real `Migration(from, to)` registered on the builder). Open both files to verify.
   - **Migration idempotence:** running a migration twice on the same DB must be safe — custom `Migration` blocks with `db.execSQL` should use `IF NOT EXISTS`/`IF EXISTS` guards on schema changes.
   - **Source-schema column references:** every column referenced in a `Migration` block must exist in the source-version schema (open the previous version's exported schema JSON to verify).
   - **Backfill completeness:** if a migration backfills data, verify it handles ALL existing-row states (nulls, empty strings, unexpected values).
   - **Foreign key indexes:** the entities use FK cascades — new `foreignKeys = [...]` declarations must have matching `@ColumnInfo(index = true)` (or `indices = [...]`) on the FK columns, or cascading writes do full-table scans. Room 2.8's drivers enforce foreign keys by default — no manual `PRAGMA foreign_keys` callback.
   - **`expect object SoulariumDatabaseConstructor`:** the database uses `@ConstructedBy(SoulariumDatabaseConstructor::class)` — its `actual`s are KSP-generated per platform. The database *builder* is NOT expect/actual: it's a platform Metro `@Provides` in `Android`/`Ios RoomBindings`. Verify KSP is configured for every target whose entities changed.

3. **Trust-boundary validation** — for every function that accepts external input (a persisted `SessionState` JSON snapshot read back from the database, DataStore preference values, image URLs loaded via Coil, share-link URLs, file content):
   - Identify all callers and verify they validate the input before use. Type-system nullability is not enough — a non-null `String` deserialized from a stored JSON snapshot can still be malformed or from an older app version.
   - For the persisted `SessionState`: verify deserialization is total and handles an older snapshot shape gracefully (no `!!` on optional fields, no swallowed `SerializationException` that produces a half-initialized object).
   - For controller-style branching on an external value (e.g., `when (event)` in `transition`): verify the `when` is exhaustive over the sealed type. A `when` over a sealed `SessionEvent`/`SessionState` should not need an `else`; if one exists, confirm it isn't masking an unhandled case.
   - For share-link / Coil image URLs: verify URLs are not built by unsanitized string concatenation of user-influenced data.
   - **Example catch:** a `@Serializable` field added to `SessionState` with no default value crashes deserialization of every session persisted before this PR.

4. **Data flow tracing** — for key data paths introduced or modified in the PR, trace the full lifecycle:
   - **Write path:** where does data enter the system? (a `SessionEvent` dispatched into `transition`, a `UiEvent` through a Presenter's `eventSink`, a DAO `@Upsert`, a DataStore write) Follow it through validation, mapping, and persistence. Verify each step handles the data type correctly.
   - **Read path:** where is the data displayed? (a Circuit Layout reading fields off the `UiState` its Presenter derives from repository Flows via `collectAsState()`/`produceState`) Verify the UI handles all possible stored values (nullable, empty list, zero, very long strings, error/loading states).
   - **State machine path:** the `transition(state, event, ctx)` function is pure and returns a `TransitionResult` carrying the next `SessionState` plus `Effect`s. Verify side effects are returned as `Effect` data and NOT executed inside `:module:game`. Verify the caller (the Presenter) actually performs every returned `Effect`.
   - **Sync / persistence path:** `SessionState` is persisted as a JSON snapshot string in a DB column. Verify every state transition that should survive process death is followed by a persistence write, and that the in-memory state and the persisted snapshot do not drift.
   - **Threading path:** Room DAOs declared `suspend` (or returning `Flow`) must be called from a coroutine context; verify Flows produced by repositories are collected on appropriate dispatchers (no `Dispatchers.Main` work in `commonMain`).

5. **Removed/changed code ripple effects** — for every public symbol, `Screen` type, Room entity, DAO, port interface method, `@Serializable` class, or `expect` declaration removed or renamed in the PR:
   - Search the entire repo (`shared/`, `module/*/`, `androidApp/`, `iosApp/`, test sources) for references to the old name. The new code may compile because the renamed symbol resolves at the call site, but indirect consumers (test fakes in `test-fixtures` modules, Swift code in `iosApp`) may still use the old name.
   - For `expect` removals: search for `actual` declarations on every target — orphaned actuals will not compile.
   - For `Screen` changes: navigation is type-safe (`navigator.goTo(SomeScreen(...))`), but a Screen whose Presenter/Layout `@CircuitInject` wiring was removed renders nothing at runtime — verify both factories still exist for every reachable Screen.
   - For port interface method removals/renames: open every implementation AND every fake (`FakeSessionRepository` in `:module:db:test-fixtures`, single-use doubles in test sources) and verify they were updated. A missing fake update breaks every consuming test.
   - For `@Serializable` field removals/renames in `:module:model`: a renamed field without `@SerialName("oldName")` breaks deserialization of every session persisted before this PR.
   - **Example catch:** PR renames a `UiEvent` subtype the Layout still references — the Layout file fails to compile only in a module-wide build, not in the Presenter-focused build the author ran.

If you can't prove a finding with concrete file/line evidence after applying these checks, downgrade its severity or omit it.

**LARGE PR CHUNKING RULE — For PRs with 500+ lines of diff:**
Instead of passing the entire diff to each agent, pass only the diff hunks relevant to that agent's specialization plus the full changed file list for context. For example, the Security agent receives diffs for `di/**`, `AndroidManifest.xml`, `analytics/**`, signing/Firebase config, and workflow files; the Data Integrity agent receives diffs for Room entities/DAOs/migrations, `module/db/schemas/**`, `@Serializable` domain models, and `expect`/`actual` declarations. Each agent still sees the complete list of changed files and can request to read any file, but the diff in their prompt is focused on their domain. This prevents agents from skimming later files in a large diff.

### Agent 1: Security Review 🔒

**Task tool config:**
- description: "Security code review"
- subagent_type: "general-purpose"
- model: "opus" (or "sonnet" in quick mode)

**Prompt focus areas (prioritize based on what actually changed in the diff — spend 80% of effort on issues directly in the diff, 20% on systemic concerns triggered by the diff context):**

Soularium is an **offline app with a minimal security surface**: no auth, no login, no tokens, no network API client. The only declared permission is `INTERNET`; the only network use is image loading via Coil and share-link URLs opened in a browser. If few security-relevant changes exist, **say so explicitly** — state "minimal security-relevant surface in this PR" rather than inventing theoretical issues. The genuinely relevant concerns are:

- **CI/CD workflow integrity**: GitHub Actions workflow changes — verify permission scopes are minimal, secrets are not exposed in logs, trigger conditions cannot be manipulated to bypass review gates, and the `ai-review-auto-approve` workflow cannot be weakened to skip human review. `crowdin-upload.yml` and `crowdin-download.yml` consume the `CROWDIN_PERSONAL_TOKEN` secret — verify it is not echoed.
- **Review process integrity**: changes to `.claude/commands/` or `.claude/rules/` review definitions — verify risk scoring is not weakened, severity thresholds are not lowered (especially the severity ≥ 7 dismissal floor), and review checklists are not stripped of critical checks.
- **Room SQL injection**: any use of `@RawQuery` or hand-built SQL — must use bound arguments, never string interpolation. Plain `@Query` statements with `:param` placeholders are safe; flag any concatenated SQL.
- **PII handling**: `scrubAnalyticsParams()` strips PII keys (name/email/phone/notes/card_id) before analytics. Verify any new analytics event passes through scrubbing, that PII does not leak into share-link URLs, and that no PII is written to logs or crash breadcrumbs.
- **Exported Android components**: `androidApp/src/main/AndroidManifest.xml` — `MainActivity` is exported (it is the launcher). Verify no other component is exported without justification, and that no new permission is added beyond `INTERNET` without a clear reason.
- **Secret / Firebase config exposure**: scan the diff for hardcoded keys or signing passwords. `local.properties`, `*.keystore`, signing configs, `google-services.json`, and `GoogleService-Info.plist` must not be committed — verify `.gitignore` covers them.
- **Share-link safety**: the shareable summary link is generated from session data as plain text — it is NOT an arbitrary URL loader. Flag any change that turns sharing into one, or that builds a share URL from unsanitized user-influenced input.
- **Coil image loading**: image URLs loaded via Coil should come from bundled/trusted content. Flag URLs constructed by unsanitized concatenation.
- **Logging hygiene**: no PII in any log or crash breadcrumb. Analytics/crash reporters are no-op until Firebase config lands — verify replacements still scrub.

**File-Type Checklists (when these paths appear in the diff, run the matching list):**

- **`.github/workflows/*.yml`** — `permissions:` block minimal (default to `contents: read`, escalate per-job only); secrets (`CROWDIN_*`, etc.) not echoed in `run:` blocks; `pull_request_target` events handled with care; the `ai-review-auto-approve` gate not weakened
- **`.claude/commands/*.md`, `.claude/rules/*.md`** — risk-score thresholds preserved; severity ≥ 7 dismissal floor preserved; required reviewer levels preserved; no critical checks stripped from agent prompts
- **Room `@Dao` / `@RawQuery`** — no string interpolation in SQL; `@Query` uses `:param` placeholders; raw queries use bound args
- **`module/analytics/src/**`, `shared/src/commonMain/kotlin/**/analytics/**`** — every event flows through `scrubAnalyticsParams()`; no PII keys (name/email/phone/notes/card_id) reach the tracker; no PII in event names or values
- **`androidApp/src/main/AndroidManifest.xml`** — only `MainActivity` exported (it is the launcher); no other `android:exported="true"` without justification; permissions limited to `INTERNET`; no `android:debuggable="true"` in the release variant
- **Share code** — sharing is plain text; not converted into an arbitrary URL loader; share URLs not built from unsanitized input
- **`local.properties`, `**/*.keystore`, signing configs, `google-services.json`, `GoogleService-Info.plist`** — must not be committed; verify `.gitignore` covers them; no hardcoded passwords in `signingConfigs { }` blocks

**Output format:**
```
## 🔒 Security Agent Review

### Critical Security Issues (BLOCKING) — Severity: 10/10
- **File:Line** — Issue description
  - Severity: 10/10
  - Risk: What attack vector this enables
  - Impact: What could happen
  - Fix: Specific code change needed

### Security Concerns (IMPORTANT) — Severity: 6-9/10
[Same format with severity scores]

### Security Suggestions — Severity: 3-5/10
[Improvement suggestions]

### Questions for Other Agents
- **To [Agent]**: Question

### Confidence
- Overall: High/Medium/Low
- Areas needing deeper analysis: [list]
```

**CODEBASE CONTEXT SEARCH:** Before flagging a pattern as an issue, use Grep to search for at least 3 other instances of the same pattern across the Kotlin source roots: `shared/` and `module/*/`. If the pattern is used consistently in 3+ other locations, it is an established project convention — do NOT flag it. If it appears only in the current PR or in fewer than 3 places, flag it.

**AUTOMATED FIX GENERATION:** For every issue with a clear fix, generate a ready-to-apply code patch. Show the exact file path and line range. Provide a before/after code block (Kotlin / Gradle Kotlin DSL / YAML / TOML / SQL as appropriate). Only generate fixes where the correct solution is unambiguous. Label each fix with its severity and the issue it addresses.

---

### Agent 2: Architecture Review 🏗️

**Task tool config:**
- description: "Architecture code review"
- subagent_type: "general-purpose"
- model: "opus" (or "sonnet" in quick mode)

**Prompt focus areas:**

`.claude/CLAUDE.md` is the authoritative architecture reference — read its Architecture, DI, and platform-abstraction sections first and review against them rather than this summary. The durable rules to enforce:

- **Module dependency direction**: `:androidApp` → `:shared` → `:module:db` → `:module:model`, with `:shared` also depending on `:module:model`, `:module:game`, and `:module:analytics` directly. Library modules never depend on `:shared` or on each other except as CLAUDE.md's module map allows. Flag any new inter-module edge not in that map, and any dependency not using the type-safe project accessors (`projects.module.model`).
- **Package-layer direction inside `:shared`**: `org.cru.soularium.domain` must not import from `data`, `ui`, or platform packages; `data` must not import from `ui`. Domain code may use platform APIs in platform `actual`s and lightweight multiplatform value types (e.g. Compose's `Locale`), but no Compose UI.
- **Hexagonal ports**: port interfaces live where CLAUDE.md places them (`domain/ports/` in `:shared`, the repository contracts in `:module:db`, `AnalyticsTracker`/`CrashReporter` in `:module:analytics`). Flag a port with no bound implementation, an implementation leaking framework types into the port signature, or `ui` code bypassing a port to talk to Room/DataStore directly.
- **Pure session state machine** (`:module:game`): `transition(state, event, ctx): TransitionResult` must be a **pure function** — no IO, no clock reads, no logging. Side effects are returned as `Effect` data and executed by the caller (the Presenter), never inside the module. Flag impurity in `transition`, a non-exhaustive `when` over the sealed state/event types, or an `Effect` executed inside `:module:game`.
- **Metro DI**: the graph is `SoulariumAppGraph` (`@DependencyGraph(AppScope::class)`), built once per platform. New bindings come from `@Inject` + `@ContributesBinding(AppScope::class)` on the implementation, or `@Provides` in a `@BindingContainer @ContributesTo(AppScope::class)` container; app-lifetime singletons carry `@SingleIn(AppScope::class)`. Flag hand-written factories, graph accessor additions on `SoulariumAppGraph` itself, or any Koin/Hilt/Dagger/Anvil annotation — DI is Metro-only.
- **Circuit UI discipline**: each screen is a `@Parcelize` `Screen` + `@AssistedInject` Presenter + stateless Layout wired by `@CircuitInject(<Feature>Screen::class, AppScope::class)`. Navigation is `navigator.goTo(...)`/`navigator.pop()` from inside Presenters. There are no ViewModels — flag any `androidx.lifecycle.ViewModel` usage.
- **`expect`/`actual` platform seams**: the current seams are `PlatformBindings`, `PlatformBackHandler`, and `SoulariumDatabaseConstructor` (in `:module:db`). The Room builder and the device-state DataStore path are NOT expect/actual — they are platform Metro `@Provides`. Every `expect` needs an `actual` for both `androidMain` and `iosMain` with matching signatures.
- **Source-set discipline**: no Android (`android.*`, platform `androidx.*`) or iOS (`platform.*`, `kotlinx.cinterop`) imports in any `commonMain`. Use `expect`/`actual` to bridge platform differences.
- **Compose recomposition stability**: large data classes held in state should be stable; flag `MutableState<List<...>>`/`MutableState<Map<...>>` (prefer immutable collections). Heavy derivation belongs in the Presenter, wrapped in `remember`/`produceState` as appropriate.
- **Coroutine/Flow scope discipline**: no `GlobalScope.launch`; no `runBlocking` outside tests; coroutine launches inside `present()` must be Compose-aware (`LaunchedEffect`, or inside an `eventSink` callback) — a bare `scope.launch { }` at the top level of `present()` re-runs on every recomposition.
- **Error handling**: game-logic errors surface via `TransitionResult.error` (`GameError` sealed interface in `:module:game`) — there is NO `Result<T>` wrapper convention. Flag thrown exceptions used for control flow or silent error swallowing (`try { ... } catch (e: Throwable) {}`).
- **Module build configuration**: cross-module Gradle conventions live in the `build-logic/` composite build's convention plugins (`soularium-kmp.module-conventions`, `metro-conventions`, etc.); module build scripts stay thin and use `gradle/libs.versions.toml` aliases. Flag config duplicated across modules that belongs in a convention plugin, or a dependency added without a catalog alias.
- **Technical debt**: created vs reduced by this PR.
- **Pattern consistency with `.claude/CLAUDE.md` conventions**.

**File-Type Checklists (when these paths appear in the diff, run the matching list):**

- **`module/game/src/**` / `module/model/src/**`** — no Android/iOS/Compose imports; `transition` is pure (no IO, no clock, no logging); side effects modeled as `Effect` data and returned, never executed; `when` over `SessionState`/`SessionEvent` is exhaustive; IDs are `@Serializable @JvmInline value class` types nested in their owner (`Session.Id`, not `SessionId`); domain models are `@Serializable`; errors flow through `GameError`/`TransitionResult.error`
- **Repository implementations (`module/db/src/**`, `shared/src/commonMain/**/data/**`)** — implementation satisfies its port contract; no Compose imports; framework types not leaked back into the port signature; row ↔ model mapping is total (no `!!` on optional columns)
- **`shared/src/commonMain/kotlin/org/cru/soularium/di/**` (Metro)** — bindings contributed via `@ContributesBinding`/`@Provides` in binding containers; `@SingleIn(AppScope::class)` on app-lifetime singletons; no new accessor properties on `SoulariumAppGraph` (use a contributed accessor interface instead); platform-specific bindings live in the platform `PlatformBindings`/`RoomBindings` actuals
- **Circuit Presenters/Layouts (`shared/src/commonMain/kotlin/**/ui/**`)** — Presenter is `@AssistedInject` with a nested `@CircuitInject @AssistedFactory` factory; `UiState` exposes `eventSink`; Layout is public, stateless, `(state, modifier)`-shaped with `@CircuitInject` on the declaration; no business logic in the Layout
- **`expect`/`actual` declaration files** — every `expect class`/`expect fun`/`expect val`/`expect object` has a corresponding `actual` for both `androidMain` and `iosMain` of its module; signatures, generic constraints, default values, and `@OptIn` annotations match exactly across the pair
- **Per-module `build.gradle.kts` and `build-logic/**`** — KMP modules apply `soularium-kmp.module-conventions` explicitly (test-fixtures modules apply `soularium-kmp.test-fixtures-conventions`); uses `gradle/libs.versions.toml` aliases (no hardcoded versions); namespace follows `org.cru.soularium`; `:androidApp` stays a pure `com.android.application` shell
- **`settings.gradle.kts`** — expected modules are `:shared`, `:androidApp`, and the `module/*` libraries per CLAUDE.md; a new module addition is a significant architectural change — flag for senior review
- **Compose `@Composable` screens** — Layouts read fields off `state` and emit intent via `state.eventSink(...)`; `modifier: Modifier = Modifier` is the LAST parameter and the first thing applied to the root composable; no business logic in the composable

**Output format:**
```
## 🏗️ Architecture Agent Review

### Critical Architecture Issues (BLOCKING) — Severity: 10/10
- **File:Line** — Issue
  - Severity: 10/10
  - Problem: What's architecturally wrong
  - Impact: Long-term consequences
  - Alternative: Better approach

### Architecture Concerns (IMPORTANT) — Severity: 6-9/10
[Same format]

### Architecture Suggestions — Severity: 3-5/10
[Better patterns and approaches]

### Technical Debt Analysis
- Debt Added: [what new debt]
- Debt Removed: [what debt fixed]
- Net Impact: Better/Worse/Neutral

### Pattern Compliance
- Follows .claude/CLAUDE.md standards: Yes/No/Partial
- Violations: [list]

### Questions for Other Agents
### Confidence
```

**CODEBASE CONTEXT SEARCH:** Before flagging a pattern as an issue, use Grep to search for at least 3 other instances of the same pattern across the Kotlin source roots: `shared/` and `module/*/`. If the pattern is used consistently in 3+ other locations, it is an established project convention — do NOT flag it. If it appears only in the current PR or in fewer than 3 places, flag it.

**AUTOMATED FIX GENERATION:** For every issue with a clear fix, generate a ready-to-apply code patch. Show the exact file path and line range. Provide a before/after code block (Kotlin / Gradle Kotlin DSL / YAML / TOML / SQL as appropriate). Only generate fixes where the correct solution is unambiguous.

---

### Agent 3: Data Integrity Review 💾

**Task tool config:**
- description: "Data integrity review"
- subagent_type: "general-purpose"
- model: "opus"

**Prompt focus areas:**

`.claude/CLAUDE.md`'s Persistence and Model sections are the authoritative reference — read them first. The durable rules to enforce:

- **Room schema correctness**: the Room stack lives in `:module:db` (`org.cru.soularium.db`). A `@Database(version = N)` bump must ship a matching exported schema JSON under `module/db/schemas/` and a migration path — while the app is unreleased, `fallbackToDestructiveMigration(dropAllTables = true)` on the builder is the accepted path; once builds ship outside the team, a real `Migration(from, to)` is required (idempotent, referencing only columns/tables that exist in the source schema). **Never edit an exported schema JSON in place** — any schema change goes through a version bump.
- **Room entity ↔ DAO consistency**: `@Entity` columns must match `@Upsert`/`@Query` projections. Nullable columns must be modeled as nullable Kotlin types, and repository row ↔ model mapping must be total (no `!!` on optional columns). FK columns need `@ColumnInfo(index = true)` to avoid full-table scans on cascading writes; Room 2.8's drivers enforce foreign keys by default. Single-item lookups are named `find*` returning `T?` (`find*Flow` for `Flow<T?>`).
- **Multiplatform Room wiring**: `SoulariumDatabase` uses `@ConstructedBy(SoulariumDatabaseConstructor::class)` — the constructor's `actual`s are KSP-generated per platform. The database *builder* is a platform Metro `@Provides` in `Android`/`Ios RoomBindings` (with `AndroidSQLiteDriver`/`BundledSQLiteDriver`). Repository impls that are Room `@Dao`s (e.g. `SessionRoomRepository`) are provided by `RoomBindings`, not `@ContributesBinding`. Flag a target whose entity set changed but whose KSP config was not updated.
- **Persisted `SessionState` snapshot**: `SessionState` (a sealed `@Serializable` hierarchy in `:module:model`, `org.cru.soularium.model.game`) is persisted as a **JSON snapshot string** (`state_snapshot_json`). Renaming or removing a serialized field, or adding a non-nullable field without a default, breaks deserialization of every session persisted before the change — treat such changes as schema changes. Flag any non-backward-compatible `@Serializable` change reachable from `SessionState`.
- **kotlinx.serialization compatibility**: renaming a property on a `@Serializable` class without `@SerialName("oldName")` breaks the wire/snapshot format; existing `@SerialName` values (e.g. on `ContactInfo` and the `SessionState` variants) pin the JSON wire format and must not change. Adding a non-nullable property without a default value crashes deserialization of older JSON. Removing a property silently drops data.
- **DataStore device flags**: device flags (intro seen, ToS agreed) persist via DataStore Preferences in `:shared`'s data layer — the common `preferenceDataStoreAt(producePath)` helper builds the store and each platform's `@Provides providesDeviceStateDataStore` supplies the path. Verify preference keys are stable, reads handle a missing key with a sensible default, and writes are not racing. The app language is NOT stored here — it is the platform per-app language setting read through `LanguageRepository`.
- **Threading and structured concurrency**: Room DAOs declared `suspend` must be called from a coroutine context; DAO `Flow` results must be collected on appropriate dispatchers. Flag `runBlocking` in `commonMain` production code. Verify `Dispatchers.Main` is not used in `commonMain` (use `Dispatchers.Default`/`Dispatchers.IO`).
- **Expect/actual contract drift**: an `expect` declaration must have an `actual` for both `androidMain` and `iosMain` of its module. Adding/removing parameters, nullability, return types, or visibility on the `expect` must mirror on every `actual`. Verify generic constraints, default values, and `@OptIn` annotations are consistent.
- **Singleton scoping of stateful objects**: the Room database and DataStore are `@SingleIn(AppScope::class)`-scoped in the Metro graph — flag any provider returning a new instance per call for objects that hold connection/file handles.
- **ID value classes**: ids are `@Serializable @JvmInline value class` types over UUID strings, **nested in their owner** (`Session.Id`, `Conversation.Id`, `CardPick.Id`). Flag a raw `String` used where a typed ID is expected, a top-level `SessionId`-style class, or a value class whose backing type changed.
- **Kotlinx-datetime correctness**: prefer `Instant` for timestamps, `LocalDate` for date-only fields. Avoid `Clock.System.now()` in production code that needs to be testable — pass time in via the transition context / an injected `Clock` rather than reading it inside `transition`.

**File-Type Checklists (when these paths appear in the diff, run the matching list):**

- **Room `@Entity` classes (`module/db/src/commonMain/**/room/entity/**`)** — every column declared in the entity exists in the DAO `@Upsert`/`@Query` projections; nullable columns map to nullable Kotlin types; primary key declared (`@PrimaryKey`); FK declarations accompanied by `@ColumnInfo(index = true)` on the FK columns; entity owns the entity ↔ model mapping (`toModel()` / companion factory) — Room types never leak out of `:module:db`
- **Room `@Dao` interfaces (`module/db/src/commonMain/**/room/dao/**`)** — `@Query` strings reference real columns and tables; bind parameters use `:name` (not concatenation); `@RawQuery` (if any) uses bound args; `Flow<T>` return types match observability needs; `suspend` for one-shot reads/writes; `@Upsert` for inserts that may collide
- **Room `@Database` (`SoulariumDatabase`)** — `version` bumped when entities change; new entities listed in `entities = [...]`; matching schema JSON committed under `module/db/schemas/`; `@ConstructedBy(SoulariumDatabaseConstructor::class)` present
- **Room migrations** — idempotent (re-running on the same DB is safe); references columns/tables that exist in the source schema; produces a destination schema matching the new entity declarations; backfill SQL handles nulls and unexpected values; migration registered on the database builder (or the documented destructive-migration fallback applies)
- **Repository implementations (`module/db/src/**`, `shared/src/commonMain/**/data/**`)** — implements its port contract; every public method has a corresponding implementation in `FakeSessionRepository` (`:module:db:test-fixtures`) or the relevant test double; behavior is testable
- **`@Serializable` classes in `module/model/src/**`** — renamed properties carry `@SerialName("oldName")` to preserve snapshot compat; existing `@SerialName` values unchanged; new non-nullable fields have a default value to avoid breaking older persisted `SessionState` JSON; no removed required fields without a migration plan
- **`expect`/`actual` declaration files** — every `expect` decl has an `actual` for both `androidMain` and `iosMain` of its module; signatures, nullability, and generic constraints match exactly
- **DataStore device-flag code (`shared/src/commonMain/**/data/**`)** — preference keys stable; reads default sensibly on a missing key; each platform's `@Provides providesDeviceStateDataStore` supplies the path

**Output format:**
```
## 💾 Data Integrity Agent Review

### Critical Data Issues (BLOCKING) — Severity: 10/10
- **File:Line** — Issue
  - Severity: 10/10
  - Problem: Data integrity concern
  - Impact: What could go wrong (schema drift, deserialization crash on old snapshots, expect/actual divergence, etc.)
  - Fix: Required action

### Data Concerns (IMPORTANT) — Severity: 6-9/10
[Same format]

### Data Suggestions — Severity: 3-5/10

### Room / Serialization Specific Checks
- Room migration coverage: [version bump without migration, schema export drift]
- Entity ↔ DAO consistency: [nullability mismatches, missing FK indexes]
- Persisted SessionState compat: [non-backward-compatible @Serializable change]
- kotlinx.serialization compat: [renamed properties without `@SerialName`, removed required fields]
- expect/actual contract: [missing `actual` on a target, signature drift]
- Repository ↔ test-fake parity: [port change without in-memory fake update]

### Questions for Other Agents
### Confidence
```

**CODEBASE CONTEXT SEARCH:** Before flagging a pattern as an issue, use Grep to search for at least 3 other instances of the same pattern across the Kotlin source roots: `shared/` and `module/*/`. If the pattern is used consistently in 3+ other locations, it is an established project convention — do NOT flag it. If it appears only in the current PR or in fewer than 3 places, flag it.

**AUTOMATED FIX GENERATION:** For every issue with a clear fix, generate a ready-to-apply code patch. Show the exact file path and line range. Provide a before/after code block (Kotlin / Gradle Kotlin DSL / YAML / TOML / SQL as appropriate). Only generate fixes where the correct solution is unambiguous.

---

### Agent 4: Testing & Quality Review 🧪

**Task tool config:**
- description: "Testing and quality review"
- subagent_type: "general-purpose"
- model: "opus" (or "sonnet" in quick mode)

**Prompt focus areas:**

`.claude/CLAUDE.md`'s Testing section and `.claude/skills/pr-review/SKILL.md`'s Testing checklist are the authoritative reference — read them first. The durable rules to enforce:

- **Test coverage**: every new public class, function, Presenter, repository, and the pure `transition` state-machine logic should have a test in its module's `commonTest`. Private functions are tested indirectly through the public interface. Do not flag missing tests for simple data classes, delegations, or generated code (Room KSP, Compose Resources).
- **Test framework**: tests use `kotlin.test` (`@Test`, `@BeforeTest`, `@AfterTest`), Kotest assertions, Turbine for `Flow` testing, and `kotlinx-coroutines-test` (`runTest`, `TestDispatcher`, `advanceUntilIdle`). Unit tests live in each module's `commonTest`; Paparazzi screenshot tests live in `androidHostTest`. There is no on-device instrumentation — everything runs host-side (Robolectric for the Android-host variant).
- **`@RunOnAndroidWith` discipline**: Presenter tests and Compose-UI interaction tests (`runComposeUiTest`, imported from `androidx.compose.ui.test.v2`) are annotated `@RunOnAndroidWith(AndroidJUnit4::class)` so the Android-host variant runs under Robolectric; pure domain tests (no Compose) are unannotated. Flag the deprecated v1 `runComposeUiTest` import as a must-fix.
- **Pure state-machine testing**: `transition(state, event, ctx)` is a pure function — it should have direct input→output tests covering every `SessionState` × `SessionEvent` combination that matters, and assertions on the returned `Effect`s. Flag a new state/event/effect path with no test.
- **Presenter testing**: Presenters are exercised via Circuit's `circuit-test` library (`FakeNavigator`, `presenter.test { awaitItem().eventSink(...) }`) inside `runTest`. Test names follow the structured form `UiEvent - <Event> - <behavior>` / `UiState - <field> - <behavior>`. Flag a Presenter whose event handling or state derivation is untested.
- **Test fakes**: reusable fakes live in a sibling `test-fixtures` module — `:module:db:test-fixtures` provides `FakeSessionRepository` (in-memory `SessionRepository` with seeding, interaction recording, fault injection). Single-use doubles stay as plain private classes in the test sources. No `mockk` — flag a test that mocks a port when a fake would be clearer, and a new port method with no matching fake update.
- **Repository / Room tests**: follow the abstract-contract pattern — a persistence-agnostic contract test plus a thin Room subclass wiring in the real database via `expect fun buildInMemorySoulariumDatabase()`, running on both Android host (Robolectric) and iOS.
- **Paparazzi screenshot tests**: new/changed `<Feature>Layout` composables need a matching `<Feature>LayoutPaparazziTest` in `shared/src/androidHostTest/`, extending `BasePaparazziTest` with the device × `nightMode` `@TestParameter` matrix, rendering the stateless Layout with a hand-built `UiState`. Snapshots are recorded via the record-snapshots workflow, not locally.
- **Coroutine test correctness**: use `runTest { ... }` (not `runBlocking`), inject a `TestDispatcher` for time control, and avoid `Thread.sleep`/`delay()` for synchronization (use `advanceTimeBy`/`advanceUntilIdle` or Turbine's `awaitItem`).
- **Turbine usage**: `Flow` tests use Turbine (`flow.test { awaitItem(); awaitComplete() }` or `cancelAndIgnoreRemainingEvents()`). Flag tests that collect Flows manually with timeouts.
- **Edge cases**: nullability boundaries, empty lists, single-element lists, very long strings, leading/trailing whitespace, an older persisted `SessionState` snapshot, process-death/restore, and cancellation.
- **Error path testing**: not just happy paths — test failure modes (`TransitionResult.error`/`GameError` paths, Room constraint failures, deserialization errors on old snapshots, `FakeSessionRepository` fault injection).
- **Debug output left in production code**: `println`, `Log.*` (Android), debug-level logging of sensitive data. Allowed in tests, not in production sources.
- **Code smell patterns**: broad catches (`catch (e: Throwable)`/`catch (e: Exception)` without rethrow), hardcoded magic numbers/strings that should be `const val`, `Thread.sleep` in production code, empty `catch` blocks, `!!` non-null assertions on values not proven non-null at the call site.
- **ktlint compliance**: code should pass `./gradlew ktlintCheck` (run from the repo root) with the project's `android_studio` code style. Max line length **120**. `@Composable` functions are exempt from function-naming rules; non-composable functions follow camelCase. Test functions use backtick-quoted descriptive names.

**File-Type Checklists (when these paths appear in the diff, run the matching list):**

- **`**/commonTest/**/*Test.kt`** — uses `kotlin.test` (`@Test`, `@BeforeTest`, `@AfterTest`) and Kotest assertions; coroutine tests use `runTest { }`, NOT `runBlocking`; `Flow` assertions use Turbine's `flow.test { ... }`; Compose-touching tests carry `@RunOnAndroidWith(AndroidJUnit4::class)`; descriptive backtick-quoted test names
- **Pure state-machine tests (`transition`, `SessionState`, `SessionEvent`, `Effect`)** — direct input→output assertions; covers the meaningful `state × event` matrix; asserts on the returned `Effect`s; no fakes needed (the function is pure)
- **Presenter test files** — uses Circuit's `circuit-test` (`FakeNavigator`, `presenter.test { ... }`) inside `runTest`; annotated `@RunOnAndroidWith(AndroidJUnit4::class)`; test names follow `UiEvent - <Event> - <behavior>` / `UiState - <field> - <behavior>`; covers loading/error/populated states
- **Repository test files** — follows the abstract-contract pattern (persistence-agnostic contract test + Room subclass); covers persistence round-trips and edge cases
- **Test fakes** — reusable fakes live in a `test-fixtures` module (`:module:db:test-fixtures`); single-use doubles stay private in the test sources; cover every port method (no missing fake for a new port method); behave like the real implementation for the tested contract
- **Paparazzi tests (`shared/src/androidHostTest/**`)** — extends `BasePaparazziTest`; device × `nightMode` matrix; renders the stateless Layout with a hand-built `UiState` (never constructs the Presenter); snapshots recorded via the record-snapshots workflow
- **Coverage scope** — every new public class/function/Presenter/state-machine path has a test in `commonTest`; skip simple data classes, delegations, and generated code (Room KSP, Compose Resources)

**Output format:**
```
## 🧪 Testing & Quality Agent Review

### Critical Testing Gaps (BLOCKING) — Severity: 10/10
- **File:Line** — Gap
  - Severity: 10/10
  - Missing: What's not tested
  - Risk: Why it's critical
  - Required: What tests to add (with skeleton)

### Testing Concerns (IMPORTANT) — Severity: 6-9/10
[Same format]

### Code Quality Issues — Severity: varies
- Debug output left in: [file:line list]
- Unused variables/imports: [list]
- ktlint violations: [list]
- Broad catches / swallowed errors: [list]
- `!!` non-null assertions on values not proven non-null: [list]

### Testing Suggestions — Severity: 3-5/10

### Coverage Assessment
- New code tested: Yes/Partial/No
- Edge cases covered: [list]
- Error handling tested: Yes/Partial/No
- Missing critical tests: [list with skeletons]

### Questions for Other Agents
### Confidence
```

**CODEBASE CONTEXT SEARCH:** Before flagging a pattern as an issue, use Grep to search for at least 3 other instances of the same pattern across the Kotlin source roots: `shared/` and `module/*/`. If the pattern is used consistently in 3+ other locations, it is an established project convention — do NOT flag it. If it appears only in the current PR or in fewer than 3 places, flag it.

**AUTOMATED FIX GENERATION:** For every issue with a clear fix, generate a ready-to-apply code patch. Show the exact file path and line range. Provide a before/after code block (Kotlin / Gradle Kotlin DSL / YAML / TOML / SQL as appropriate). Only generate fixes where the correct solution is unambiguous.

---

### Agent 5: UX Review 👤

**Task tool config:**
- description: "UX and accessibility review"
- subagent_type: "general-purpose"
- model: "opus"

**Prompt focus areas:**

`.claude/rules/design_system_rules.md` and CLAUDE.md's UI-layer section are the authoritative reference — read them first. The durable rules to enforce:

- **Circuit composition discipline**: each screen is a Presenter + Layout pair. The Layout is a **public, stateless** `@Composable fun <Feature>Layout(state: <Feature>Presenter.UiState, modifier: Modifier = Modifier)` — it reads fields off `state` and emits intent via `state.eventSink(...)`. No business logic in the Layout; no Presenter or repository passed to a Layout directly.
- **Modifier ordering and forwarding**: `modifier` should be the first thing applied to the root composable (so callers can layer on size/padding/clickable). Public composables must accept and forward `modifier: Modifier = Modifier` — and it must be the LAST parameter.
- **State hoisting**: stateful UI that needs to be testable or restored should be derived in the Presenter's `present()` and exposed via `UiState`, not held in a Layout-local `remember { mutableStateOf(...) }`. Layout-local `remember` is OK for transient view-only state (animation progress, a text-field draft, a visibility toggle).
- **Material3 token usage**: colors, typography, and shapes come from `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, `MaterialTheme.shapes.*` — see `.claude/rules/design_system_rules.md`. Flag hardcoded `Color(0xFF...)`, hex strings, or `sp`/`dp` literals where a theme token exists. Never use Material2 imports in `commonMain`.
- **Theme consumption**: the app theme is applied once at the app root; feature screens should not call `MaterialTheme(...)` themselves. Flag duplicate theme application.
- **Dark mode**: any custom color must work in both light and dark themes — use `MaterialTheme.colorScheme.*` rather than hard-coded `Color`. Do not branch on dark-mode state for color decisions.
- **Accessibility (`Modifier.semantics`)**: every `Image`, `Icon`, and clickable surface needs a `contentDescription` (or `null` with a clear justification for purely decorative elements). Touch targets must be ≥ 48dp. Custom interactive composables should use `Modifier.semantics { role = Role.Button }` or equivalent.
- **Navigation**: navigation uses Circuit — screens are `@Parcelize` `Screen` types (in `ui/nav/Screens.kt` or co-located with their feature package), and cross-screen navigation is `navigator.goTo(SomeScreen(...))` / `navigator.pop()` from inside a Presenter. Flag a Screen with no `@CircuitInject`-wired Presenter/Layout pair, or navigation calls made from a Layout.
- **Back handling**: `PlatformBackHandler` is `BackHandler` on Android and a no-op on iOS — verify back behavior degrades gracefully on iOS.
- **Keyboard navigation and focus**: forms should chain `Modifier.focusRequester` correctly; `KeyboardOptions`/`KeyboardActions` (`imeAction = ImeAction.Next` / `Done`) should match the form flow.
- **Loading/error/empty states**: every screen that loads data needs three distinct states. Flag a screen that shows no loading indicator while loading, or no rendered path for a persistence failure.
- **Recomposition cost**: heavy work (sorting, filtering large lists) must not run in a composable body — wrap in `remember(key)` or move it into the Presenter. Flag `LazyColumn` items without a stable `key` when the list can reorder.
- **Resource and string usage**: user-visible strings should come from Compose Multiplatform resources (`stringResource`) under `**/composeResources/values/strings.xml`, not be inlined as Kotlin literals. Image resources go through `painterResource`.
- **No platform-specific imports in commonMain UI**: `androidx.compose.ui.viewinterop.AndroidView` is only available in `androidMain`. Use `expect/actual` when wrapping a platform widget.
- **Material icons**: prefer `androidx.compose.material.icons.Icons.*` from `compose-material-icons-extended`. Avoid hand-rolling `ImageVector` paths.

**File-Type Checklists (when these paths appear in the diff, run the matching list):**

- **Circuit Layouts (`shared/src/commonMain/kotlin/**/ui/**`)** — public, stateless, `(state: <Feature>Presenter.UiState, modifier: Modifier = Modifier)` with `modifier` LAST; `@CircuitInject` on the declaration; `modifier` applied first to the root; uses `MaterialTheme.*` tokens (not hardcoded `Color(0xFF...)`); user-visible strings via `stringResource(...)`; loading/error/empty states distinguished; `LazyColumn`/`LazyRow` items have stable `key`s when the list can reorder
- **Theme files** — additions to the color/typography/shape scheme are exposed via `MaterialTheme.*` (no ad-hoc top-level vals); dark/light variants both defined for any new color
- **Navigation (`Screen` declarations)** — each `Screen` is a `@Parcelize` `data object`/`data class`; a Presenter and Layout are `@CircuitInject`-wired to it; screen arguments travel as `Screen` constructor properties (reaching the Presenter via `@Assisted`)
- **Compose resource files (`**/composeResources/values/strings.xml`, `**/composeResources/drawable/*`)** — every user-visible string the diff introduces references a resource (not a Kotlin literal); image resources use `painterResource(Res.drawable.*)`; resource keys follow `feature_section_purpose` naming
- **Material icon imports** — pulled from `androidx.compose.material.icons.Icons.*`; avoid hand-rolled `ImageVector` paths; `Icon` composables include `contentDescription` (or `null` with a clearly stated reason)
- **Accessibility surface** — every interactive composable has a `Modifier.semantics { }` annotation OR uses a built-in role-providing primitive (`Button`, `IconButton`, `Switch`, `Checkbox`); image/icon composables include `contentDescription`; touch targets are ≥ 48dp (`Modifier.minimumInteractiveComponentSize()` or larger)

**Output format:**
```
## 👤 UX Agent Review

### Critical UX Issues (BLOCKING) — Severity: 10/10
- **File:Line** — Issue
  - Severity: 10/10
  - Problem: UX concern
  - User Impact: How it affects users
  - Fix: Required action

### UX Concerns (IMPORTANT) — Severity: 6-9/10
[Same format]

### Accessibility Issues
- Missing `Modifier.semantics` / `contentDescription`: [file:line list]
- Touch targets < 48dp: [issues]
- Keyboard / focus traversal problems: [issues]
- Screen reader (TalkBack / VoiceOver) concerns: [concerns]

### Design System Compliance
- Material3 token usage (`MaterialTheme.colorScheme.*`, `.typography.*`, `.shapes.*`) consistent: Yes/No
- Theme applied at the right scope (no duplicate `MaterialTheme` calls): Yes/No
- Hardcoded `Color(0xFF...)`/`sp`/`dp` values vs theme tokens: [list]
- Dark mode support via theme (no hardcoded light-mode-only colors): Yes/No/N/A

### UX Suggestions — Severity: 3-5/10
### Questions for Other Agents
### Confidence
```

**CODEBASE CONTEXT SEARCH:** Before flagging a pattern as an issue, use Grep to search for at least 3 other instances of the same pattern across the Kotlin source roots: `shared/` and `module/*/`. If the pattern is used consistently in 3+ other locations, it is an established project convention — do NOT flag it. If it appears only in the current PR or in fewer than 3 places, flag it.

**AUTOMATED FIX GENERATION:** For every issue with a clear fix, generate a ready-to-apply code patch. Show the exact file path and line range. Provide a before/after code block (Kotlin / Gradle Kotlin DSL / YAML / TOML / SQL as appropriate). Only generate fixes where the correct solution is unambiguous.

---

### Agent 6: Soularium Standards Compliance Review 📋

**Task tool config:**
- description: "Soularium standards compliance review"
- subagent_type: "general-purpose"
- model: "opus" (or "sonnet" in quick mode)

**Prompt focus areas:**

Read `.claude/CLAUDE.md` AND `.claude/skills/pr-review/SKILL.md` thoroughly — the pr-review skill carries the detailed, up-to-date per-category checklist (architecture & layering, domain models, persistence, Circuit UI, Metro DI, expect/actual, build files, resources, testing, code style). Apply that checklist rather than re-deriving standards from memory; this section only summarizes the headline standards:

**Architecture Standards (module graph + package layering + state machine):**
- [ ] Module dependency direction matches CLAUDE.md's module map (`:androidApp` → `:shared` → `:module:db` → `:module:model`; `:module:game`/`:module:analytics` per the map); library modules never depend on `:shared`
- [ ] `org.cru.soularium.domain` does not import from `data`/`ui`; `data` does not import from `ui`
- [ ] `transition(state, event, ctx)` (`:module:game`) is pure — no IO, no clock reads, no logging; side effects modeled as `Effect` data and returned
- [ ] `SessionState`/`SessionEvent` are sealed; `when` over them is exhaustive
- [ ] Game errors flow through `TransitionResult.error` / the `GameError` sealed interface (no `Result<T>` wrapper)

**DI Standards (Metro):**
- [ ] New impl classes use `@Inject` + `@ContributesBinding(AppScope::class)`; non-constructor-injectable types go through `@Provides` in a `@BindingContainer @ContributesTo(AppScope::class)` container
- [ ] App-lifetime singletons carry `@SingleIn(AppScope::class)`
- [ ] Platform-specific bindings live in the platform `PlatformBindings`/`RoomBindings` actuals
- [ ] No new accessor properties on `SoulariumAppGraph` (use a contributed accessor interface + `asContribution<>()`)
- [ ] No Koin, Hilt, Dagger, or Anvil annotations — DI is Metro-only

**Build / Module Standards:**
- [ ] Cross-module conventions live in `build-logic/` convention plugins; KMP modules apply `soularium-kmp.module-conventions` explicitly (`soularium-kmp.test-fixtures-conventions` for test-fixtures modules)
- [ ] `:androidApp` stays a pure `com.android.application` shell (no KMP plugin)
- [ ] New dependencies are added via `gradle/libs.versions.toml` aliases, not hardcoded coordinates
- [ ] Build commands run from the repo root (`./gradlew ...`)

**KMP Source-Set Standards:**
- [ ] `commonMain` contains no Android- or iOS-specific imports
- [ ] Every `expect` declaration has an `actual` for both `androidMain` and `iosMain` of its module
- [ ] Cross-package imports respect the layering

**Data Standards:**
- [ ] Room `@Database` version bumps ship a matching exported schema JSON under `module/db/schemas/` and a migration path; existing schema JSONs are never edited in place
- [ ] `@Serializable` changes in `:module:model` are backward-compatible with the persisted `SessionState` snapshot (`@SerialName` on renames, defaults on new fields; existing `@SerialName` values unchanged)
- [ ] Repository implementations satisfy their port contracts; test fakes (`FakeSessionRepository` et al.) are kept in sync

**UI Standards (Circuit):**
- [ ] Screens are `@Parcelize` `Screen` types; Presenter + Layout pairs are wired via `@CircuitInject(<Feature>Screen::class, AppScope::class)` — no hand-written factories
- [ ] Presenters are `@AssistedInject` classes implementing `Presenter<UiState>`, with `UiState : CircuitUiState` exposing `eventSink` and `UiEvent : CircuitUiEvent`
- [ ] Layouts are public, stateless `(state, modifier)` composables; `modifier` LAST, applied first on the root
- [ ] Material3 tokens used per `.claude/rules/design_system_rules.md`; user-visible strings via `stringResource(Res.string.*)`

**Testing Standards:**
- [ ] `kotlin.test` + Kotest assertions as the test stack; unit tests in each module's `commonTest`, Paparazzi in `androidHostTest`
- [ ] Presenter/Compose-touching tests carry `@RunOnAndroidWith(AndroidJUnit4::class)`; pure domain tests are unannotated
- [ ] Coroutine tests use `runTest`, not `runBlocking`; `Flow` assertions use Turbine; Presenter tests use `circuit-test`
- [ ] Reusable fakes live in `test-fixtures` modules; single-use doubles stay private in test sources; no `mockk`
- [ ] Test names are backtick-quoted; Presenter tests follow `UiEvent - <Event> - <behavior>` / `UiState - <field> - <behavior>`

**Code Quality Standards:**
- [ ] Passes `./gradlew ktlintCheck` (run from the repo root) with the `android_studio` code style
- [ ] Max line length **120**
- [ ] `@Composable` functions are exempt from function-naming rules; non-composable functions use camelCase
- [ ] Trailing commas follow the pr-review skill's convention (only touch commas on lines you're already modifying)
- [ ] No debug output left in production code (`println`, `Log.*`, logging of sensitive data)
- [ ] No `TODO` without an issue reference in the comment
- [ ] No platform-specific imports leaking into `commonMain`
- [ ] New `CompositionLocal`s are added to `compose_allowed_composition_locals` in a scoped `.editorconfig`

**Package & Naming Standards:**
- [ ] Package follows `org.cru.soularium.*` (`org.cru.soularium.app` reserved for `:androidApp`)
- [ ] Module namespace matches the package

**Output format:**
```
## 📋 Soularium Standards Compliance Review

### Standards Violations (BLOCKING) — Severity: 8-10/10
- **File:Line** — Violation
  - Severity: [8-10]/10
  - Standard: What standard is violated
  - Issue: What's wrong
  - Fix: How to fix

### Standards Concerns (IMPORTANT) — Severity: 5-7/10
[Same format]

### Standards Checklist Results
**Architecture (module graph + layering + state machine)**: ✅/⚠️/❌
**DI (Metro)**: ✅/⚠️/❌
**Build / Modules**: ✅/⚠️/❌
**KMP Source-Sets**: ✅/⚠️/❌
**Data**: ✅/⚠️/❌ (or N/A)
**UI**: ✅/⚠️/❌ (or N/A)
**Testing**: ✅/⚠️/❌
**Code Quality**: ✅/⚠️/❌
**Package & Naming**: ✅/⚠️/❌

### Pattern Deviations
[List deviations from .claude/CLAUDE.md patterns]

### Questions for Other Agents
### Confidence
```

**CODEBASE CONTEXT SEARCH:** Before flagging a pattern as an issue, use Grep to search for at least 3 other instances of the same pattern in `shared/` and `module/*/` (Kotlin source roots). If the pattern is used consistently in 3+ other locations, it is an established project convention — do NOT flag it. If it appears only in the current PR or in fewer than 3 places, flag it.

**AUTOMATED FIX GENERATION:** For every issue with a clear fix, generate a ready-to-apply code patch. Show the exact file path and line range. Provide a before/after code block (Kotlin / Gradle Kotlin DSL / YAML / TOML / SQL as appropriate). Only generate fixes where the correct solution is unambiguous.

---

After launching all selected agents, display:

```
✅ All [N] agents launched in parallel
⏳ Waiting for agents to complete their reviews...
```

---

## Stage 1B — Dependency Impact Analysis (Parallel)

**IMPORTANT:** Launch this as an additional Task tool invocation in the **same message** as the Stage 1 agent launches. This ensures it runs truly in parallel with the review agents. Do NOT try to run this in the main context "while agents are running" — foreground Task calls block until completion.

Analyze dependency impact using KMP/Kotlin-specific patterns. For each changed file, identify the dependents that may be affected. The module graph (per CLAUDE.md): `:androidApp` → `:shared` → `:module:db` → `:module:model`, with `:shared` also depending on `:module:model`, `:module:game`, and `:module:analytics`; `:module:game` depends only on `:module:model`; `:module:db:test-fixtures` sits beside `:module:db` for shared test doubles.

**Module-level Gradle dependents:** For each changed module's `build.gradle.kts`, identify the downstream modules per the graph above. `api(...)` dependencies propagate transitively to dependents; `implementation(...)` does not. A change in `:module:model` can ripple through every other module.

**Public symbol consumers (Kotlin):** For each public class, function, or constant changed or removed in any source set, Grep across `shared/`, `module/*/`, and `androidApp/` for import statements (`import org.cru.soularium.<...>`) and direct usages.

**`expect`/`actual` consumers:** For each changed `expect` declaration, list every `actual` (one per active target) and every consumer of the declaration. Removing or renaming an `expect` breaks every consumer; changing its signature breaks every `actual`.

**Port interface consumers:** For each changed port interface (`DeviceStateRepository` in `domain/ports/`, `SessionRepository` in `:module:db`, `AnalyticsTracker`/`CrashReporter` in `:module:analytics`), search for its implementations, its Metro binding (`@ContributesBinding` or a `@Provides` in a binding container), its Presenter consumers, and the fakes (`FakeSessionRepository` in `:module:db:test-fixtures`, doubles in test sources). Adding a method without a default implementation breaks every implementation and every fake.

**Metro binding consumers:** For each changed `@ContributesBinding`/`@Provides` binding, identify the injection sites (constructor injection in Presenters/impls, contributed accessor interfaces). Metro validates the graph at compile time — a removed binding fails the `:shared` compile; verify the fix updates all consumers rather than working around the graph.

**Circuit screen consumers:** For each changed `Screen` type, search for `navigator.goTo(...)` call sites and the `@CircuitInject` annotations binding its Presenter/Layout. Removing the wiring leaves the screen unreachable or blank at runtime.

**Room entity/DAO consumers:** For each changed `@Entity`, list the DAOs that select or upsert it and the repository methods that expose it. Schema-breaking changes require a version bump plus a regenerated schema JSON under `module/db/schemas/`.

**`@Serializable` consumers:** For each changed `@Serializable` class, identify call sites that serialize/deserialize it (`Json.encodeToString`/`decodeFromString`, the persisted `SessionState` snapshot column, share-link generation). Renaming a property without `@SerialName` breaks the wire format and any persisted JSON.

For high-impact files (10+ dependents) flag as critical. Display:

```
📦 DEPENDENCY IMPACT ANALYSIS

🚨 CRITICAL IMPACT: module/db/.../repository/SessionRepository.kt — [N] dependents (Room impl + Presenters + FakeSessionRepository)
⚠️  HIGH IMPACT: module/model/.../game/SessionState.kt — [N] dependents (state machine + persisted snapshot)
📊 MEDIUM IMPACT: shared/.../ui/nav/Screens.kt — [N] navigator.goTo() call sites

Breaking Changes:
[List any removed public symbols, renamed expect declarations without actual updates, removed Screen wiring, changed port methods without fake updates, or non-backward-compatible @Serializable changes]
```

---

## Stage 2 — Collect Agent Reports

Wait for all agents to complete and display progress:

```
Agent Reviews Complete:
✅ 🔒 Security Agent — Found [X] critical, [Y] concerns
✅ 🏗️ Architecture Agent — Found [X] critical, [Y] concerns
✅ 💾 Data Integrity Agent — Found [X] critical, [Y] concerns
✅ 🧪 Testing Agent — Found [X] critical, [Y] concerns
✅ 👤 UX Agent — Found [X] critical, [Y] concerns
✅ 📋 Standards Agent — Found [X] violations, [Y] concerns
```

Parse each agent's output and extract:
- Critical issues with severity scores
- Important concerns with severity scores
- Suggestions
- Questions for other agents
- Confidence level

---

## Stage 2A — Coverage Gap Analysis (Standard & Deep Mode)

**Skip this stage if the review mode is `quick`.**

After collecting all agent reports, analyze which changed files received adequate review coverage and which were overlooked. This replaces self-verification (which suffers from confirmation bias — the same agent misses the same things twice).

### Step 1: Map findings to files

Parse every finding from all agent reports and extract the file path. Build a coverage map:

For each file in the changed file list (from Stage 0B):
- Count how many total findings reference that file (across all agents)
- Track which agents produced findings for that file

### Step 2: Categorize coverage

- **Well-covered**: 2+ agents produced findings for this file
- **Under-covered**: Exactly 1 agent produced findings for this file
- **Uncovered**: 0 agents produced any findings for this file

### Step 3: Determine which files need gap review

- **Standard mode**: Only uncovered files (0 findings) get gap review
- **Deep mode**: Both uncovered AND under-covered files get gap review

Exclude from gap review:
- Files that are test-only (`**/commonTest/**`) with < 20 changed lines — these are low-risk
- Files that are documentation-only (`*.md`, `*.txt`) — these are low-risk
- Files where the only changes are whitespace, comments, or import reordering

### Step 4: Display coverage map

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🗺️ COVERAGE GAP ANALYSIS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Files with good coverage:
  ✅ shared/.../session/SessionStateMachine.kt — 4 findings (Architecture, Data Integrity, Testing, Standards)
  ✅ shared/.../ui/conversation/ConversationPresenter.kt — 3 findings (Architecture, Testing, Standards)

Files with thin coverage:
  ⚠️ shared/.../commonTest/ConversationPresenterTest.kt — 1 finding (Testing only)

Files with NO coverage:
  ❌ shared/.../db/ConversationDao.kt — 0 findings
  ❌ module/db/schemas/org.cru.soularium.db.room.SoulariumDatabase/2.json — 0 findings

[If gap review needed]:
🔍 [N] file(s) need focused gap review — launching fresh agents...

[If no gaps]:
✅ All changed files received adequate review coverage.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Stage 2B — Focused Gap Review (Standard & Deep Mode)

**Skip this stage if no files need gap review (from Stage 2A) or if the review mode is `quick`.**

For each file (or group of files) needing gap review, launch a **fresh** general-purpose agent. These agents use fresh context with NO knowledge of previous findings — this prevents the confirmation bias that makes self-verification ineffective.

**Design principles (same proven pattern used by Stage 8 fix-iteration):**
- **Fresh agents** — NOT the same agents from Stage 1, and NOT resuming previous agents
- **No previous findings shared** — the agent reviews with completely fresh eyes
- **Narrow scope** — each agent reviews 1-3 files max for thorough line-by-line analysis
- **Cap at 5 gap-review agents** — if more than 5 file groups need review, combine smaller files together

### File grouping

If more than 5 files need gap review, group them by relatedness:
- Group an entity + its DAO + its migration + its schema JSON together
- Group a screen's Layout + its Presenter together
- Group a port interface + its implementation + its in-memory fake together
- Otherwise group by directory

### Agent prompt

Launch all gap-review agents in parallel using a SINGLE message with multiple Task tool invocations.

**Task tool config per gap agent:**
- description: "Gap review: [file names]"
- subagent_type: "general-purpose"
- model: "opus" (standard and deep modes)

**Prompt:**

```
You are a fresh code reviewer performing a focused, exhaustive review of specific files in a PR.

PROJECT CONTEXT:
Read .claude/CLAUDE.md to understand this project's coding standards and conventions.

FILES TO REVIEW (your PRIMARY focus — review every changed line):
[Paste ONLY the diff hunks for the assigned file(s)]

FULL FILE CONTENT (for surrounding context):
Read each file listed above in full using the Read tool.

FULL PR CONTEXT (for understanding how these files relate to the broader change):
Changed files in this PR: [list all changed files from Stage 0B]
PR title/description: [from Stage 0B]

MISSION: Perform an exhaustive, line-by-line review of the assigned files' changes. You are reviewing these files because they received insufficient coverage in the initial review pass. Be thorough — check every changed line for:

1. **Correctness** — Does the code do what it's supposed to? Logic errors, off-by-one, null handling
2. **Security** — SQL injection (Room @RawQuery), PII leakage, CI/workflow integrity
3. **Data integrity** — Room migration safety, schema export drift, persisted SessionState snapshot compatibility, expect/actual correctness
4. **Testing** — Are the changes adequately tested in commonTest? Missing edge cases?
5. **Standards** — Does it follow the patterns in .claude/CLAUDE.md?
6. **Architecture** — Is the code in the right layer/module? Pure game logic kept pure, ports respected, Metro bindings contributed correctly?

IMPORTANT RULES:
- SCOPE RULE: Only flag issues in code that was added or modified in the diff. You may READ surrounding code for context, but every finding MUST reference a line that appears in the diff.
- SEARCH BOUNDARY RULE: Do NOT search in **/build/**, **/.gradle/**, **/.kotlin/**, **/generated/**, .idea/, .vscode/, docs/, node_modules/, or dependency caches under ~/.gradle/caches/, ~/.konan/, ~/.m2/.
- NOISE FILTER RULE: Ignore toolu_[a-zA-Z0-9]+ strings — they are system artifacts, not code.
- CODEBASE CONTEXT: Before flagging an issue, search the codebase for how similar code is handled. Don't flag patterns used consistently across the codebase.
- PROVE DON'T SPECULATE: Read actual source code to confirm every finding. Only report issues you can cite with exact file and line.

OUTPUT FORMAT:
## 🔍 Gap Review — [file name(s)]

### Issues Found

For each issue:
- **File:Line** — Issue description
  - Severity: [1-10]/10
  - Category: [Security/Architecture/Data Integrity/Testing/Standards/UX]
  - Problem: What's wrong
  - Impact: What could happen
  - Fix: Specific code change needed (with before/after code block if applicable)

### No Issues
If the code looks correct after thorough review, respond with:
"Exhaustive review complete — no issues found in [file name(s)]."

This is a valid and expected outcome. Not every file has issues.
```

### After gap agents complete

Merge all gap-review findings into the main finding pool. These findings participate in:
- Stage 3 (cross-examination debate) — gap findings are attributed to "Gap Review Agent" and other agents can challenge/support them
- Stage 4B (automated fix extraction)
- Stage 5 (consensus synthesis)

**Gap finding rebuttal rule:** Gap review findings have no dedicated defender in Stage 4 (rebuttals). To compensate, gap findings are presumed valid and **stand unless a challenging agent provides concrete counter-evidence** (specific code references proving the finding is incorrect). A challenge based solely on "this is an established pattern" or "I don't think this is severe enough" is insufficient to overturn a gap finding — the challenger must cite the actual code that disproves the issue.

Display:

```
Gap Review Results:
✅ 🔍 Gap Agent 1 (ConversationDao.kt) — Found [X] issues
✅ 🔍 Gap Agent 2 (schema JSON) — No issues found
[...]

Total new findings from gap review: [N]
```

---

## Stage 3 — Cross-Examination Debate (Round 1)

**Skip condition:** Skip Stages 3, 4, and 4B entirely if ALL of the following are true:
- Total finding count (from all agents + gap review) is ≤ 3
- ALL findings have severity < 7.0

If skipped, display: "⏩ Skipping debate — [N] low-severity findings don't warrant cross-examination." Then jump directly to Stage 5, using the raw (pre-debate) severity scores as final scores.

**Also skip in quick mode** — quick mode never runs debate.

Display: "🗣️ Starting cross-examination debate round..."

For EACH agent, launch a NEW Task with their original findings PLUS all other agents' findings. All debate agents run in parallel.

**Debate prompt for each agent:**

```
You are the [Agent Name] in the cross-examination debate phase.

YOUR ORIGINAL FINDINGS:
[Paste that agent's original review output with severity scores]

OTHER AGENTS' FINDINGS:
[All other agents' findings with severity scores]

QUESTIONS DIRECTED AT YOU:
[Any "Questions for Other Agents" from other agents that are addressed to this agent. If none are directed at this agent, write "None."]

MISSION: Review other agents' findings from your specialized perspective.

DEBATE ACTIONS (use severity scores to prioritize):
1. **CHALLENGE** — Disagree with a finding (max 3 challenges, focus on severity 7+)
   - Cite your reasoning with evidence
   - Suggest revised severity score
2. **SUPPORT** — Strongly agree and add context (for severity 8+)
3. **EXPAND** — Build on a finding with additional concerns
4. **QUESTION** — Ask for clarification
5. **ANSWER** — Respond to questions directed at you from other agents

RULES:
- Maximum 3 challenges (focus on important disagreements)
- Provide specific reasoning and evidence
- Reference file:line when possible
- Suggest severity score adjustments (1-10)
- Be constructive, not combative
- IMPORTANT: Do not speculate. Only challenge or support with evidence from actual code.

OUTPUT FORMAT:

## [Agent Name] — Cross-Examination

### Challenges
- **Challenge to [Agent X] re: [finding]**
  - Original severity: [X]/10
  - Why I disagree: [reasoning with code evidence]
  - Revised severity: [Y]/10

### Strong Support
- **Support for [Agent X] re: [finding]**
  - Additional context: [your perspective]
  - Severity agreement: [X]/10 is correct

### Expansions
- **Building on [Agent X]'s [topic]**:
  - Additional severity: [+N] points
  - Reasoning: [why more severe]

### Questions
- **To [Agent X]**: [question]

### Answers to Questions
- **From [Agent X]**: "[their question]"
  - Answer: [your response with evidence]

### Summary
- Challenges: [N]
- Supports: [N]
- Key disagreements: [main contentions]
```

Launch all debate agents in parallel.

---

## Stage 4 — Rebuttals (Debate Round 2)

Collect all challenges from Stage 3 and give each challenged agent a chance to respond.

Display: "🔄 Starting rebuttal round..."

For each agent that received challenges, launch a new Task:

```
You are the [Agent Name] responding to challenges from debate round 1.

YOUR ORIGINAL FINDINGS:
[Their original findings with severity scores]

CHALLENGES RAISED AGAINST YOU:
[List each challenge with severity score adjustments]

MISSION: Respond to each challenge, adjusting severity scores based on evidence.

RESPONSE OPTIONS:
1. **DEFEND** — Additional evidence supports your finding (maintain severity)
2. **CONCEDE** — Acknowledge challenge, downgrade/remove finding
3. **REVISE** — Update finding based on new perspective
4. **ESCALATE** — Flag as unresolved, needs human senior review

OUTPUT FORMAT:

## [Agent Name] — Rebuttals

### Response to Challenge #1 from [Agent]
- Original Severity: [X]/10
- Decision: DEFEND/CONCEDE/REVISE/ESCALATE
- Reasoning: [explanation with code evidence]
- Final Severity: [Y]/10
- Updated Finding (if revised): [description]

### Summary
- Defended: [N]
- Conceded: [N]
- Revised: [N]
- Escalated: [N]
```

---

## Stage 4B — Extract & Organize Automated Fixes

**Note:** Fix extraction happens AFTER debate so that fix priorities reflect post-debate severity adjustments.

Parse agent outputs for automated fix patches:

**Process:**
1. Extract every before/after code patch from all agent reports
2. Group patches by file path
3. If multiple agents suggest fixes for the same file:line, merge or pick the highest-severity version
4. Deduplicate identical suggestions
5. Sort by **post-debate** severity (highest first)

**Display:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔧 AUTOMATED FIX PLAN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[N] fixes across [M] files

Fix #1 — Severity [X]/10 — [Agent Name]
File: [path]:[line]
[before/after code block]

Fix #2 — ...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Note: These fixes are for reference during the review. Stages 1–7 do NOT modify files. If you want to apply fixes, see **Stage 8 — Apply Fixes (Optional)** below, which is offered after the review report is delivered.

---

## Stage 5 — Consensus Synthesis & Cross-Cutting Analysis

Analyze all findings, debates, and final severity scores to build consensus. Then perform a cross-cutting consistency check that no individual agent can do alone.

**Process:**
1. Collect all final findings. For each finding, the **final severity** is determined as follows:
   - If the finding went through debate AND the original agent issued a CONCEDE or REVISE in Stage 4: use the **revised severity** from the rebuttal
   - If the finding went through debate but the agent DEFENDED: use the **original severity**
   - If the finding was ESCALATED: use the **original severity** and flag as "needs human review"
   - If debate was skipped (per skip condition): use the **raw severity** from the original agent report
   - Gap review findings (from Stage 2B): use raw severity unless challenged and conceded in debate
2. Group by similarity (same file:line or same general issue)
3. For grouped findings, use the **highest final severity** among the group (not the average — averaging dilutes genuine blockers)
4. Count agent agreement (how many agents flagged the same or similar issue)

**Cross-Cutting Consistency Check (post-debate):**

This step catches bugs that individual agents miss because they review from a single perspective.

1. **Operation Inventory**: List every distinct operation the PR implements (e.g., "advance a session via a `SessionEvent`", "load the card content", "persist a `CardPick`", "restore a session from its snapshot"). For each operation, identify ALL code paths that perform it — including the `transition` handler, the Presenter's event handling, repository methods, the persisted-snapshot write, and the navigation step.

2. **Safeguard Parity Check**: For each operation with multiple code paths, verify they all have equivalent:

| Code path | Validation | Persistence | Effect dispatch | Error handling | State consistency |
|-----------|-----------|-------------|-----------------|---------------|-------------------|

Flag any row that is missing a safeguard present in another row. These are must-fix (severity 9+).

3. **"Fix One, Fix All" Check**: If the PR fixes a pattern in one place (e.g., adding a default to one `@Serializable` field for snapshot compatibility), search for ALL other instances of that same pattern in the PR. Flag remaining instances.

**Consensus Levels (use highest final severity per finding, not average):**

Classification is based on severity first. Agent count serves as a confidence signal but never downgrades the tier below one step from the severity-based classification.

- **Severity 9.0–10.0, 4+ agents (or all agents in quick mode)**: CRITICAL BLOCKER
- **Severity 9.0–10.0, fewer than above threshold**: HIGH PRIORITY BLOCKER
- **Severity 8.0–8.9, 3+ agents**: HIGH PRIORITY BLOCKER
- **Severity 8.0–8.9, 1-2 agents**: IMPORTANT (should fix before merge)
- **Severity 7.0–7.9, 3+ agents**: IMPORTANT (should fix before merge)
- **Severity 7.0–7.9, 1-2 agents**: MEDIUM PRIORITY
- **Severity 5.0–6.9, 2+ agents**: MEDIUM PRIORITY
- **Severity 5.0–6.9, 1 agent**: SUGGESTION
- **Severity < 5.0**: SUGGESTION
- **Agents differ by 4+ severity points**: NEEDS HUMAN REVIEW (see Severity Spread Escalation below)

**Severity Spread Escalation:** If any individual agent rates a finding at severity 9+ AND another agent rates the same finding (or challenges it to) severity 5 or below (a spread of 4+ points), do NOT simply average the scores. Instead:
1. Flag the finding as **"NEEDS HUMAN REVIEW"** regardless of the average
2. Display both severity assessments in the report with each agent's reasoning
3. This finding counts as a blocker for verdict purposes until a human adjudicates

**Dismissed Finding Matching (from Stage 0D):**

If the dismissed findings list from Stage 0D is non-empty, compare each consensus finding against it:

1. **Match criteria** (ALL must be true):
   - Same file path (exact match)
   - Line number within ±10 of the dismissed finding's line
   - Body text has substantial overlap — strip `<!-- severity:X -->` tags and label prefixes, then check if the core issue description shares key phrases with the dismissed finding

2. **Dismissal rules:**
   - Only findings with consensus severity < 7 can be dismissed
   - Findings with severity ≥ 7 are NEVER dismissed, even if a match exists
   - Each dismissed finding from Stage 0D can match at most one new finding

3. **Mark matched findings as "dismissed"** — they will appear in the report (Stage 6) under a dedicated section, but they do NOT count toward the verdict calculation

**Verdict Calculation:**

Use only **non-dismissed, non-pre-existing** findings to determine the verdict. Findings with severity < 5 (pure suggestions) are **informational only** and do not affect the verdict — they appear in the report but do not require `/dismiss` and do not block a CLEAN verdict.

- **BLOCKERS_FOUND**: Any non-dismissed finding classified as **CRITICAL BLOCKER** or **HIGH PRIORITY BLOCKER** per the Consensus Levels table above, OR any finding flagged as NEEDS HUMAN REVIEW (from Severity Spread Escalation). Note: severity 8.0–8.9 with only 1–2 agents is downgraded to IMPORTANT and does NOT trigger BLOCKERS_FOUND on its own.
- **APPROVED_WITH_SUGGESTIONS**: No non-dismissed blockers, but has non-dismissed findings with severity **>= 5**
- **CLEAN**: No non-dismissed findings with severity **>= 5** (lower severity suggestions and pre-existing findings may exist but are informational only)

**Compute counters for AI_REVIEW_META** (these populate the JSON metadata block emitted in Stage 7).

The counters are populated from **Stage 5 classifications**, not from raw severity ranges. Use the Consensus Levels table above to determine each finding's tier, then map:

- `blockers` = count of non-dismissed, non-pre-existing findings classified as **CRITICAL BLOCKER** or **HIGH PRIORITY BLOCKER**, plus any finding flagged **NEEDS HUMAN REVIEW** (per Severity Spread Escalation)
- `important` = count of non-dismissed, non-pre-existing findings classified as **IMPORTANT** (this includes severity 7.0–7.9 with any agent count, AND severity 8.0–8.9 with only 1–2 agents per the agent-count downgrade rule above)
- `dismissed` = count of findings dismissed via `/dismiss` in Stage 0D that matched a new finding in this run

The auto-approve workflow gates on both `blockers == 0` AND `important == 0`. A severity 8.5 finding with only 1 agent is classified IMPORTANT (not a blocker) by the Consensus Levels table — it goes in `important`, NOT `blockers`. Trust the Stage 5 classification, not the severity range alone.

**Dead zone guidance (severity 7.0–7.9):** Findings in the "Important" tier (7.0–7.9) cannot be dismissed via `/dismiss` (the 7.0 threshold prevents it) but do not trigger BLOCKERS_FOUND by themselves. They DO prevent a CLEAN verdict (they're >= 5.0). When the report has Important-tier findings but no blockers, the verdict is APPROVED_WITH_SUGGESTIONS — but the report should include a note:

> ⚠️ **[N] Important findings (severity 7.0–7.9)** — These are strongly recommended fixes that don't block merge but can't be dismissed. Consider addressing them in this PR or creating follow-up tickets.

Display consensus summary:

```
📊 Consensus Analysis:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Critical Blockers (Severity 9.0–10.0): [N]
High Priority Blockers (Severity 8.0–8.9): [N]
Important Issues (Severity 7.0–7.9): [N]
Medium Priority (Severity 5.0–6.9): [N]
Suggestions (Severity < 5.0): [N] (informational — do not block verdict)
Pre-existing Issues: [N] (informational — do not block verdict)
Dismissed by Developer: [N]
Unresolved Debates: [N]

Total Findings: [N] ([M] active, [D] dismissed, [I] informational)
Average Confidence: [High/Medium/Low]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Stage 6 — Generate Review Report

Print the comprehensive review report:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🤖 MULTI-AGENT CODE REVIEW REPORT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Agents: [N] specialized reviewers with debate rounds
Mode: [quick/standard/deep]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Sections in order:**

### Verdict

Based on consensus:
- **BLOCKERS FOUND**: ❌ [N] critical + [N] high priority issues must be resolved before merge
- **APPROVED WITH SUGGESTIONS**: ⚠️ No blockers, but [N] improvements recommended (severity 5+)
- **CLEAN**: ✅ No significant issues found

[If BLOCKERS_FOUND, list the top 3 blocker summaries as one-liners here so the developer immediately knows what to fix]

### 📊 Risk Assessment
[From Stage 0B — risk score, level, reviewer recommendation]

### 📦 Dependency Impact
[From Stage 1B — high-impact files, breaking changes]

### 🚫 Critical Blockers (Severity 9.0–10.0)
For each:
- Severity: [X.X]/10 (Consensus from [N] agents)
- File: [file:line]
- Flagged by: [Agent 1 (score), Agent 2 (score), ...]
- Problem: [detailed description]
- Debate Summary: [challenges and resolutions]
- Required Action: [specific fix]

### 🔴 High Priority Blockers (Severity 8.0–8.9)
[Same format as critical]

### ⚠️ Important Issues (Severity 7.0–7.9)
Note: Important issues (7.0–7.9) are strongly recommended but don't block merge. They cannot be dismissed via `/dismiss`. Address them in this PR or create follow-up tickets.
For each:
- Severity: [X.X]/10
- File: [file:line]
- Flagged by: [Agents]
- Issue: [description]
- Recommended Fix: [how to address]

### 💡 Medium Priority (Severity 5.0–6.9)
[Bulleted list with file:line and brief description]

### 💭 Suggestions (Severity < 5.0)
[Grouped by category, bulleted list]

Note: Suggestions (severity < 5) are informational and do not require `/dismiss`. They do not block a CLEAN verdict.

### 🔎 Pre-existing Issues (Informational)
Only include this section if agents flagged pre-existing issues (labeled `[Pre-existing]`).

For each:
- Severity: [X.X]/10
- File: [file:line]
- Issue: [description]
- Note: This issue predates this PR. Consider addressing in a follow-up PR.

Pre-existing issues are informational only — they do not count toward the verdict and do not require `/dismiss`.

If no pre-existing issues exist, omit this section entirely.

### ✅ Dismissed Findings (Acknowledged by Developer)
Only include this section if there are dismissed findings from Stage 0D that matched new findings.

For each dismissed finding:
- Severity: [X.X]/10
- File: [file:line]
- Finding: [description]
- Dismissed by: @[developer] — "[reason]"

These findings were reviewed and acknowledged by the developer. They do not count toward the verdict.

If no dismissed findings exist, omit this section entirely.

### 🤔 Unresolved Debates
For each:
- Debate Topic: [what]
- Severity Range: [Low]-[High]/10
- Positions: [Agent A argues X, Agent B argues Y]
- Why unresolved: [explanation]
- Recommendation: Senior developer should decide

### 📝 Review Summary Table

| Agent | Critical | High | Important | Suggestions | Confidence |
|-------|----------|------|-----------|-------------|------------|
| 🔒 Security | [N] | [N] | [N] | [N] | [H/M/L] |
| 🏗️ Architecture | [N] | [N] | [N] | [N] | [H/M/L] |
| 💾 Data Integrity | [N] | [N] | [N] | [N] | [H/M/L] |
| 🧪 Testing | [N] | [N] | [N] | [N] | [H/M/L] |
| 👤 UX | [N] | [N] | [N] | [N] | [H/M/L] |
| 📋 Soularium Standards | [N] | [N] | [N] | [N] | [H/M/L] |
| **Total** | **[N]** | **[N]** | **[N]** | **[N]** | |

---

## Stage 7 — Deliver Results

**Step 1: Get PR metadata**
```bash
COMMIT_SHA=$(gh pr view --json commits --jq '.commits[-1].oid' 2>/dev/null)
PR_NUM=$(gh pr view --json number --jq '.number' 2>/dev/null)
```

If no PR exists (or the repo has no remote), print to terminal only.

**Step 2: Ask delivery method**

"How would you like your feedback?"
- **Post to GitHub**: Comments will be left on your GitHub PR
- **Print to Terminal**: Comments will be printed here in terminal
- **Both**: Post to GitHub AND print to terminal

**For GitHub posting:**

**Step 2A: Validate findings against PR file list**

GitHub's PR review API only accepts line comments on files that are part of the PR diff. Before building `.ai-review.json`, validate each finding's file path:

```bash
# Get the authoritative list of files in this PR's diff
gh pr diff --name-only 2>/dev/null
```

For each finding with a file path and line number:
1. Check if the file path appears in the `gh pr diff --name-only` output
2. If **YES** → include it as a normal line-anchored comment in the `comments` array
3. If **NO** → this is a "related file finding" — do NOT include it in `comments` (GitHub will reject it with 422 "Path could not be resolved")

**Handling non-PR file findings:**

Findings on files not in the PR diff must be included in the review `body` text instead. Add a section in the body BEFORE the `AI_REVIEW_META` tag:

```markdown
---

### 📎 Findings on Related Files (Not in This PR)

These findings were identified during the review on files related to the changes but not directly modified in this PR. They cannot be posted as line comments.

**[Severity Label] path/File.kt:LINE** — Description of the finding
- Severity: X.X/10
- Flagged by: [Agent Name]
- Recommended Action: [what to do]

[Repeat for each non-PR finding]
```

Non-PR file findings:
- Are informational — they do NOT count toward the verdict or blocker count
- Cannot be dismissed via `/dismiss` (they aren't line comments)
- Should still be shown so the developer is aware of related issues
- Pre-existing issues (labeled `[Pre-existing]`) naturally fall here if they're on non-PR files

**IMPORTANT:** Always use `gh pr diff --name-only` (not `git diff $BASE_REF..$HEAD_REF --name-only`) for this check. The `git diff` version may include files from other PRs that have been merged between the base and head refs, while `gh pr diff` shows only files in THIS PR's diff.

Create `.ai-review.json` with validated findings as line-anchored comments using `line` + `side` fields (format described below).

**CRITICAL — event field must be "COMMENT":**

The `event` field in the review JSON MUST always be `"COMMENT"` — NEVER `"APPROVE"` or `"REQUEST_CHANGES"`. The agent review does NOT approve PRs. A separate GitHub Action (`ai-review-auto-approve`) reads the metadata from the review comment and handles approval automatically. Attempting to approve will fail (you can't approve your own PR via the API).

**Top-level JSON structure:**
```json
{
  "commit_id": "COMMIT_SHA",
  "event": "COMMENT",
  "body": "<review summary with AI_REVIEW_META>",
  "comments": [...]
}
```

**IMPORTANT — Severity tag in each comment body:**

Every comment in the `comments` array MUST start with a machine-readable severity tag. This enables the `/dismiss` feature to validate that only non-blocking findings (severity < 7) can be dismissed.

Format: `<!-- severity:X.X --> [Label] Description...`

Example:
```json
{
  "path": "shared/src/commonMain/kotlin/org/cru/soularium/ui/conversation/ConversationPresenter.kt",
  "line": 42,
  "side": "RIGHT",
  "body": "<!-- severity:5.2 --> [Medium] Consider hoisting this transient flag into UiState for consistency with existing Presenters."
}
```

Labels map to severity tiers (exclusive upper bounds — use the tier where the score falls):
- `[Critical]` — severity 9.0–10.0
- `[High]` — severity 8.0–8.9
- `[Important]` — severity 7.0–7.9
- `[Medium]` — severity 5.0–6.9
- `[Suggestion]` — severity < 5.0

**IMPORTANT — Machine-readable metadata for auto-approval:**

The review `body` in `.ai-review.json` MUST include this HTML comment at the very end, after all human-readable content:

```
<!-- AI_REVIEW_META: {"risk_level": "[LOW|MEDIUM|HIGH|CRITICAL]", "risk_score": [N], "blockers": [N], "important": [N], "verdict": "[CLEAN|APPROVED_WITH_SUGGESTIONS|BLOCKERS_FOUND]", "dismissed": [N]} -->
```

**The HTML comment must be on a single line.** The auto-approve workflow's regex is anchored to end-of-line (`\s*$/m`) and does not span newlines. Do not pretty-print the JSON across multiple lines.

Where:
- `risk_level`: The risk level from Stage 0B (LOW, MEDIUM, HIGH, or CRITICAL)
- `risk_score`: The numeric risk score (0-10) from Stage 0B
- `blockers`: Total count of non-dismissed findings classified as **CRITICAL BLOCKER** or **HIGH PRIORITY BLOCKER** by Stage 5 (severity 9.0+ with any agent count, plus severity 8.0–8.9 with 3+ agents). A severity 8.0–8.9 finding with only 1–2 agents is downgraded to IMPORTANT and counted in `important` instead, NOT here.
- `important`: Total count of non-dismissed findings classified as **IMPORTANT** by Stage 5 (severity 7.0–7.9 with any agent count, AND severity 8.0–8.9 with only 1–2 agents per the agent-count downgrade rule). These cannot be dismissed via `/dismiss` (the floor is 7.0), so the auto-approve workflow blocks on `important > 0` to prevent the dead zone where IMPORTANT-tier findings would silently auto-approve.
- `verdict`: One of `CLEAN`, `APPROVED_WITH_SUGGESTIONS`, or `BLOCKERS_FOUND` — calculated using only non-dismissed findings (see Stage 5)
- `dismissed`: Count of findings that were dismissed by the developer via `/dismiss` replies (0 if none)

All six fields are required. The auto-approve workflow rejects metadata with missing or invalid fields rather than coercing to permissive defaults — fail closed.

This metadata is invisible in the rendered review but enables the `ai-review-auto-approve` GitHub Action to automatically approve LOW/MEDIUM risk PRs with a CLEAN or APPROVED_WITH_SUGGESTIONS verdict — satisfying the required reviewer rule without human intervention. HIGH/CRITICAL risk PRs or PRs with BLOCKERS_FOUND still require human review. CI is enforced separately by branch protection rules.

**Step 2B: Validate JSON before posting**

Before posting, validate the JSON is well-formed:

```bash
jq . .ai-review.json > /dev/null 2>&1 || { echo "ERROR: .ai-review.json is malformed JSON. Dumping to terminal for manual review:"; cat .ai-review.json; exit 1; }
```

If validation fails, print the raw JSON to the terminal so the developer can salvage the review output. Do NOT attempt to post malformed JSON to the API.

Then post the review (`gh` resolves `{owner}/{repo}` from the current checkout — do NOT hardcode a slug):

```bash
if gh api \
  --method POST \
  -H "Accept: application/vnd.github+json" \
  "repos/{owner}/{repo}/pulls/${PR_NUM}/reviews" \
  --input .ai-review.json; then
  rm .ai-review.json
  echo "Review posted to PR #${PR_NUM}"
else
  echo "ERROR: Failed to post review to GitHub. The review file has been preserved at .ai-review.json for manual retry or inspection."
  echo "To retry: gh api --method POST -H 'Accept: application/vnd.github+json' 'repos/{owner}/{repo}/pulls/${PR_NUM}/reviews' --input .ai-review.json"
fi
```

**Troubleshooting `gh api` failures:**

- **422 "Path could not be resolved"**: A file path in the `comments` array doesn't exist in the GitHub PR diff, or a line number is outside the diff hunk range. Verify every `path` exists in `gh pr diff --name-only` output (use the validation in Step 2A) and every `line` falls within a valid hunk range from `gh pr diff`. This is the #1 cause of 422s.
- **422 "Unprocessable Entity"**: Line numbers are outside the diff range — recheck each `line` against the `@@ -X,Y +A,B @@` headers in `gh pr diff`. The line must be in the `[A, A+B-1]` range of an added/context line for `"side": "RIGHT"`, or `[X, X+Y-1]` of a removed/context line for `"side": "LEFT"`.
- **422 "Pull request review thread line must be part of the diff"**: Same root cause as above — a line outside the diff hunks. Re-validate against the diff hunk headers.
- **File creation blocked / cannot write `.ai-review.json`**: Print the JSON object to chat (no prose, no code fences, no leading whitespace) so the developer can paste it directly into a `curl`/`gh api` call.
- **Empty response from `gh api`**: Verify `gh` CLI is authenticated (`gh auth status`) and the PR exists (`gh pr view ${PR_NUM}`). Check that the auth token has `repo` scope. If the repo has no remote configured, GitHub posting is not possible — fall back to terminal-only delivery.
- **Stacked PRs**: If the PR targets a feature branch (not `main`), always use `gh pr diff` for both the file list AND the diff content — never `git diff main...HEAD`, which would include parent-branch changes that GitHub does not consider part of THIS PR's diff. Comments anchored to those lines will fail with 422.
- **Cannot approve own PR**: If you see "Can not approve your own pull request", the `event` field was set to `"APPROVE"` instead of `"COMMENT"`. The agent review NEVER approves — auto-approval is handled by the separate `ai-review-auto-approve` workflow which posts as `github-actions[bot]`.

**After posting the review to GitHub**, if ALL of these conditions are met, trigger the auto-approve workflow:
- The review was posted to GitHub (not terminal-only)
- Risk level is LOW or MEDIUM
- Verdict is CLEAN or APPROVED_WITH_SUGGESTIONS
- Zero blockers AND zero Important findings (per Stage 5 classification, not raw severity). The workflow's Gate 2 rejects on `important > 0`, so dispatching with Important findings just generates a wasted run + misleading audit trail.

Chain this dispatch with the prior `gh api .../reviews` POST in a single Bash invocation so `${PR_NUM}` (set in Step 1) stays in scope; the Bash tool does not preserve shell state across separate invocations:

```bash
BRANCH=$(gh pr view ${PR_NUM} --json headRefName --jq '.headRefName')
if [ -n "$BRANCH" ]; then
  if dispatch_err=$(gh workflow run ai-review-auto-approve.yml \
        -f head_branch="${BRANCH}" \
        2>&1); then
    echo "Triggered auto-approve workflow for branch ${BRANCH}"
  else
    echo "WARNING: Failed to trigger auto-approve workflow: ${dispatch_err}"
  fi
else
  echo "WARNING: Could not determine branch name, skipping auto-approve trigger"
fi
```

**Note:** the workflow file (`.github/workflows/ai-review-auto-approve.yml`) only runs from the repository default branch, so until this PR is merged the dispatch step will produce a "could not find any workflows" warning. That's the expected bootstrap state and resolves once the workflow lives on the default branch.

Do NOT trigger the workflow if any condition is not met (e.g., BLOCKERS_FOUND verdict or HIGH/CRITICAL risk). The auto-approve workflow does NOT check CI status — CI is enforced by the repo's branch protection rules (CI must pass before merging).

**Line number calculation rules:**
- Use `line` (actual file line number) + `side` (`"RIGHT"` for new/context lines, `"LEFT"` for deleted lines)
- Use the `+N` side of `@@` hunk headers from `gh pr diff` to determine line numbers in the new file. The `+N,M` means the new file starts this hunk at line `N` and spans `M` lines. Count down from `N`.
- Before finalizing, re-read the diff and verify each line number falls within a valid hunk range

**Worked example (Kotlin diff):**

```diff
@@ -56,6 +57,7 @@ class ConversationPresenter(                  <- new file lines start at 57

     private val json: Json,                                          <- line 58
     private val repository: SessionRepository,                       <- line 59
+    private val analytics: AnalyticsTracker,                         <- line 60 (use line: 60, side: "RIGHT")
 ) : Presenter<ConversationPresenter.UiState> {                       <- line 61
```

The `+57` means the new file starts this hunk at line 57. The added line `+    private val analytics...` is line 60 — comment on it as `{"line": 60, "side": "RIGHT"}`.

**IMPORTANT:** The `line` MUST fall within the diff hunk range for that file. You can only comment on lines that appear in the diff output (added, removed, or context lines). You CANNOT comment on arbitrary lines that are outside the diff hunks. Lines in the file that are not surfaced as added/removed/context in the diff are out of bounds.

For an existing file with context-only changes:

```diff
@@ -10,6 +10,8 @@
 unchanged line                       <- line 10
 unchanged line                       <- line 11
+    onAdvance(SessionEvent.Next)     <- line 12 (use line: 12, side: "RIGHT")
+    showLoading()                    <- line 13 (use line: 13, side: "RIGHT")
 unchanged line                       <- line 14
```

For a deleted line, use `"side": "LEFT"` and the original (pre-diff) line number from the `-N,M` side of the hunk header.

**For terminal printing:**
- Group comments by file path
- Show line numbers with each comment
- Display the review body/summary at the top

After delivery, print:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Multi-agent review complete for PR #[NUMBER]
   [N] agents | [N] findings | [N] blockers
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**If the review was posted to GitHub AND risk is LOW or MEDIUM**, also print:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🤖 AUTO-APPROVAL ELIGIBLE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

This PR qualifies for auto-approval (LOW/MEDIUM risk).

[If CLEAN verdict]:
  ✅ No issues found — auto-approve workflow triggered.
     CI must still pass before the PR can be merged (branch protection).

[If APPROVED_WITH_SUGGESTIONS verdict]:
  ✅ Auto-approve workflow triggered.
     Suggestions have been posted as review comments for you to consider.
     These are non-blocking — address them at your discretion.
     CI must still pass before the PR can be merged (branch protection).

[If BLOCKERS_FOUND verdict]:
  🚫 Blockers were found — human review required.
  To resolve:
  1. Fix all findings classified as CRITICAL BLOCKER or HIGH PRIORITY BLOCKER (per Stage 5 — generally severity 9+, plus severity 8.0–8.9 with 3+ agents)
  2. For severity < 7 issues you disagree with, reply
     /dismiss: <reason> on the review comment in GitHub
  3. Push your fixes and re-run /agent-review

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**If the review was NOT posted to GitHub AND risk is LOW or MEDIUM**, print:

```
💡 TIP: Post this review to GitHub to enable auto-approval
   for LOW/MEDIUM risk PRs. Re-run with "Post to GitHub" or
   "Both" to activate.
```

**If risk is HIGH or CRITICAL**, print nothing extra (human review is always required).

---

### Step 3: Offer Stage 8 (Apply Fixes)

After delivering the review (terminal, GitHub, or both), prompt the user:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔧 APPLY FIXES?
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Would you like me to apply the suggested fixes? Choose one:
  1. **Apply all** — implement every actionable finding (must-fix, deep,
     cross-cutting, nice-to-have, suggested tests, reuse opportunities)
     and re-run the review until clean (Stage 8)
  2. **Apply approved** — walk through findings interactively; you approve
     each one before I make the change, then re-run the review until clean
  3. **Skip** — leave fixes to the developer (default)

Pick 1, 2, or 3.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

WAIT FOR USER RESPONSE before doing anything else. If the user picks **3** (Skip) or does not respond, the review ends here. If the user picks **1** (Apply all) or **2** (Apply approved), proceed to Stage 8 with the chosen mode.

---

## Stage 8 — Apply Fixes (Optional)

**Triggered only by explicit user choice from Stage 7 (option 1 or 2).** This stage iteratively reviews → fixes → re-reviews until no actionable findings remain or a safety limit is reached.

### Configuration

- **Apply mode**: `apply-all` (option 1) or `apply-approved` (option 2) — set by user choice in Stage 7
- **Review output during iteration**: **Terminal** (never post to GitHub during iterative review)
- **Maximum iterations**: **10** (safety limit to prevent infinite loops)

### Critical: Fresh Context Per Iteration

Each review iteration MUST run in a **fresh subagent** using the Task tool (`subagent_type: "general-purpose"`). This prevents focus bias — without fresh context, the reviewer pattern-locks on the issue categories it found in previous iterations and misses entirely different classes of bugs (e.g., finding state-machine purity issues in iteration 1, then missing concurrency bugs in later iterations because it's still focused on purity).

Do NOT run the review in the main conversation context. Do NOT resume a previous review agent. Each iteration = new agent = fresh eyes.

### Process

#### For each iteration N (starting at N = 1):

**Step 1: Run the review in a fresh subagent**

Launch a new subagent using the Task tool with `subagent_type: "general-purpose"`. Give it:
- Instruction to `Read` `.claude/commands/agent-review.md` and follow Stages 0A–7 (do NOT inline the file content into the prompt — that's ~1900 lines per iteration; tell the subagent to read it from disk)
- Instruction to operate in `standard` mode (or `deep` if the user explicitly asked for deep)
- Instruction to print results to terminal (NOT post to GitHub)
- Instruction to skip `.ai-review.json` creation
- Instruction to skip the auto-approve workflow trigger
- Instruction to return ALL findings in its response (must-fix, nice-to-have, deep investigation, cross-cutting, suggested tests, reuse opportunities, pattern findings)

**IMPORTANT:** Do NOT pass any information about previous iterations' findings to the subagent. It must review with completely fresh eyes. The only context it needs is the agent-review instructions and the codebase.

**Step 2: Analyze the subagent's review results**

When the subagent returns, categorize all findings:

- **Must-fix** (severity ≥ 7): bugs, security issues, breaking changes, performance problems — ALWAYS implement these in `apply-all` mode; ASK FIRST in `apply-approved` mode
- **Deep investigation findings**: proven issues from the PROVE DON'T SPECULATE rule — ALWAYS implement these in `apply-all` mode; ASK FIRST in `apply-approved` mode
- **Cross-cutting consistency issues**: safeguard parity failures from Stage 5 — ALWAYS implement these in `apply-all` mode; ASK FIRST in `apply-approved` mode
- **Nice-to-have**: style improvements, minor refactoring — implement these in `apply-all`; ASK FIRST in `apply-approved`
- **Suggested tests**: missing test coverage — implement these in `apply-all`; ASK FIRST in `apply-approved`
- **Reuse opportunities**: code that could use existing utilities — implement these in `apply-all`; ASK FIRST in `apply-approved`

Before acting on findings, filter out false positives:
- Read the actual source code to verify each finding is real
- Discard findings that misread the code or don't apply
- Only implement fixes for confirmed issues

**If the review is clean** (no must-fix, no nice-to-have, no suggested tests, no reuse opportunities, no deep findings, no consistency issues — only praise or "no issues found" statements):

```
✅ Code review passed — no remaining issues after N iteration(s).
```

Stop the cycle and proceed to "After the final clean review" below.

**If the review has actionable findings:**

```
🔄 Iteration N: Found X actionable item(s). Implementing fixes...
```

List all items being addressed. In `apply-approved` mode, present each finding to the user and wait for approval before applying — track which were approved, which were skipped.

**Step 3: Implement all approved fixes**

For each approved finding, make the code change:
- Fix bugs, security issues, and must-fix items first (severity ≥ 7)
- Then address nice-to-have improvements
- Then add suggested tests
- Then apply reuse opportunities

**Conflict handling:** if two fixes target the same file/lines and would conflict, apply the higher-severity fix and report the lower-severity one as deferred ("Deferred to next iteration: <finding> conflicted with <higher-severity-finding>"). The deferred fix will reappear in the next iteration's review (or won't, if the higher-severity fix incidentally addresses it).

After all fixes are applied, run the verification commands from the repo root (in this order — stop and fix on the first failure rather than running the rest):

1. **Format Kotlin sources:** `./gradlew ktlintFormat` — auto-fix any new style issues introduced by the edits
2. **Lint check:** `./gradlew ktlintCheck` (from the repo root) — fail on any remaining ktlint violations. Note: `ktlintFormat` handles auto-fixable rules only; some rules can still flag issues here even after step 1. Treat those as a real fix-needed signal — do not loop step 1 expecting it to clear them.
3. **All unit tests:** `./gradlew allTests` (from the repo root) — runs every module's `commonTest` suite across all KMP targets. For a fast inner-loop check, `testAndroidHostTest` covers the Android-host JVM subset.
4. **Android assembly:** `./gradlew :androidApp:assembleDebug` (from the repo root) — confirms the Android app still compiles
5. **iOS framework link** (only on macOS hosts): `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` (from the repo root) — if not running on macOS, skip with a note: "iOS framework link skipped (not on macOS); CI will run it on `macos-14`"

**Codegen note:** Room (KSP) and Compose Resources generate code on every build. If an issue is in generated code, the fix must go in the source (Room `@Entity`/`@Dao`, the `composeResources/` files), not the generated output.

**Dependency / vulnerability check (informational):** this repo does NOT have an automated CVE/SAST scanner wired into Gradle. If the iteration's fixes introduced or removed dependencies in `gradle/libs.versions.toml`, print a one-line note: "⚠️ Dependency changes — verify any pending CVE bumps for the affected libraries." Continue regardless.

**If any verification step fails:**
- Print the failing step's output
- DO NOT proceed to commit
- DO NOT continue to the next iteration with failing tests/lint
- Investigate and fix the failure (treat it as a new must-fix in this iteration), then re-run the verification chain from step 1
- If you cannot fix the failure within reasonable effort, halt the loop and print: "⚠️ Iteration N halted — verification failed. The codebase is in an unfinished state; review before continuing."

**Step 4: Commit the fixes**

Stage and commit all changes from this iteration. Use a HEREDOC commit message referencing the iteration:

```bash
git add <specific files modified this iteration>
git commit -m "$(cat <<'EOF'
Fix <short summary> (agent review iteration N)

<bulleted list of findings addressed>
EOF
)"
```

Examples:
- `Fix expect/actual contract drift in PlatformBackHandler (agent review iteration 1)`
- `Add missing test coverage for ConversationPresenter event handling (agent review iteration 2)`

Prefer `git add <file>` over `git add -A` to avoid accidentally staging unrelated files. Confirm the commit succeeded with `git status` before continuing.

**Step 5: Loop back to Step 1**

Start the next iteration with a **new** fresh subagent. Do NOT resume the previous one.

### Important Rules

- NEVER run the review in the main conversation context — always use a fresh subagent
- NEVER pass previous iteration findings to the new subagent — fresh eyes every time
- NEVER post reviews to GitHub during the iterative cycle — always print to terminal
- NEVER skip running the verification chain after implementing fixes
- NEVER continue to the next iteration if tests, ktlint, or assembly are failing
- NEVER amend a previous iteration's commit — each iteration produces its own commit so the developer can audit each step
- If you hit the maximum iteration limit (10), stop and print:
  ```
  ⚠️ Reached maximum iterations (10). Remaining issues may need manual review.
  ```
  Then list any remaining unresolved findings with file:line references.
- Each iteration should make meaningful progress. If the same issue keeps appearing across iterations, investigate why the fix isn't working rather than blindly re-applying it. Common causes:
  - The fix is in the wrong file (a sibling file has the same pattern)
  - The fix doesn't actually address the root cause (test still fails)
  - A code generator (Room KSP, Compose Resources) regenerates the offending code on every build — the fix must go in the source `@Entity`/`@Dao` or `composeResources/` files, not the generated output
- In `apply-approved` mode, if the user rejects a finding (declines the fix), DO NOT re-prompt for it in the next iteration. Track rejected findings across iterations and skip them automatically. Print a note when a finding is auto-skipped: "⏩ Skipping <finding> — previously rejected by developer."

### After the final clean review

When the loop exits with a clean verdict, print:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Stage 8 complete — clean review after N iteration(s).
   M commit(s) added. Run CI to confirm before merging.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Then ask:

> Would you like me to re-run `/agent-review` one final time with **Post to GitHub** enabled? This is the canonical review-of-record on the PR and triggers the auto-approve workflow for LOW/MEDIUM risk PRs.

If the user agrees, re-enter at Stage 0A with the user's preferred mode (`standard` by default) and `Post to GitHub` selected at Stage 7.

If the user declines or does not respond, the agent-review run ends here.
