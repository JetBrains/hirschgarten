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


  val ProjectUnitTestsGithub = ProjectUnitTests()

  val allSteps = sequential {
    parallel() {
      PluginBuildFactory.ForAllPlatforms.forEach { buildType(it) }
      buildType(ProjectUnitTestsGithub)
      PluginBenchmarkFactory.AllBenchmarkTests.forEach { buildType(it) }
      IdeStarterTestFactory.AllIdeStarterTests.forEach { buildType(it) }
      StaticAnalysisFactory.EnabledAnalysisTestsGitHub.forEach { buildType(it) }
    }
    buildType(Aggregator, options = {
      onDependencyFailure = FailureAction.ADD_PROBLEM
      onDependencyCancel = FailureAction.ADD_PROBLEM
    })
  }.buildTypes()

  allSteps.forEach { buildType(it) }

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


  val ProjectUnitTestsSpace = ProjectUnitTests(customVcsRoot = VcsRootHirschgartenSpace)

  val allSteps = sequential {
    parallel() {
      PluginBuildFactory.ForAllPlatformsSpace.forEach { buildType(it) }
      buildType(ProjectUnitTestsSpace)
      PluginBenchmarkFactory.AllBenchmarkTestsSpace.forEach { buildType(it) }
      IdeStarterTestFactory.AllIdeStarterTestsSpace.forEach { buildType(it) }
      StaticAnalysisFactory.EnabledAnalysisTestsSpace.forEach { buildType(it) }
    }
    buildType(AggregatorSpace, options = {
      onDependencyFailure = FailureAction.ADD_PROBLEM
      onDependencyCancel = FailureAction.ADD_PROBLEM
    })
  }.buildTypes()

  allSteps.forEach { buildType(it) }

  buildTypesOrderIds = arrayListOf(
    *PluginBuildFactory.ForAllPlatformsSpace.toTypedArray(),
    ProjectUnitTestsSpace,
    *PluginBenchmarkFactory.AllBenchmarkTestsSpace.toTypedArray(),
    *IdeStarterTestFactory.AllIdeStarterTestsSpace.toTypedArray(),
    *StaticAnalysisFactory.EnabledAnalysisTestsSpace.toTypedArray(),
    AggregatorSpace
  )
})
