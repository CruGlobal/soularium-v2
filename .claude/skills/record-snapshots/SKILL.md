---
name: record-snapshots
description: Push branch to GitHub, run the record-snapshots workflow, fetch the snapshot commit it creates, and either fold it into an existing commit via fixup + autosquash or leave it as a standalone commit (approving any pending CI runs in that case).
allowed-tools: Bash, Read, Grep, Glob
---

Record updated Paparazzi snapshots for the current branch and integrate the resulting CI commit either by folding it into an existing commit or by leaving it as its own commit on the branch.

## Steps

### 1. Capture branch state

```bash
git branch --show-current
git log --oneline main..HEAD
```

Note the current branch name and list of commits since `main`.

### 2. Push to origin

```bash
git push origin HEAD
```

If this fails (rejected), explain why and stop — do not force-push without user confirmation.

### 3. Trigger the workflow

Before triggering, check whether a run is already in progress for this branch:

```bash
gh run list --workflow=record-snapshots.yml --branch=<branch> --limit=1 --json databaseId,status,conclusion
```

If a run is already `in_progress` or `queued`, inform the user and ask whether to wait for that run or trigger a new one. Otherwise, trigger:

```bash
gh workflow run record-snapshots.yml --ref <branch>
```

Wait a few seconds for GitHub to register the run, then get its ID:

```bash
sleep 5
gh run list --workflow=record-snapshots.yml --branch=<branch> --limit=1 --json databaseId,status,conclusion
```

### 4. Wait for completion

```bash
gh run watch <run-id> --interval 30
```

If the conclusion is not `success`, fetch and display the run log and stop:

```bash
gh run view <run-id> --log-failed
```

### 5. Fetch the snapshot commit

```bash
git fetch origin <branch>
```

Check how many new commits the remote has:

```bash
git log --oneline HEAD..origin/<branch>
```

- **0 commits ahead**: no snapshots changed (CI skipped the commit) — inform the user and stop.
- **1 commit ahead**: proceed.
- **More than 1 commit ahead**: warn the user and stop — the situation is unexpected and needs manual handling.

### 6. Identify the recommended target commit

Snapshot diffs are usually caused by a Layout (composable) change, but the Paparazzi test fixture can also drive them. Gather candidates from BOTH sources and then pick whichever is more recent — **don't fall through cascade-style**, because the cascade can hide the actual cause when the layout was edited more recently than the test:

```bash
git log --oneline main..HEAD -- '*PaparazziTest*.kt'    # Paparazzi test candidates
git log --oneline main..HEAD -- '*Layout.kt'             # Layout candidates
```

If neither returns anything, fall back to any test source touched:

```bash
git log --oneline main..HEAD -- '*/src/commonTest/**' '*/src/androidHostTest/**'
```

The **recommended** target is the most recent candidate across the combined pool. If the most-recent Paparazzi-test commit and most-recent Layout commit are different, present BOTH in Step 7 — that ambiguity usually means the user is the right person to pick.

### 7. Ask the user how to integrate the snapshot commit

Build the squash-candidate list from the Paparazzi-test + Layout pool collected in Step 6, deduplicated and sorted by recency (newest first). Pick whichever commit you judge most likely to have caused the pixel diff and mark it as **recommended** — typically the most recent Layout edit (since layout changes drive pixel diffs more often than test-fixture changes), but use the diff itself if you can tell which file actually moved (compare the recorded PNGs to `git show` the candidate diffs).

Use the AskUserQuestion tool. Present, in this order:
- **Every candidate** from the pool, newest first. Prefix the recommended one with `(recommended)` in its label so it's the obvious default.
- A `Select a different commit` option — when chosen, follow-up with a second AskUserQuestion listing all commits in `main..HEAD` so the user can pick any SHA on the branch, not just the candidates.
- A `Leave as a standalone commit on the branch` option.

