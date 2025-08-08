import configurations.*
import configurations.IdeStarterTests.IdeStarterTestFactory
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
      //ProjectFormat,
      ProjectBuild.ProjectBuild,
      ProjectUnitTests.ProjectUnitTests,
      PluginBenchmark.BenchmarkDefault,
      PluginBenchmark.BenchmarkWithVersion,
      *IdeStarterTestFactory.AllIdeStarterTests.toTypedArray(),
      StaticAnalysis.Hirschgarten,
      StaticAnalysis.Bazel,
      ResultsAggregator.Aggregator
    )
}

