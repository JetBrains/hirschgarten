import configurations.*
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
  vcsRoot(VcsRootHirschgarten)
  vcsRoot(VcsRootBazelQodana)
  vcsRoot(VcsRootBuildBuddyQodana)

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
        PluginBuildFactory.ForAllPlatforms.forEach { buildType(it) }
        buildType(ProjectUnitTests)
        // Add all benchmark tests from factory
        PluginBenchmarkFactory.AllBenchmarkTests.forEach { buildType(it) }
        // Add all IDE starter tests from factory
        IdeStarterTestFactory.AllIdeStarterTests.forEach { buildType(it) }
        // Add only enabled static analysis tests from factory
        StaticAnalysisFactory.EnabledAnalysisTests.forEach { buildType(it) }
      }

      buildType(
        Aggregator, options = {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      })
    }.buildTypes()

  // initialize all build steps for bazel-bsp
  allSteps.forEach { buildType(it) }


  Aggregator.triggers {
    vcs {
      branchFilter = ProjectBranchFilters.githubBranchFilter
      triggerRules = ProjectTriggerRules.triggerRules
    }
  }

  // setup display order for bazel-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      CheckFormating,
      *PluginBuildFactory.ForAllPlatforms.toTypedArray(),
      ProjectUnitTests,
      *PluginBenchmarkFactory.AllBenchmarkTests.toTypedArray(),
      *IdeStarterTestFactory.AllIdeStarterTests.toTypedArray(),
      *StaticAnalysisFactory.EnabledAnalysisTests.toTypedArray(),
      Aggregator
    )
}

