import configurations.*
import configurations.IdeStarterTests.IdeStarterTests
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
        buildType(IdeStarterTests.GoLandSyncTest)
        buildType(IdeStarterTests.CoroutineDebugTest)
        buildType(IdeStarterTests.ReopenWithoutResyncTest)
        buildType(IdeStarterTests.RunLineMarkerTest)
        buildType(IdeStarterTests.ExternalRepoResolveTest)
        buildType(IdeStarterTests.JarSourceExcludeTest)
        buildType(IdeStarterTests.BazelProjectModelModifierTest)
        buildType(IdeStarterTests.BazelCoverageTest)
        buildType(IdeStarterTests.TestResultsTreeTest)
        buildType(IdeStarterTests.ImportRunConfigurationsTest)
        buildType(IdeStarterTests.NonModuleTargetsTest)
        buildType(IdeStarterTests.RunAllTestsActionTest)
        buildType(IdeStarterTests.DisabledKotlinPluginTest)
        buildType(IdeStarterTests.PyCharmTest)
        buildType(IdeStarterTests.FastBuildTest)
        buildType(IdeStarterTests.ProjectViewOpenTest)
        buildType(IdeStarterTests.MoveKotlinFileTest)
        buildType(ServerE2eTests.SampleRepo)
        buildType(ServerE2eTests.LocalJdk)
        buildType(ServerE2eTests.RemoteJdk)
//            buildType(ServerE2eTests.ServerDownloadsBazelisk)
//        buildType(ServerE2eTests.AndroidProject)
//        buildType(ServerE2eTests.AndroidKotlinProject)
        buildType(ServerE2eTests.ScalaProject)
        buildType(ServerE2eTests.KotlinProject)
        buildType(ServerE2eTests.GoProject)
        buildType(ServerE2eTests.PythonProject)
        buildType(ServerE2eTests.JavaDiagnostics)
        buildType(ServerE2eTests.ManualTargets)
        buildType(ServerE2eTests.BuildSync)
        buildType(ServerE2eTests.FirstPhaseSync)
        buildType(ServerE2eTests.PartialSync)
        buildType(ServerE2eTests.ExternalAutoloads)
        buildType(ServerE2eTests.NestedModules)
        buildType(StaticAnalysis.Hirschgarten)
//        buildType(StaticAnalysis.AndroidBazelRules)
//        buildType(StaticAnalysis.AndroidTestdpc)
        buildType(StaticAnalysis.Bazel)
//        buildType(StaticAnalysis.JetpackComposeGitHub)
      }

      buildType(ResultsAggregator, options = {
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

//  ResultsAggregator.triggers {
//    finishBuildTrigger {
//      buildType = "${ProjectFormat.GitHub.id}"
//      successfulOnly = true
//      branchFilter = ProjectBranchFilters.githubBranchFilter
//    }
//  }

  ResultsAggregator.triggers {
    vcs {
      branchFilter = ProjectBranchFilters.githubBranchFilter
      triggerRules = ProjectTriggerRules.triggerRules
    }
  }

  // setup display order for bazel-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      //ProjectFormat,
      ProjectBuild,
      ProjectUnitTests,
      PluginBenchmark.BenchmarkDefault,
      PluginBenchmark.BenchmarkWithVersion,
      IdeStarterTests.HotswapTest,
      IdeStarterTests.GoLandSyncTest,
      IdeStarterTests.CoroutineDebugTest,
      IdeStarterTests.ReopenWithoutResyncTest,
      IdeStarterTests.RunLineMarkerTest,
      IdeStarterTests.ExternalRepoResolveTest,
      IdeStarterTests.JarSourceExcludeTest,
      IdeStarterTests.BazelProjectModelModifierTest,
      IdeStarterTests.BazelCoverageTest,
      IdeStarterTests.TestResultsTreeTest,
      IdeStarterTests.ImportRunConfigurationsTest,
      IdeStarterTests.NonModuleTargetsTest,
      IdeStarterTests.RunAllTestsActionTest,
      IdeStarterTests.DisabledKotlinPluginTest,
      IdeStarterTests.PyCharmTest,
      IdeStarterTests.FastBuildTest,
      IdeStarterTests.ProjectViewOpenTest,
      IdeStarterTests.MoveKotlinFileTest,
      ServerE2eTests.SampleRepo,
      ServerE2eTests.LocalJdk,
      ServerE2eTests.RemoteJdk,
      // ServerE2eTests.ServerDownloadsBazelisk,
//      ServerE2eTests.AndroidProject,
//      ServerE2eTests.AndroidKotlinProject,
      ServerE2eTests.ScalaProject,
      ServerE2eTests.KotlinProject,
      ServerE2eTests.GoProject,
      ServerE2eTests.PythonProject,
      ServerE2eTests.JavaDiagnostics,
      ServerE2eTests.ManualTargets,
      ServerE2eTests.BuildSync,
      ServerE2eTests.FirstPhaseSync,
      ServerE2eTests.PartialSync,
      ServerE2eTests.NestedModules,
      ServerE2eTests.ExternalAutoloads,
      StaticAnalysis.Hirschgarten,
//      StaticAnalysis.AndroidBazelRules,
//      StaticAnalysis.AndroidTestdpc,
      StaticAnalysis.Bazel,
//      StaticAnalysis.JetpackCompose,
      ResultsAggregator
    )
}

