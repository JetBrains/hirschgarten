import configurations.*
import configurations.pluginBsp.IntellijBenchmark
import configurations.server.*
import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

version = "2024.03"

object ProjectBranchFilters {
  val githubBranchFilter = "+:pull/*"
  val spaceBranchFilter =
    """
    +:<default>
    +:*
    -:bazel-steward*
    """.trimIndent()
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
    """.trimIndent()
}

project {
  subProject(GitHub)
  subProject(Space)
}

object GitHub : Project({
  name = "GitHub"
  id(name.toExtId())
  vcsRoot(BaseConfiguration.GitHubVcs)

// setup pipeline chain for bazel-bsp
  val allSteps =
    sequential {
      buildType(ProjectFormat.GitHub)
      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
        buildType(ProjectBuild.GitHub)
        buildType(ProjectUnitTests.GitHub)
        buildType(IntellijBenchmark.GitHub)
        buildType(BazelE2eTests.SampleRepoGitHub)
        buildType(BazelE2eTests.LocalJdkGitHub)
        buildType(BazelE2eTests.RemoteJdkGitHub)
//            buildType(BazelE2eTests.ServerDownloadsBazeliskGitHub)
        buildType(BazelE2eTests.AndroidProjectGitHub)
        buildType(BazelE2eTests.AndroidKotlinProjectGitHub)
        buildType(BazelE2eTests.ScalaProjectGitHub)
        buildType(BazelE2eTests.KotlinProjectGitHub)
        buildType(BazelE2eTests.PythonProjectGitHub)
        buildType(BazelE2eTests.JavaDiagnosticsGitHub)
        buildType(BazelBenchmark.GitHub)
      }

      buildType(ResultsAggregator.GitHub, options = {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      })
    }.buildTypes()

  // initialize all build steps for bazel-bsp
  allSteps.forEach { buildType(it) }

  // setup trigger for bazel-bsp pipeline
  allSteps.last().triggers {
    vcs {
      branchFilter = ProjectBranchFilters.githubBranchFilter
      triggerRules = ProjectTriggerRules.triggerRules
    }
  }

  // setup display order for bazel-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      RelativeId("GitHubFormatCheckFormatting"),
      RelativeId("GitHubBuildBuildProject"),
      RelativeId("GitHubUnitTestsProjectUnitTests"),
      RelativeId("GitHubBenchmarkServer10targets"),
      RelativeId("GitHubBenchmarkPluginBsp10targets"),
      RelativeId("GitHubE2eTestsServerE2eSampleRepoTest"),
      RelativeId("GitHubE2eTestsServerE2eLocalJdkTest"),
      RelativeId("GitHubE2eTestsServerE2eRemoteJdkTest"),
//        RelativeId("GitHubE2eTestsServerE2eServerDownloadsBazeliskTest"),
      RelativeId("GitHubE2eTestsServerE2eAndroidProjectTest"),
      RelativeId("GitHubE2eTestsServerE2eAndroidKotlinProjectTest"),
      RelativeId("GitHubE2eTestsServerE2eEnabledRulesTest"),
      RelativeId("GitHubE2eTestsServerE2eKotlinProjectTest"),
      RelativeId("GitHubE2eTestsServerE2ePythonProjectTest"),
      RelativeId("GitHubE2eTestsServerE2eJavaDiagnosticsTest"),
      RelativeId("GitHubResults"),
    )
})

object Space : Project({
  name = "Space"
  id(name.toExtId())
  vcsRoot(BaseConfiguration.SpaceVcs)

// setup pipeline chain for bazel-bsp
  val allSteps =
    sequential {
      buildType(ProjectFormat.Space)
      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
        buildType(ProjectBuild.Space)
        buildType(ProjectUnitTests.Space)
        buildType(IntellijBenchmark.Space)
        buildType(BazelE2eTests.SampleRepoSpace)
        buildType(BazelE2eTests.LocalJdkSpace)
        buildType(BazelE2eTests.RemoteJdkSpace)
//            buildType(BazelE2eTests.ServerDownloadsBazeliskSpace)
        buildType(BazelE2eTests.AndroidProjectSpace)
        buildType(BazelE2eTests.AndroidKotlinProjectSpace)
        buildType(BazelE2eTests.ScalaProjectSpace)
        buildType(BazelE2eTests.KotlinProjectSpace)
        buildType(BazelE2eTests.PythonProjectSpace)
        buildType(BazelE2eTests.JavaDiagnosticsSpace)
        buildType(BazelBenchmark.Space)
      }

      buildType(ResultsAggregator.Space, options = {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      })
    }.buildTypes()

  // initialize all build steps for bazel-bsp
  allSteps.forEach { buildType(it) }

  // setup trigger for bazel-bsp pipeline
  allSteps.last().triggers {
    vcs {
      branchFilter = ProjectBranchFilters.spaceBranchFilter
      triggerRules = ProjectTriggerRules.triggerRules
    }
  }

  // setup display order for bazel-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      RelativeId("SpaceFormatCheckFormatting"),
      RelativeId("SpaceBuildBuildProject"),
      RelativeId("SpaceUnitTestsProjectUnitTests"),
      RelativeId("SpaceBenchmarkServer10targets"),
      RelativeId("SpaceBenchmarkPluginBsp10targets"),
      RelativeId("SpaceE2eTestsServerE2eSampleRepoTest"),
      RelativeId("SpaceE2eTestsServerE2eLocalJdkTest"),
      RelativeId("SpaceE2eTestsServerE2eRemoteJdkTest"),
//        RelativeId("SpaceE2eTestsServerE2eServerDownloadsBazeliskTest"),
      RelativeId("SpaceE2eTestsServerE2eAndroidProjectTest"),
      RelativeId("SpaceE2eTestsServerE2eAndroidKotlinProjectTest"),
      RelativeId("SpaceE2eTestsServerE2eEnabledRulesTest"),
      RelativeId("SpaceE2eTestsServerE2eKotlinProjectTest"),
      RelativeId("SpaceE2eTestsServerE2ePythonProjectTest"),
      RelativeId("SpaceE2eTestsServerE2eJavaDiagnosticsTest"),
      RelativeId("SpaceResults"),
    )
})
