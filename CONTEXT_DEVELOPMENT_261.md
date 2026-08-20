# Context for Next Task

> **Maintenance tip**: Keep this file updated as you make progress — update the tip commit hash,
> move completed work into the "Recently Completed" section, and revise the "Pending" section.
> This file is the primary handoff document between sessions.
>
> **Next step guidance**: Only write a "Next step" when there is something actionable *after* a PR
> merges. Don't write "get PR reviewed/merged" — this doc lives in the repo and is read post-merge,
> so those entries are immediately stale. Use `—` or omit the field when nothing remains.

## Current State (as of 2026-08-18)

### Working Branch
`feat/file-event-batch-guard` — latest tip: `70a5a0b38b feat: skip new-file event processing when batch exceeds 5 files`

### Recently Completed Work (PR #33)

**Branch**: `fix/jvm-wrapper-project-sdk` → merged into `development-261`

**Problem**: PR #31 introduced two regressions with `jvm_wrapper_runtime` JDK resolution:
1. Project SDK reported as "not configured" — `defaultJdkName` derived from raw wrapper path but SDK registered under resolved path.
2. Target modules showed red code — `ModuleDetailsToJavaModuleTransformer` looked up the raw-path SDK name which no longer existed.

**Fix**: `SdkUtils.resolveJavaHome()` widened to `internal`; `defaultJdkName` derived from resolved path; alias SDK registered under original wrapper-path name.

### Pending / In-Progress Work

**File event batch guard** — branch `feat/file-event-batch-guard`, PR open (→ `development-261`).

**Problem**: A `git pull` can deliver dozens of `Create`/`ExternalCreate` events simultaneously, triggering an expensive Bazel inverse-sources query that chokes IntelliJ.

**Fix** (`BazelFileEventListener.kt`): `addNewFilesToBothModels` counts new-file events with an early-exit sequence (`asSequence().filter().drop(5).any()`); if exceeded, skip processing and show the Resync notification instead. Limit is `NEW_FILE_EVENTS_LIMIT = 5`.

**Shard module elimination** — branch `feat/shard-module-elimination`, PR #34 open (→ `development-261`).

**Problem**: `java_incremental_library` splits a target into one umbrella + N shard sub-targets. Importing shards as separate IntelliJ modules caused red code and bloated the module list.

**Fix** (`AspectBazelProjectMapper.kt`, `UnsyncedTargetUpdater.kt`): Shard-tagged targets filtered before partition into workspace/non-workspace; shard deps dropped from umbrella dependency lists; `resolveShardFolkDependencies` removed; `UnsyncedTargetUpdater` skips shard-tagged targets like `no-ide`.

**Next step**: —

### Repo Context
- This is a Snowflake fork of the JetBrains Bazel IntelliJ plugin (`hirschgarten`)
- Snowflake repo: `github.com/Snowflake-Labs/hirschgarten`
- Development branch for IntelliJ 261 builds: `development-261`
- PRs are filed against `development-261` (not `main`)
- Git remote for pushing PRs: `snowflake` (points to `github.com/Snowflake-Labs/hirschgarten`)
- Use your own `sfc-gh-*` GitHub account when creating PRs
