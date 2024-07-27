import configurations.*
import configurations.pluginBazel.IntellijBazelBuild
import configurations.pluginBazel.IntellijBazelTests
import configurations.pluginBsp.IntellijBenchmark
import configurations.pluginBsp.IntellijBuild
import configurations.pluginBsp.IntellijTests
import configurations.server.*
import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs


version = "2024.03"

project {
  subProject(Server)
  subProject(PluginBsp)
  subProject(PluginBazel)
  vcsRoot(BaseConfiguration.GitHubVcs)
  vcsRoot(BaseConfiguration.SpaceVcs)

  buildType(BazelFormat.GitHub)
  buildType(BazelFormat.Space)
}

object Server : Project({
  name = "Server"
  id("Server".toExtId())

  subProject(ServerGitHub)
  subProject(ServerSpace)
})

object PluginBsp : Project({
  name = "Plugin BSP"
  id("Plugin BSP".toExtId())

  subProject(PluginBspGitHub)
  subProject(PluginBspSpace)
})

object PluginBazel : Project({
  name = "Plugin Bazel"
  id("Plugin Bazel".toExtId())

  subProject(PluginBazelGitHub)
  subProject(PluginBazelSpace)
})

object ServerGitHub : Project({

  name = "GitHub"
  id("GitHubServer".toExtId())

  // setup pipeline chain for bazel-bsp
  val allSteps =
    sequential {

      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
        buildType(BazelBuild.GitHub)
        buildType(BazelUnitTests.GitHub)
        buildType(BazelE2eTests.SampleRepoGitHub)
        buildType(BazelE2eTests.LocalJdkGitHub)
        buildType(BazelE2eTests.RemoteJdkGitHub)
//            buildType(BazelE2eTests.ServerDownloadsBazeliskGitHub)
        buildType(BazelE2eTests.AndroidProjectGitHub)
        buildType(BazelE2eTests.AndroidKotlinProjectGitHub)
        buildType(BazelE2eTests.ScalaProjectGitHub)
        buildType(BazelE2eTests.KotlinProjectGitHub)
        buildType(BazelE2ePluginTests.GitHub)
        buildType(BazelBenchmark.GitHub)
      }

      buildType(ResultsAggregator.ServerGitHub, options = {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      })
    }.buildTypes()

  // initialize all build steps for bazel-bsp
  allSteps.forEach { buildType(it) }

  // make all the builds depend on formater
  allSteps.dropLast(1).forEach {
    it.dependencies {
      snapshot(BazelFormat.GitHub) {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }
    }
  }

  // setup trigger for bazel-bsp pipeline
  allSteps.last().triggers {
    vcs {
      branchFilter = "+:pull/*"
      triggerRules =
        """
        +:/server/**
        +:*
        -:**.md
        -:**.yml
        -:LICENSE
        """.trimIndent()
    }
  }

  // setup display order for bazel-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      RelativeId("GitHubBuildServerBuild"),
      RelativeId("GitHubUnitTestsServerUnitTests"),
      RelativeId("GitHubE2eTestsServerE2eSampleRepoTest"),
      RelativeId("GitHubE2eTestsServerE2eLocalJdkTest"),
      RelativeId("GitHubE2eTestsServerE2eRemoteJdkTest"),
//        RelativeId("GitHubE2eTestsServerE2eServerDownloadsBazeliskTest"),
      RelativeId("GitHubE2eTestsServerE2eKotlinProjectTest"),
      RelativeId("GitHubE2eTestsServerE2eAndroidProjectTest"),
      RelativeId("GitHubE2eTestsServerE2eAndroidKotlinProjectTest"),
      RelativeId("GitHubE2eTestsServerE2eEnabledRulesTest"),
      RelativeId("GitHubE2eTestsServerPluginRun"),
      RelativeId("GitHubBenchmarkServer10targets"),
      RelativeId("GitHubServerResults"),
    )
})

