Core insights for working with .teamcity (TeamCity Kotlin DSL)

Overview
- This project uses TeamCity Kotlin DSL (version "2024.12").
- CI is organized as two subprojects: GitHub and Space. Both reuse the same build definitions but run against different VCS roots.
- Keep logic DRY via factories and a shared base build type.

Layout
- Root entry: `.teamcity/settings.kts`
  - Declares VCS roots at the root scope and registers two subprojects: `GitHub` and `Space`.
  - Defines branch filters and wiring (format step → parallel builds → aggregator) per subproject.
- Build definitions: `.teamcity/configurations/*`
  - `baseConfiguration.kt` — `BaseBuildType` shared behavior (IDs, requirements, commit status, PR handling).
  - `vcsRoots.kt` — VCS roots (`VcsRootHirschgarten` for GitHub, `VcsRootHirschgartenSpace` for Space, plus Qodana repos).
  - `utils.kt` — `DockerParams`, `CommonParams` (Bazel args, images), and `CredentialsStore`.
  - `pluginBuild.kt` — plugin builds + factory (`ForAllPlatforms`, `ForAllPlatformsSpace`).
  - `projectUnitTests.kt` — unit tests build type (class; pass VCS root).
  - `pluginBenchmark.kt` — benchmark tests + factory (GitHub/Space variants).
  - `ideStarterTests.kt` — IDE-starter tests + factory (GitHub/Space variants).
  - `staticAnalysis.kt` — Qodana analysis builds; GitHub/Space sets and VCS-aware deps on latest platform build.
  - `resultsAggregator.kt` — composite aggregator builds per subproject.
  - `projectFormat.kt` — `FormatBuildFactory` produces the formatting build for GitHub/Space.

VCS Roots & Credentials
- GitHub: `VcsRootHirschgarten` uses `tc-cloud-github-connection` and `CredentialsStore.GitHubPassword`.
- Space: `VcsRootHirschgartenSpace` uses `PROJECT_EXT_15` and `CredentialsStore.SpaceToken`.
- Add new secrets as credential references in `CredentialsStore`; do not hardcode values.

Subprojects & Pipelines
- `GitHub` subproject
  - Steps: `FormatBuildFactory.GitHub` → parallel (plugin builds, unit tests, benchmarks, IDE-starter, GH analyses) → `Aggregator`.
  - PR checks and commit statuses configured for GitHub.
- `Space` subproject
  - Steps: `FormatBuildFactory.Space` → parallel (Space builds/tests/analyses) → `AggregatorSpace`.
  - Commit status publisher uses Space connection; no PR provider.

Build Types & Factories
- Always try to derive new builds from `BaseBuildType`.
- Target a VCS by passing `customVcsRoot`:
  - GitHub: default (or `customVcsRoot = VcsRootHirschgarten`).
  - Space: `customVcsRoot = VcsRootHirschgartenSpace`.
- IDs are auto-prefixed (`GitHub…`/`Space…`) by `BaseBuildType`; avoid manual `id()` overrides.
- To add platform builds, extend factories rather than wiring items manually.

Static Analysis (Qodana)
- Add an `AnalysisDef` with target `vcsRoot`, cloud token key, and credentials.
- `EnabledAnalysisTestsGitHub`/`EnabledAnalysisTestsSpace` control inclusion in pipelines.
- Each analysis depends on the latest platform build matching its VCS and mounts built plugin artifacts.

Triggers
- GitHub formatting uses a PR-based VCS trigger with selective rules.
- Space formatting uses a branch-based VCS trigger (`<default>`, all branches, excludes steward/merge refs).
- Aggregators use `finishBuildTrigger` on the corresponding format build.

Adding A New Platform
- Update `CommonParams.CrossBuildPlatforms` in `utils.kt`.
- Factories (`PluginBuildFactory`, `PluginBenchmarkFactory`, `IdeStarterTestFactory`, static analysis selection) will include new entries automatically.

Validation
- From `.teamcity`, run: `mvn teamcity-configs:generate -f pom.xml`.
- Ensure generation succeeds locally before pushing.

Gotchas
- Name collisions: identical display names are fine across subprojects; avoid placing both GH and Space builds in the same project.
- Agent sizing: `BaseBuildType` sets defaults; heavy builds override to Large/XLarge as needed.
- Commit status/PR provider: configured only for GitHub builds; Space uses the connection publisher.