The AskUserQuestion tool caps a single prompt at 4 options. If the candidate pool plus the two meta-options exceeds 4, keep only the top 2 candidates by recency in the first prompt — the user can still reach the rest through `Select a different commit`. Never drop the meta-options to make room for more candidates.

If the user picks a candidate or a different commit, continue with Step 8 (Squash path). If the user picks `Leave as standalone`, jump to Step 11 (Standalone path).

### 8. Squash path — apply the snapshot changes locally

The CI commit may add new snapshots, delete old ones (e.g. when a test was renamed), or both. Handle all cases:

```bash
# Get the full diff between local HEAD and the remote snapshot commit
git diff --name-only HEAD origin/<branch>
git diff --name-status HEAD origin/<branch>
```

For files marked **A** (added) or **M** (modified) — stage them at the remote version:
```bash
git checkout origin/<branch> -- <file> [<file> ...]
```

For files marked **D** (deleted) — remove them:
```bash
git rm <file> [<file> ...]
```

Do not use a single `git checkout origin/<branch> -- $(git diff --name-only ...)` shortcut, as it cannot handle deletions (checking out a deleted file would re-create it instead of removing it).

### 9. Squash path — create the fixup commit

```bash
git commit --fixup=<target-sha>
```

### 10. Squash path — rebase with autosquash, then confirm + force-push

```bash
git rebase --autostash --autosquash $(git merge-base HEAD main)
```

If the rebase fails (conflict), explain the conflict and stop — do not attempt to auto-resolve.

Show the user the resulting log:

```bash
git log --oneline main..HEAD
```

**Ask for explicit confirmation before force-pushing.** Then, with confirmation:

```bash
git push --force-with-lease origin <branch>
```

Use `--force-with-lease` (not `--force`) to guard against unexpected remote changes. **Stop here — the squash path is complete.**

### 11. Standalone path — fast-forward local to include the snapshot commit

The snapshot commit is already on `origin/<branch>` from CI; the goal is just to bring local in sync without altering history. No force-push needed.

```bash
git merge --ff-only origin/<branch>
git log --oneline main..HEAD | head -3
```

Confirm the snapshot commit appears at the top of the local log.

### 12. Standalone path — unblock any pending CI runs on the snapshot commit

When the snapshot commit lands on a PR branch, downstream workflows (build, lint, paparazzi verify, etc.) typically get blocked because GitHub prevents workflow runs created by another workflow from cascading. These blocked runs show up with **`status: completed` and `conclusion: action_required`** — NOT `status: action_required`. Check both shapes:

```bash
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
HEAD_SHA=$(git rev-parse HEAD)

gh api "repos/$REPO/actions/runs?head_sha=$HEAD_SHA" \
  --jq '.workflow_runs[] | select(
      .status == "action_required"
      or .status == "waiting"
      or .conclusion == "action_required"
    ) | {id, name, status, conclusion}'
```

For each run ID returned, try `gh run rerun` first — most blocked runs in this project are on internal branches where the cascade block is the cause, and `rerun` restarts the run as a fresh top-level event:

```bash
gh run rerun <run_id>
```

`rerun` works for internal-branch runs. If it fails (e.g. the run was actually blocked by a fork-PR approval gate), fall back to the approve endpoint, which is the dedicated path for fork-PR runs:

```bash
gh api -X POST "repos/$REPO/actions/runs/<run_id>/approve"
```

`approve` returns `403 "This run is not from a fork pull request"` for internal-branch runs, which is why we try `rerun` first. If both fail, surface the run to the user — don't retry further.

After triggering, wait a couple seconds and re-query the same SHA to confirm the runs left the blocked state:

```bash
sleep 3
gh api "repos/$REPO/actions/runs?head_sha=$HEAD_SHA" \
  --jq '.workflow_runs[] | {id, name, status, conclusion}'
```

If you still see runs with `conclusion: action_required`, surface them to the user with their `name` and a link to the Actions tab — don't loop endlessly.

If the initial query returned no runs in any blocked state, that's the happy path — workflows triggered naturally and there's nothing to unblock. Say so and finish.
