import configurations.intellijBsp.*
import configurations.*
import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.Project


version = "2024.03"

project {
    subProject(IntellijBspGitHub)
    subProject(IntellijBspSpace)
}

object IntellijBspGitHub : Project({

    name = "Intellij-BSP GH"
    id("GitHub".toExtId())
    vcsRoot(BaseConfiguration.GitHubVcs)


    // setup pipeline chain for intellij-bsp
    val allSteps = sequential {

        parallel(options = {
            onDependencyFailure = FailureAction.CANCEL
            onDependencyCancel = FailureAction.CANCEL

        }) {
            buildType(IntellijDetekt.GitHub)
            buildType(IntellijBuild.GitHub)
            buildType(IntellijTests.GitHub)
            buildType(IntellijBenchmark.GitHub)
        }

        buildType(ResultsAggregator.GitHub) {
            onDependencyFailure = FailureAction.ADD_PROBLEM
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }.buildTypes()

    // initialize all build steps for intellij-bsp
    allSteps.forEach { buildType(it) }

    // setup trigger for intellij-bsp pipeline
    allSteps.last().triggers {
        vcs {
            triggerRules = """
                -:**.md
                -:**.yml
                -:/LICENSE
                -:/.teamcity/**
            """.trimIndent()
            branchFilter = """
                +:pull/*
            """.trimIndent()
        }
    }

    // setup display order for intellij-bsp pipeline
    buildTypesOrderIds = arrayListOf(
            RelativeId("GitHubFormatDetekt"),
            RelativeId("GitHubBuildBuildIntellijBsp"),
            RelativeId("GitHubTestsUnitTests"),
            RelativeId("GitHubBenchmark10Targets"),
            RelativeId("GitHubResults")
    )
})

object IntellijBspSpace : Project({

    name = "Intellij-BSP Space"
    id("Space".toExtId())
    vcsRoot(BaseConfiguration.SpaceVcs)

    // setup pipeline chain for intellij-bsp
    val allSteps = sequential {

        parallel(options = {
            onDependencyFailure = FailureAction.CANCEL
            onDependencyCancel = FailureAction.CANCEL

        }) {
            buildType(IntellijDetekt.Space)
            buildType(IntellijBuild.Space)
            buildType(IntellijTests.Space)
            buildType(IntellijBenchmark.Space)
        }

        buildType(ResultsAggregator.Space) {
            onDependencyFailure = FailureAction.ADD_PROBLEM
            onDependencyCancel = FailureAction.ADD_PROBLEM
        }
    }.buildTypes()

    // initialize all build steps for intellij-bsp
    allSteps.forEach { buildType(it) }

    // setup trigger for intellij-bsp pipeline
    allSteps.last().triggers {
        vcs {
            triggerRules = """
                -:**.md
                -:**.yml
                -:/LICENSE
                -:/.teamcity/**
            """.trimIndent()
            branchFilter = """
                +:*
            """.trimIndent()
        }
    }

    // setup display order for intellij-bsp pipeline
    buildTypesOrderIds = arrayListOf(
        RelativeId("SpaceFormatDetekt"),
        RelativeId("SpaceBuildBuildIntellijBsp"),
        RelativeId("SpaceTestsUnitTests"),
        RelativeId("SpaceBenchmark10Targets"),
        RelativeId("SpaceResults")
    )
  }
)