object ServerSpace : Project({

  name = "Space"
  id("SpaceServer".toExtId())

  // setup pipeline chain for bazel-bsp
  val allSteps =
    sequential {

      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
        buildType(BazelBuild.Space)
        buildType(BazelUnitTests.Space)
        buildType(BazelE2eTests.SampleRepoSpace)
        buildType(BazelE2eTests.LocalJdkSpace)
        buildType(BazelE2eTests.RemoteJdkSpace)
//            buildType(BazelE2eTests.ServerDownloadsBazeliskSpace)
        buildType(BazelE2eTests.AndroidProjectSpace)
        buildType(BazelE2eTests.AndroidKotlinProjectSpace)
        buildType(BazelE2eTests.ScalaProjectSpace)
        buildType(BazelE2eTests.KotlinProjectSpace)
        buildType(BazelE2ePluginTests.Space)
        buildType(BazelBenchmark.Space)
      }

      buildType(ResultsAggregator.ServerSpace, options = {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      })
    }.buildTypes()

  // initialize all build steps for bazel-bsp
  allSteps.forEach { buildType(it) }

  // make all the builds depend on formater
  allSteps.dropLast(1).forEach {
    it.dependencies {
      snapshot(BazelFormat.Space) {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }
    }
  }

  // setup trigger for bazel-bsp pipeline
  allSteps.last().triggers {
    vcs {
      branchFilter =
        """
        +:<default>
        +:*
        -:bazel-steward*
        """.trimIndent()
      triggerRules =
        """
        +:/server/**
        +:*
        -:**.md
        -:**.yml
        -:LICENSE
        """.trimIndent()
    }
  }

  // setup display order for bazel-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      RelativeId("SpaceBuildServerBuild"),
      RelativeId("SpaceUnitTestsServerUnitTests"),
      RelativeId("SpaceE2eTestsServerE2eSampleRepoTest"),
      RelativeId("SpaceE2eTestsServerE2eLocalJdkTest"),
      RelativeId("SpaceE2eTestsServerE2eRemoteJdkTest"),
//        RelativeId("SpaceE2eTestsServerE2eServerDownloadsBazeliskTest"),
      RelativeId("SpaceE2eTestsServerE2eKotlinProjectTest"),
      RelativeId("SpaceE2eTestsServerE2eAndroidProjectTest"),
      RelativeId("SpaceE2eTestsServerE2eAndroidKotlinProjectTest"),
      RelativeId("SpaceE2eTestsServerE2eEnabledRulesTest"),
      RelativeId("SpaceE2eTestsServerPluginRun"),
      RelativeId("SpaceBenchmarkServer10targets"),
      RelativeId("SpaceServerResults"),
    )
})

object PluginBspGitHub : Project({

  name = "GitHub"
  id("GitHubPluginBsp".toExtId())

  // setup pipeline chain for intellij-bsp
  val allSteps =
    sequential {

      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
//            buildType(IntellijDetekt.GitHub)
        buildType(IntellijBuild.GitHub)
        buildType(IntellijTests.GitHub)
        buildType(IntellijBenchmark.GitHub)
      }

      buildType(ResultsAggregator.PluginBspGitHub) {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      }
    }.buildTypes()

  // initialize all build steps for intellij-bsp
  allSteps.forEach { buildType(it) }

  // make all the builds depend on formater
  allSteps.dropLast(1).forEach {
    it.dependencies {
      snapshot(BazelFormat.GitHub) {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }
    }
  }

  // setup trigger for intellij-bsp pipeline
  allSteps.last().triggers {
    vcs {
      triggerRules =
        """
        +:/plugin-bsp/**
        +:*
        -:**.md
        -:**.yml
        -:LICENSE
        """.trimIndent()
      branchFilter =
        """
        +:pull/*
        """.trimIndent()
    }
  }

  // setup display order for intellij-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
//            RelativeId("GitHubFormatDetekt"),
      RelativeId("GitHubBuildBuildPluginBsp"),
      RelativeId("GitHubTestsPluginBspUnitTests"),
      RelativeId("GitHubBenchmarkPluginBsp10Targets"),
      RelativeId("GitHubPluginBspResults"),
    )
})

