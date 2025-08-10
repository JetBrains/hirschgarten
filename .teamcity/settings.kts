import configurations.*
import jetbrains.buildServer.configs.kotlin.v10.toExtId
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
        buildType(ProjectBuild.GitHub)
        buildType(ProjectUnitTests.GitHub)
        buildType(PluginBenchmark.BenchmarkDefaultGitHub)
        buildType(PluginBenchmark.BenchmarkWithVersionGitHub)
        buildType(IdeStarterTests.HotswapTestGitHub)
        buildType(IdeStarterTests.GoLandSyncTestGitHub)
        buildType(IdeStarterTests.CoroutineDebugTestGitHub)
        buildType(IdeStarterTests.ReopenWithoutResyncTestGitHub)
        buildType(IdeStarterTests.RecoverDotBazelBspTestGitHub)
        buildType(IdeStarterTests.RunLineMarkerTestGitHub)
        buildType(IdeStarterTests.ExternalRepoResolveTestGitHub)
        buildType(IdeStarterTests.JarSourceExcludeTestGitHub)
        buildType(IdeStarterTests.BazelProjectModelModifierTestGitHub)
        buildType(IdeStarterTests.BazelCoverageTestGitHub)
        buildType(IdeStarterTests.TestResultsTreeTestGitHub)
        buildType(IdeStarterTests.ImportRunConfigurationsTestGitHub)
        buildType(IdeStarterTests.NonModuleTargetsTestGitHub)
        buildType(IdeStarterTests.RunAllTestsActionTestGitHub)
        buildType(IdeStarterTests.DisabledKotlinPluginTestGitHub)
        buildType(IdeStarterTests.PyCharmTestGitHub)
        buildType(IdeStarterTests.FastBuildTestGitHub)
        buildType(IdeStarterTests.ProjectViewOpenTestGitHub)
        buildType(IdeStarterTests.MoveKotlinFileTestGitHub)
        buildType(ServerE2eTests.SampleRepoGitHub)
        buildType(ServerE2eTests.LocalJdkGitHub)
        buildType(ServerE2eTests.RemoteJdkGitHub)
//        buildType(ServerE2eTests.AndroidProjectGitHub)
//        buildType(ServerE2eTests.AndroidKotlinProjectGitHub)
        buildType(ServerE2eTests.ScalaProjectGitHub)
        buildType(ServerE2eTests.KotlinProjectGitHub)
        buildType(ServerE2eTests.GoProjectGitHub)
        buildType(ServerE2eTests.PythonProjectGitHub)
        buildType(ServerE2eTests.JavaDiagnosticsGitHub)
        buildType(ServerE2eTests.ManualTargetsGitHub)
        buildType(ServerE2eTests.BuildSyncGitHub)
        buildType(ServerE2eTests.FirstPhaseSyncGitHub)
        buildType(ServerE2eTests.PartialSyncGitHub)
        buildType(ServerE2eTests.ExternalAutoloadsGitHub)
        buildType(ServerE2eTests.NestedModulesGitHub)
        buildType(StaticAnalysis.HirschgartenGitHub)
//        buildType(StaticAnalysis.AndroidBazelRulesGitHub)
//        buildType(StaticAnalysis.AndroidTestdpcGitHub)
        buildType(StaticAnalysis.BazelGitHub)
//        buildType(StaticAnalysis.JetpackComposeGitHub)
      }

      buildType(ResultsAggregator.GitHub, options = {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      })
    }.buildTypes()

  // initialize all build steps for bazel-bsp
  allSteps.forEach { buildType(it) }

  // setup trigger for bazel-bsp pipeline
//  ProjectFormat.GitHub.triggers {
//    vcs {
//      branchFilter = ProjectBranchFilters.githubBranchFilter
//      triggerRules = ProjectTriggerRules.triggerRules
//    }
//  }

//  ResultsAggregator.GitHub.triggers {
//    finishBuildTrigger {
//      buildType = "${ProjectFormat.GitHub.id}"
//      successfulOnly = true
//      branchFilter = ProjectBranchFilters.githubBranchFilter
//    }
//  }

  ResultsAggregator.GitHub.triggers {
    vcs {
      branchFilter = ProjectBranchFilters.githubBranchFilter
      triggerRules = ProjectTriggerRules.triggerRules
    }
  }

  // setup display order for bazel-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      //ProjectFormat.GitHub,
      ProjectBuild.GitHub,
      ProjectUnitTests.GitHub,
      PluginBenchmark.BenchmarkDefaultGitHub,
      PluginBenchmark.BenchmarkWithVersionGitHub,
      IdeStarterTests.HotswapTestGitHub,
      IdeStarterTests.GoLandSyncTestGitHub,
      IdeStarterTests.CoroutineDebugTestGitHub,
      IdeStarterTests.ReopenWithoutResyncTestGitHub,
      IdeStarterTests.RecoverDotBazelBspTestGitHub,
      IdeStarterTests.RunLineMarkerTestGitHub,
      IdeStarterTests.ExternalRepoResolveTestGitHub,
      IdeStarterTests.JarSourceExcludeTestGitHub,
      IdeStarterTests.BazelProjectModelModifierTestGitHub,
      IdeStarterTests.BazelCoverageTestGitHub,
      IdeStarterTests.TestResultsTreeTestGitHub,
      IdeStarterTests.ImportRunConfigurationsTestGitHub,
      IdeStarterTests.NonModuleTargetsTestGitHub,
      IdeStarterTests.RunAllTestsActionTestGitHub,
      IdeStarterTests.DisabledKotlinPluginTestGitHub,
      IdeStarterTests.PyCharmTestGitHub,
      IdeStarterTests.FastBuildTestGitHub,
      IdeStarterTests.ProjectViewOpenTestGitHub,
      IdeStarterTests.MoveKotlinFileTestGitHub,
      ServerE2eTests.SampleRepoGitHub,
      ServerE2eTests.LocalJdkGitHub,
      ServerE2eTests.RemoteJdkGitHub,
//      ServerE2eTests.AndroidProjectGitHub,
//      ServerE2eTests.AndroidKotlinProjectGitHub,
      ServerE2eTests.ScalaProjectGitHub,
      ServerE2eTests.KotlinProjectGitHub,
      ServerE2eTests.GoProjectGitHub,
      ServerE2eTests.PythonProjectGitHub,
      ServerE2eTests.JavaDiagnosticsGitHub,
      ServerE2eTests.ManualTargetsGitHub,
      ServerE2eTests.BuildSyncGitHub,
      ServerE2eTests.FirstPhaseSyncGitHub,
      ServerE2eTests.PartialSyncGitHub,
      ServerE2eTests.NestedModulesGitHub,
      ServerE2eTests.ExternalAutoloadsGitHub,
      StaticAnalysis.HirschgartenGitHub,
//      StaticAnalysis.AndroidBazelRulesGitHub,
//      StaticAnalysis.AndroidTestdpcGitHub,
      StaticAnalysis.BazelGitHub,
//      StaticAnalysis.JetpackComposeGitHub,
      ResultsAggregator.GitHub
    )
}

