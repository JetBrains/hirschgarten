# Context for Next Task

> **Maintenance tip**: Keep this file updated as you make progress — update the tip commit hash,
> move completed work into the "Recently Completed" section, and revise the "Pending" section.
> This file is the primary handoff document between sessions.
>
> **Next step guidance**: Only write a "Next step" when there is something actionable *after* a PR
> merges. Don't write "get PR reviewed/merged" — this doc lives in the repo and is read post-merge,
> so those entries are immediately stale. Use `—` or omit the field when nothing remains.

## Current State (as of 2026-08-12)

### Working Branch
`development-261` — latest tip: `669b674a0b Merge pull request #32`

### Recently Completed Work (PR #31)

**Branch**: `fix/resolve-jvm-wrapper-jdk-home` → merged into `development-261`

**Problem**: When a project uses `jvm_wrapper_runtime`, the `java_home` from the Bazel aspect points to the wrapper directory (contains only `bin/java` as a shell script), not the actual JDK. IntelliJ's `JavaSdk.isValidSdkHome()` fails → "JDK not found on disk or corrupted" warning after sync.

**Fix** (`SdkUtils.kt`): `addJdkIfNeeded()` now calls `resolveJavaHome()` before creating the SDK. `resolveJavaHome()` tries two strategies: (1) parse `bin/java` wrapper script for `exec .../bin/java` lines and resolve against Bazel exec root; (2) scan `execroot/_main/external/` for JDK directories.

### Recently Completed Work (PR #32)

**Branch**: `feat/non-bazel-python-directories` → merged into `development-261`

**Problem**: Python files not covered by any Bazel target inherit the Java SDK → no Python code intelligence.

**Fix**: New `.bazelproject` section `non_bazel_python_directories:` — explicitly list directories. The plugin creates one IntelliJ module per listed directory with a Python SDK. Bazel-covered directories are skipped to avoid duplicate modules.

### Pending / In-Progress Work

**JVM wrapper project SDK regression** — branch `fix/jvm-wrapper-project-sdk`, PR #33 open (→ `development-261`).

**Problem**: PR #31 introduced two regressions:
1. The project SDK is reported as "not configured" — `defaultJdkName` hashed the raw wrapper path but the SDK was registered under the resolved path; the names diverged so `setProjectSdk` couldn't find it.
2. Target modules showed red code — `ModuleDetailsToJavaModuleTransformer` computes `jvmJdkName` from the raw wrapper path, but after PR #31 only the resolved-path SDK was registered, so the module SDK dependency resolved to a non-existent SDK.

**Fix** (on `fix/jvm-wrapper-project-sdk`):
- `SdkUtils.resolveJavaHome()` widened from `private` to `internal`
- `CollectProjectDetailsTask.kt`: `defaultJdkName` now derived from `SdkUtils.resolveJavaHome(it.first())` (resolved path) — fixes regression #1
- `SdkUtils.addJdkIfNeeded()`: when `resolvedHome != javaHome`, also registers an alias SDK under the original wrapper-path name pointing to the same real JDK — fixes regression #2

**Next step**: —

### Repo Context
- This is a Snowflake fork of the JetBrains Bazel IntelliJ plugin (`hirschgarten`)
- Snowflake repo: `github.com/Snowflake-Labs/hirschgarten`
- Development branch for IntelliJ 261 builds: `development-261`
- PRs are filed against `development-261` (not `main`)
- Git remote for pushing PRs: `snowflake` (points to `github.com/Snowflake-Labs/hirschgarten`)
- Use your own `sfc-gh-*` GitHub account when creating PRs
