Core insights for working with .teamcity (TeamCity Kotlin DSL)

Overview
- Uses TeamCity Kotlin DSL (version "2024.12").
- CI is organized as two subprojects: GitHub and Space. Both reuse the same build definitions but run against different VCS roots.
- Logic is kept DRY via factories and a shared base build type.

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
- `staticAnalysis.kt` — Qodana analysis builds; GitHub/Space sets and VCS-aware deps on latest platform build. Bazel Qodana is currently non-blocking at the Results level (composite dependency ignores failures).
  - `resultsAggregator.kt` — composite aggregator builds per subproject.
  - `projectFormat.kt` — `FormatBuildFactory` produces the formatting build for GitHub/Space.

VCS Roots & Credentials
- GitHub: `VcsRootHirschgarten` uses `tc-cloud-github-connection` and `CredentialsStore.GitHubPassword`.
- Space: `VcsRootHirschgartenSpace` uses `PROJECT_EXT_15` and `CredentialsStore.SpaceToken`.
- Add new secrets as credential references in `CredentialsStore`; do not hardcode values.

Subprojects & Pipelines
- `GitHub` subproject
  - Steps: `FormatBuildFactory.GitHub` → parallel (plugin builds, unit tests, benchmarks, IDE-starter, GH analyses) → `Aggregator`.
  - Commit status + PR provider are enabled for GitHub-like VCS roots (including Bazel/BuildBuddy Qodana builds).
  - `Space` subproject
  - Steps: `FormatBuildFactory.Space` → parallel (Space builds/tests/analyses) → `AggregatorSpace`.
  - Commit status publisher uses Space connection.

Build Types & Factories
- Always try to derive new builds from `BaseBuildType`.
- Target a VCS by passing `customVcsRoot`:
  - GitHub: default (or `customVcsRoot = VcsRootHirschgarten`).
  - Space: `customVcsRoot = VcsRootHirschgartenSpace`.
- IDs are auto-prefixed (`GitHub…`/`Space…`). Factories for analyses pass `idNamespace` explicitly to keep build IDs under the correct subproject even when `customVcsRoot` is a third-party GitHub repo (e.g., Bazel/BuildBuddy Qodana).
- To add platform builds, extend factories rather than wiring items manually.

Static Analysis (Qodana)
- Definitions live in `staticAnalysis.kt` as `AnalysisDef` entries.
  - Required: `name`, `vcsRoot`, `cloudTokenKey`, `cloudTokenCredentials`.
  - Optional: `allowFailure` (non-blocking), `unchanged`/`diff` (enables post-check with `scripts/evaluate_qodana.py`), `linterImage`, `qodanaConfig`, `qodanaBaseline`.
- Inclusion in pipelines is controlled by `EnabledAnalysisTestsGitHub`/`EnabledAnalysisTestsSpace`. Selection is based on the `vcsRoot`:
  - GitHub set: all enabled analyses except the Space VCS.
  - Space set: Space, plus Bazel and BuildBuddy analyses.
- Bazel Qodana is non-blocking via Results: the `Results` composite build depends on Bazel Qodana with failure action set to ignore, so its failures don’t break pipelines. (The analysis build also sets `allowFailure`, but the decisive behavior is in the composite dependency.)
- Plugin mounting: artifacts from the latest platform build are mounted into Qodana with `additionalDockerArguments` and a preparatory unzip step. Artifact rules come from `CommonParams.QodanaArtifactRules`.
- Linter image: pulled from `CommonParams.DockerQodana*Image` and tagged as `20XY.Z-nightly`, where `XY.Z` is derived from the last entry of `CommonParams.CrossBuildPlatforms` (currently `252` → `2025.2-nightly`).
- Hirschgarten analyses use repo-local config/baseline paths: `tools/qodana/qodana.yaml` and `tools/qodana/qodana.sarif.json`.
- Optional anomaly check: when `analysisDef.unchanged` is set, a Python step runs `scripts/evaluate_qodana.py` to validate “UNCHANGED/NEW” problem counts from the build log.

Triggers
- No VCS/dependency triggers are currently configured in DSL. Pipelines are defined and ordered; add VCS or finish-build triggers to `Format`/`Aggregator` if automation is needed.

Adding A New Platform
- Update `CommonParams.CrossBuildPlatforms` in `utils.kt`.
- Factories (`PluginBuildFactory`, `PluginBenchmarkFactory`, `IdeStarterTestFactory`, static analysis selection) will include new entries automatically.

Validation
- From `.teamcity`, run: `mvn teamcity-configs:generate -f pom.xml`.
- Ensure generation succeeds locally before pushing.

Gotchas
- Name collisions: identical display names are fine across subprojects; avoid placing both GH and Space builds in the same project.
- Agent sizing: `BaseBuildType` sets defaults; heavy builds override to Large/XLarge as needed.
- Commit status/PR provider: enabled for GitHub-like VCS roots (including Bazel/BuildBuddy Qodana); Space uses the Space connection publisher.

Notes
- Docker registry login for Qodana images uses `PROJECT_EXT_3`.
- `BaseBuildType` injects an initial step to set `env.CONTAINER_UID/GID` for Dockerized steps.
- Cross-platform builds are controlled by `CommonParams.CrossBuildPlatforms` (currently `252`).
