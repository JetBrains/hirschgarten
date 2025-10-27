import configurations.*
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project

version = "2024.12"

project {
  // Expose VCS roots at the root project scope
  vcsRoot(VcsRootHirschgarten)
  vcsRoot(VcsRootHirschgartenSpace)
  vcsRoot(VcsRootBazelQodana)
  vcsRoot(VcsRootBuildBuddyQodana)

  subProject(GitHub)
  subProject(Space)
}

object GitHub : Project({
  name = "GitHub"

  // Register pipeline build types explicitly
  val ProjectUnitTestsGithub = ProjectUnitTests()
  val buildsGithub = PluginBuildFactory.ForAllPlatforms
  val benchmarksGithub = PluginBenchmarkFactory.AllBenchmarkTests
  val ideStarterGithub = IdeStarterTestFactory.AllIdeStarterTests
  val analysesGitHub = StaticAnalysisFactory.EnabledAnalysisTestsGitHub

  // Activate builds in the project
  buildsGithub.forEach { buildType(it) }
  buildType(ProjectUnitTestsGithub)
  benchmarksGithub.forEach { buildType(it) }
  ideStarterGithub.forEach { buildType(it) }
  analysesGitHub.forEach { buildType(it) }

  // Assemble pipeline list separately for dependency wiring
  val pipelineGithub: List<BuildType> =
    buildsGithub + ProjectUnitTestsGithub + benchmarksGithub + ideStarterGithub + analysesGitHub
  
  // Add Aggregator and wire dependencies with per-dependency failure behavior
  buildType(Aggregator)

  val qodanaBazelGitHub = analysesGitHub.firstOrNull { it.name.contains("Qodana Bazel") }

  Aggregator.apply {
    dependencies {
      pipelineGithub.forEach { dep ->
        snapshot(dep) {
          // Default: add problem on failure/failed-to-start
          onDependencyFailure = FailureAction.ADD_PROBLEM
          onDependencyCancel = FailureAction.ADD_PROBLEM
          // Special-case: ignore Bazel Qodana failures in Results
          if (qodanaBazelGitHub != null && dep.id == qodanaBazelGitHub.id) {
            onDependencyFailure = FailureAction.IGNORE
            onDependencyCancel = FailureAction.IGNORE
          }
        }
      }
    }
  }

  buildTypesOrderIds = arrayListOf(
    *PluginBuildFactory.ForAllPlatforms.toTypedArray(),
    ProjectUnitTestsGithub,
    *PluginBenchmarkFactory.AllBenchmarkTests.toTypedArray(),
    *IdeStarterTestFactory.AllIdeStarterTests.toTypedArray(),
    *StaticAnalysisFactory.EnabledAnalysisTestsGitHub.toTypedArray(),
    Aggregator
  )
})

object Space : Project({
  name = "Space"

  // Register pipeline build types explicitly
  val ProjectUnitTestsSpace = ProjectUnitTests(customVcsRoot = VcsRootHirschgartenSpace)
  val buildsSpace = PluginBuildFactory.ForAllPlatformsSpace
  val benchmarksSpace = PluginBenchmarkFactory.AllBenchmarkTestsSpace
  val ideStarterSpace = IdeStarterTestFactory.AllIdeStarterTestsSpace
  val analysesSpace = StaticAnalysisFactory.EnabledAnalysisTestsSpace

  // Activate builds in the project
  buildsSpace.forEach { buildType(it) }
  buildType(ProjectUnitTestsSpace)
  benchmarksSpace.forEach { buildType(it) }
  ideStarterSpace.forEach { buildType(it) }
  analysesSpace.forEach { buildType(it) }

  // Assemble pipeline list separately for dependency wiring
  val pipelineSpace: List<BuildType> =
    buildsSpace + ProjectUnitTestsSpace + benchmarksSpace + ideStarterSpace + analysesSpace

  // Add AggregatorSpace and wire dependencies; ignore Bazel Qodana failures
  buildType(AggregatorSpace)

  val qodanaBazelSpace = analysesSpace.firstOrNull { it.name.contains("Qodana Bazel") }

  AggregatorSpace.apply {
    dependencies {
      pipelineSpace.forEach { dep ->
        snapshot(dep) {
          onDependencyFailure = FailureAction.ADD_PROBLEM
          onDependencyCancel = FailureAction.ADD_PROBLEM
          if (qodanaBazelSpace != null && dep.id == qodanaBazelSpace.id) {
            onDependencyFailure = FailureAction.IGNORE
            onDependencyCancel = FailureAction.IGNORE
          }
        }
      }
    }
  }

  buildTypesOrderIds = arrayListOf(
    *PluginBuildFactory.ForAllPlatformsSpace.toTypedArray(),
    ProjectUnitTestsSpace,
    *PluginBenchmarkFactory.AllBenchmarkTestsSpace.toTypedArray(),
    *IdeStarterTestFactory.AllIdeStarterTestsSpace.toTypedArray(),
    *StaticAnalysisFactory.EnabledAnalysisTestsSpace.toTypedArray(),
    AggregatorSpace
  )
})