object PluginBspSpace : Project({

  name = "Space"
  id("SpacePluginBsp".toExtId())

  // setup pipeline chain for intellij-bsp
  val allSteps =
    sequential {

      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
//            buildType(IntellijDetekt.Space)
        buildType(IntellijBuild.Space)
        buildType(IntellijTests.Space)
        buildType(IntellijBenchmark.Space)
      }

      buildType(ResultsAggregator.PluginBspSpace) {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      }
    }.buildTypes()

  // initialize all build steps for intellij-bsp
  allSteps.forEach { buildType(it) }

  // make all the builds depend on formater
  allSteps.dropLast(1).forEach {
    it.dependencies {
      snapshot(BazelFormat.Space) {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }
    }
  }

  // setup trigger for intellij-bsp pipeline
  allSteps.last().triggers {
    vcs {
      triggerRules =
        """
        +:/plugin-bsp/**
        +:*
        -:**.md
        -:**.yml
        -:LICENSE
        """.trimIndent()
      branchFilter =
        """
        +:*
        """.trimIndent()
    }
  }

  // setup display order for intellij-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
//        RelativeId("SpaceFormatDetekt"),
      RelativeId("SpaceBuildBuildPluginBsp"),
      RelativeId("SpaceTestsPluginBspUnitTests"),
      RelativeId("SpaceBenchmarkPluginBsp10Targets"),
      RelativeId("SpacePluginBspResults"),
    )
})

object PluginBazelGitHub : Project({
  id("GitHubPluginBazel".toExtId())
  name = "GitHub"

  // setup pipeline chain for intellij-bsp
  val allSteps =
    sequential {

      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
        buildType(IntellijBazelBuild.GitHub)
        buildType(IntellijBazelTests.UnitTestsGitHub)
//            buildType(IntellijBazelTests.IdeProbeGitHub)
      }

      buildType(ResultsAggregator.PluginBazelGitHub) {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      }
    }.buildTypes()

  // initialize all build steps for intellij-bsp
  allSteps.forEach { buildType(it) }

  // make all the builds depend on formater
  allSteps.dropLast(1).forEach {
    it.dependencies {
      snapshot(BazelFormat.GitHub) {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }
    }
  }

  // setup trigger for intellij-bsp pipeline
  allSteps.last().triggers {
    vcs {
      triggerRules =
        """
        +:/plugin-bazel/**
        +:*
        -:**.md
        -:**.yml
        -:LICENSE
        """.trimIndent()
      branchFilter =
        """
        +:pull/*
        """.trimIndent()
    }
  }

  // setup display order for intellij-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      RelativeId("GitHubBuildBuildPluginBazel"),
//        RelativeId("GitHubTestsPluginBazelIdeProbe"),
      RelativeId("GitHubTestsPluginBazelUnitTests"),
      RelativeId("GitHubPluginBazelResults"),
    )
})

object PluginBazelSpace : Project({
  id("SpacePluginBazel".toExtId())
  name = "Space"

  // setup pipeline chain for intellij-bsp
  val allSteps =
    sequential {

      parallel(options = {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }) {
        buildType(IntellijBazelBuild.Space)
        buildType(IntellijBazelTests.UnitTestsSpace)
//            buildType(IntellijBazelTests.IdeProbeSpace)
      }

      buildType(ResultsAggregator.PluginBazelSpace) {
        onDependencyFailure = FailureAction.ADD_PROBLEM
        onDependencyCancel = FailureAction.ADD_PROBLEM
      }
    }.buildTypes()

  // initialize all build steps for intellij-bsp
  allSteps.forEach { buildType(it) }

  // make all the builds depend on formater
  allSteps.dropLast(1).forEach {
    it.dependencies {
      snapshot(BazelFormat.Space) {
        onDependencyFailure = FailureAction.CANCEL
        onDependencyCancel = FailureAction.CANCEL
      }
    }
  }

  // setup trigger for intellij-bsp pipeline
  allSteps.last().triggers {
    vcs {
      triggerRules =
        """
        +:/plugin-bazel/**
        +:*
        -:**.md
        -:**.yml
        -:LICENSE
        """.trimIndent()
      branchFilter =
        """
        +:<default>
        +:*
        """.trimIndent()
    }
  }

  // setup display order for intellij-bsp pipeline
  buildTypesOrderIds =
    arrayListOf(
      RelativeId("SpaceBuildBuildPluginBazel"),
//        RelativeId("SpaceTestsPluginBazelIdeProbe"),
      RelativeId("SpaceTestsPluginBazelUnitTests"),
      RelativeId("SpacePluginBazelResults"),
    )
})
