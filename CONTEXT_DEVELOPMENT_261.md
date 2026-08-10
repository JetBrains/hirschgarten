# Context for Next Task

> **Maintenance tip**: Keep this file updated as you make progress — update the tip commit hash,
> move completed work into the "Recently Completed" section, and revise the "Pending" section.
> This file is the primary handoff document between sessions.

## Current State (as of 2026-08-10)

### Working Branch
`development-261` — latest tip: `98bc05a260 Merge pull request #30`

### Recently Completed Work (PR #29)

**Branch**: `fix/add-java-source-root-properties-on-file-add` → merged into `development-261`

Two commits in this PR:
1. `18baa6588b` — Fix missing `JavaSourceRootPropertiesEntity` when adding new files to modules
2. `20e33fe445` — Batch unsynced target RPCs: N calls → 1 for new file sync

#### What was fixed

**Bug**: When a new `.java`/`.kt` file was added to a Bazel target:
- No code intelligence until file was closed and reopened
- Alt+Enter import resolution failed for other files importing from the new class
- Root cause: `addToModule()` created a `SourceRootEntity` but never attached a `JavaSourceRootPropertiesEntity` with `packagePrefix` — IntelliJ's `SingleFileSourcesTrackerImpl.getPackageNameForSingleFileSource()` returned null without it

**Deduplication**: Removed the dead `AssignFileToModuleListener` class (it was not registered in `plugin.xml` — upstream had already moved functionality to `BazelFileEventListener`). Kept utility functions in a new `ModuleAssignmentUtils.kt`.

**Optimization**: `addFileToTargets()` in `BazelFileEventListener` was calling `fetchAndCacheUnsyncedTarget()` once per unsynced target (N individual BSP RPCs). Refactored into a 3-phase approach: collect all unsynced labels → single batch RPC via `fetchAndCacheUnsyncedTargets()` → add files to modules.

#### Key files changed
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace/ModuleAssignmentUtils.kt` (new, replaces `AssignFileToModuleListener.kt`)
  - `addToModule()` — now creates `JavaSourceRootPropertiesEntity` with `packagePrefix`
  - `resolvePackagePrefix()` — looks up `PackageMarkerEntity` for the parent directory
  - `getModulesForFile()`, `askForInverseSources()`, `processTargetsForTestlibStripping()`
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace/AssignFileToModuleListener.kt` (deleted)
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace/UnsyncedTargetUpdater.kt`
  - Added `fetchAndCacheUnsyncedTargets(labels: List<Label>, ...)` — batch BSP call via `WorkspaceBuildTargetSelector.SpecificTargets(labels)`
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace/fileEvents/BazelFileEventListener.kt`
  - `addFileToTargets()` refactored: 3-phase batch approach
  - Added `enqueueExternalEvents()` companion method (internal) for `BazelFileEventSubmitter`
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace/BazelFileEventSubmitter.kt` — routes to `BazelFileEventListener.enqueueExternalEvents()`
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspace/fileEvents/SimplifiedFileEvent.kt` — added `ExternalCreate` subclass
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/action/registered/AddFileToModuleAction.kt` — passes `packagePrefix` to `addToModule()`

### Recently Completed Work (PR #30)

**Branch**: `context/update-development-261-context` → merged into `development-261`

Added `CONTEXT_DEVELOPMENT_261.md` as a persistent handoff document between sessions, then made the GitHub account note generic.

### Pending / In-Progress Work

**JVM Wrapper JDK resolution** — branch `fix/resolve-jvm-wrapper-jdk-home`, PR #31 open (→ `development-261`).

**Problem**: When a project uses `jvm_wrapper_runtime`, the `java_home` from the Bazel aspect points to the wrapper directory (contains only `bin/java` as a shell script), not the actual JDK. IntelliJ's `JavaSdk.isValidSdkHome()` fails for this path → "JDK not found on disk or corrupted" warning after sync.

**Fix implemented** (on `fix/resolve-jvm-wrapper-jdk-home`, rebased onto `development-261`):
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/jvm/sync/SdkUtils.kt`
  - `addJdkIfNeeded()` now calls `resolveJavaHome()` before creating the SDK
  - `resolveJavaHome()`: fast-path returns early if already a valid JDK; otherwise tries:
    1. Parse `{javaHome}/bin/java` wrapper script for `exec .../bin/java` lines, resolve against Bazel exec root
    2. Scan `execroot/_main/external/` for JDK directories, prefer those matching the version hint in the wrapper name
  - Helpers: `resolveRealJdkFromWrapper()`, `findJdkInExternalDir()`, `findExecRoot()`

**Next step**: —

### Repo Context
- This is a Snowflake fork of the JetBrains Bazel IntelliJ plugin (`hirschgarten`)
- Snowflake repo: `github.com/Snowflake-Labs/hirschgarten`
- Development branch for IntelliJ 261 builds: `development-261`
- PRs are filed against `development-261` (not `main`)
- Git remote for pushing PRs: `snowflake` (points to `github.com/Snowflake-Labs/hirschgarten`)
- Use your own `sfc-gh-*` GitHub account when creating PRs
