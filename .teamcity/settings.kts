import configurations.*
import configurations.IdeStarterTests.IdeStarter
import configurations.PluginBenchmark.PluginBenchmark
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

version = "2024.12"

object ProjectBranchFilters {
  val githubBranchFilter = "+:pull/*"
}

object ProjectTriggerRules {
  val triggerRules =
    """
    -:**.md
    -:**.txt
    -:**.yml
    -:**.yaml
    -:LICENSE
    -:LICENCE
    -:CODEOWNERS
    -:/.teamcity/**
    -:/tools/**
    """.trimIndent()
}


project {
  vcsRoot(BaseConfiguration.GitHubVcs)

// setup pipeline chain for bazel-bsp
  val allSteps =
    sequential {
      // 1. Run formatter first
      // buildType(ProjectFormat.GitHub)
      // 2. Run everything else in parallel
      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
        buildType(ProjectBuild.ProjectBuild)
        buildType(ProjectUnitTests.ProjectUnitTests)
        buildType(PluginBenchmark.BenchmarkDefault)
        buildType(PluginBenchmark.BenchmarkWithVersion)
        buildType(IdeStarter.HotswapTest)
        buildType(IdeStarter.GoLandSyncTest)
        buildType(IdeStarter.CoroutineDebugTest)
        buildType(IdeStarter.ReopenWithoutResyncTest)
        buildType(IdeStarter.RunLineMarkerTest)
        buildType(IdeStarter.ExternalRepoResolveTest)
        buildType(IdeStarter.JarSourceExcludeTest)
        buildType(IdeStarter.BazelProjectModelModifierTest)
        buildType(IdeStarter.BazelCoverageTest)
        buildType(IdeStarter.TestResultsTreeTest)
        buildType(IdeStarter.ImportRunConfigurationsTest)
        buildType(IdeStarter.NonModuleTargetsTest)
        buildType(IdeStarter.RunAllTestsActionTest)
        buildType(IdeStarter.DisabledKotlinPluginTest)
        buildType(IdeStarter.PyCharmTest)
        buildType(IdeStarter.FastBuildTest)
        buildType(IdeStarter.ProjectViewOpenTest)
        buildType(IdeStarter.MoveKotlinFileTest)
        buildType(StaticAnalysis.Hirschgarten)
        buildType(StaticAnalysis.Bazel)
      }

      buildType(
        ResultsAggregator.Aggregator, options = {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      })
    }.buildTypes()

  // initialize all build steps for bazel-bsp
  allSteps.forEach { buildType(it) }


  ResultsAggregator.Aggregator.triggers {
    vcs {
      branchFilter = ProjectBranchFilters.githubBranchFilter
      triggerRules = ProjectTriggerRules.triggerRules
    }
  }

  // setup display order for bazel-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      //ProjectFormat,
      ProjectBuild.ProjectBuild,
      ProjectUnitTests.ProjectUnitTests,
      PluginBenchmark.BenchmarkDefault,
      PluginBenchmark.BenchmarkWithVersion,
      IdeStarter.HotswapTest,
      IdeStarter.GoLandSyncTest,
      IdeStarter.CoroutineDebugTest,
      IdeStarter.ReopenWithoutResyncTest,
      IdeStarter.RunLineMarkerTest,
      IdeStarter.ExternalRepoResolveTest,
      IdeStarter.JarSourceExcludeTest,
      IdeStarter.BazelProjectModelModifierTest,
      IdeStarter.BazelCoverageTest,
      IdeStarter.TestResultsTreeTest,
      IdeStarter.ImportRunConfigurationsTest,
      IdeStarter.NonModuleTargetsTest,
      IdeStarter.RunAllTestsActionTest,
      IdeStarter.DisabledKotlinPluginTest,
      IdeStarter.PyCharmTest,
      IdeStarter.FastBuildTest,
      IdeStarter.ProjectViewOpenTest,
      IdeStarter.MoveKotlinFileTest,
      StaticAnalysis.Hirschgarten,
      StaticAnalysis.Bazel,
      ResultsAggregator.Aggregator
    )
}

