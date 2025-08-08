import configurations.*
import configurations.IdeStarterTests.IdeStarterTestFactory
import configurations.PluginBenchmark.PluginBenchmarkFactory
import configurations.ProjectFormat.CheckFormating
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
       buildType(CheckFormating)
      // 2. Run everything else in parallel
      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
        // Add all platform builds from factory
        PluginBuild.Factory.ForAllPlatforms.forEach { buildType(it) }
        buildType(ProjectUnitTests.ProjectUnitTests)
        // Add all benchmark tests from factory
        PluginBenchmarkFactory.AllBenchmarkTests.forEach { buildType(it) }
        // Add all IDE starter tests from factory
        IdeStarterTestFactory.AllIdeStarterTests.forEach { buildType(it) }
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
      CheckFormating,
      *PluginBuild.Factory.ForAllPlatforms.toTypedArray(),
      ProjectUnitTests.ProjectUnitTests,
      *PluginBenchmarkFactory.AllBenchmarkTests.toTypedArray(),
      *IdeStarterTestFactory.AllIdeStarterTests.toTypedArray(),
      StaticAnalysis.Hirschgarten,
      StaticAnalysis.Bazel,
      ResultsAggregator.Aggregator
    )
}

