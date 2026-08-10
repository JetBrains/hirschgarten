# Context for Next Task

## Current State (as of 2026-08-10)

### Working Branch
`development-261` — latest tip: `260af4c882 Merge pull request #29`

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

### Pending / In-Progress Work

**JVM Wrapper JDK resolution** — separate branch `fix/resolve-jvm-wrapper-jdk-home`, NOT yet merged, NO PR yet.

**Problem**: When a project uses `jvm_wrapper_runtime`, the `java_home` from the Bazel aspect points to the wrapper directory (contains only `bin/java` as a shell script), not the actual JDK. IntelliJ's `JavaSdk.isValidSdkHome()` fails for this path → "JDK not found on disk or corrupted" warning after sync.

**Fix implemented** (on `fix/resolve-jvm-wrapper-jdk-home`):
- `plugin-bazel/src/main/kotlin/org/jetbrains/bazel/jvm/sync/SdkUtils.kt`
  - Added `resolveJavaHome(javaHome: Path, project: Project): Path`
  - If `javaHome` is not a valid JDK: reads `bin/java` as text, parses `exec` lines to find real java binary relative to Bazel exec root, derives actual JDK home
  - Helpers: `resolveRealJdkFromWrapper()`, `findJdkInExternalDir()`, `findExecRoot()`
- Note: this fix is on a REMOTE machine; `jvm_wrapper_runtime` creates a `java_runtime` with `java_home` pointing to wrapper dir (`jdk21_jvm_wrapper_wrapper_script`)

**Next step**: Create a PR from `fix/resolve-jvm-wrapper-jdk-home` → `development-261`

### Repo Context
- This is a Snowflake fork of the JetBrains Bazel IntelliJ plugin (`hirschgarten`)
- Snowflake repo: `github.com/Snowflake-Labs/hirschgarten`
- Development branch for IntelliJ 261 builds: `development-261`
- PRs are filed against `development-261` (not `main`)
- Git remote for pushing PRs: `snowflake`
- GitHub account for PRs: `sfc-gh-gguo`
