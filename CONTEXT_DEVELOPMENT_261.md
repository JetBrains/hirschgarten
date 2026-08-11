# Context for Next Task

> **Maintenance tip**: Keep this file updated as you make progress — update the tip commit hash,
> move completed work into the "Recently Completed" section, and revise the "Pending" section.
> This file is the primary handoff document between sessions.
>
> **Next step guidance**: Only write a "Next step" when there is something actionable *after* a PR
> merges. Don't write "get PR reviewed/merged" — this doc lives in the repo and is read post-merge,
> so those entries are immediately stale. Use `—` or omit the field when nothing remains.

## Current State (as of 2026-08-11)

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

### Recently Completed Work (PR #30)

**Branch**: `context/update-development-261-context` → merged into `development-261`

Added `CONTEXT_DEVELOPMENT_261.md` as a persistent handoff document.

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

---

**Non-Bazel Python directories** — branch `feat/non-bazel-python-directories`, PR #32 open (→ `development-261`).

**Problem**: Python files not covered by any Bazel target inherit the Java SDK → no Python code intelligence, IDE freezes.

**Fix implemented** (on `feat/non-bazel-python-directories`):
- New `.bazelproject` section `non_bazel_python_directories:` — lists directories explicitly
- `NonBazelPythonDirectoriesSection.kt` (new) — parses the section as `List<Path>`
- `ProjectViewExtensions.kt` — adds `ProjectView.nonBazelPythonDirectories` extension property
- `PythonProjectSync.kt` — `createFallbackPythonModuleIfNeeded()` replaced: reads the explicit list, resolves relative paths against workspace root, creates one module per listed directory with a Python SDK; falls back to `findPythonSdk()` when no Python Bazel targets exist

Example `.bazelproject` usage:
```
non_bazel_python_directories:
  Snowfort
  tools/scripts
```

**Next step**: —

### Repo Context
- This is a Snowflake fork of the JetBrains Bazel IntelliJ plugin (`hirschgarten`)
- Snowflake repo: `github.com/Snowflake-Labs/hirschgarten`
- Development branch for IntelliJ 261 builds: `development-261`
- PRs are filed against `development-261` (not `main`)
- Git remote for pushing PRs: `snowflake` (points to `github.com/Snowflake-Labs/hirschgarten`)
- Use your own `sfc-gh-*` GitHub account when creating PRs
