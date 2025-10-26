import configurations.*
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

version = "2024.12"

object ProjectBranchFilters {
  val githubBranchFilter = "+:pull/*"
  val spaceBranchFilter =
    """
    +:<default>
    +:*
    -:bazel-steward*
    -:refs/merge/*
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
    -:/tools/**
    """.trimIndent()
}


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
    buildType(FormatBuildFactory.GitHub)
    parallel(options = {
      onDependencyFailure = FailureAction.ADD_PROBLEM
      onDependencyCancel = FailureAction.ADD_PROBLEM
    }) {
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

  FormatBuildFactory.GitHub.triggers {
    vcs {
      branchFilter = ProjectBranchFilters.githubBranchFilter
      triggerRules = ProjectTriggerRules.triggerRules
    }
  }
  Aggregator.triggers {
    finishBuildTrigger {
      buildType = "${FormatBuildFactory.GitHub.id}"
      successfulOnly = false
      branchFilter = ProjectBranchFilters.githubBranchFilter
    }
  }

  buildTypesOrderIds = arrayListOf(
    FormatBuildFactory.GitHub,
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
    buildType(FormatBuildFactory.Space)
    parallel(options = {
      onDependencyFailure = FailureAction.ADD_PROBLEM
      onDependencyCancel = FailureAction.ADD_PROBLEM
    }) {
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

  FormatBuildFactory.Space.triggers {
    vcs {
      branchFilter = ProjectBranchFilters.spaceBranchFilter
      triggerRules = ProjectTriggerRules.triggerRules
    }
  }
  AggregatorSpace.triggers {
    finishBuildTrigger {
      buildType = "${FormatBuildFactory.Space.id}"
      successfulOnly = false
      branchFilter = ProjectBranchFilters.spaceBranchFilter
    }
  }

  buildTypesOrderIds = arrayListOf(
    FormatBuildFactory.Space,
    *PluginBuildFactory.ForAllPlatformsSpace.toTypedArray(),
    ProjectUnitTestsSpace,
    *PluginBenchmarkFactory.AllBenchmarkTestsSpace.toTypedArray(),
    *IdeStarterTestFactory.AllIdeStarterTestsSpace.toTypedArray(),
    *StaticAnalysisFactory.EnabledAnalysisTestsSpace.toTypedArray(),
    AggregatorSpace
  )
})
