import configurations.*
import configurations.intellijBazel.*
import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.Project


version = "2024.03"

project {
    subProject(IntellijBazelGitHub)
    subProject(IntellijBazelSpace)
}

object IntellijBazelGitHub : Project({
    id("GitHub".toExtId())
    name = "Intellij-Bazel GH"
    vcsRoot(BaseConfiguration.GitHubVcs)

    // setup pipeline chain for intellij-bsp
    val allSteps = sequential {

        parallel(options = {
            onDependencyFailure = FailureAction.CANCEL
            onDependencyCancel = FailureAction.CANCEL

        }) {
            buildType(IntellijBazelBuild.GitHub)
            buildType(IntellijBazelTests.UnitTestsGitHub)
            buildType(IntellijBazelTests.IdeProbeGitHub)
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
            branchFilter = """
                +:pull/*
            """.trimIndent()
        }
    }

    // setup display order for intellij-bsp pipeline
    buildTypesOrderIds = arrayListOf(
        RelativeId("GitHubBuildBuildIntellijBazel"),
        RelativeId("GitHubTestsIdeProbe"),
        RelativeId("GitHubTestsUnitTests"),
        RelativeId("GitHubResults")
    )
})

object IntellijBazelSpace : Project({
    id("Space".toExtId())
    name = "Intellij-Bazel Space"
    vcsRoot(BaseConfiguration.SpaceVcs)

    // setup pipeline chain for intellij-bsp
    val allSteps = sequential {

        parallel(options = {
            onDependencyFailure = FailureAction.CANCEL
            onDependencyCancel = FailureAction.CANCEL

        }) {
            buildType(IntellijBazelBuild.Space)
            buildType(IntellijBazelTests.UnitTestsSpace)
            buildType(IntellijBazelTests.IdeProbeSpace)
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
            branchFilter = """
                +:<default>
                +:*
            """.trimIndent()
        }
    }

    // setup display order for intellij-bsp pipeline
    buildTypesOrderIds = arrayListOf(
        RelativeId("SpaceBuildBuildIntellijBazel"),
        RelativeId("SpaceTestsIdeProbe"),
        RelativeId("SpaceTestsUnitTests"),
        RelativeId("SpaceResults")
    )
})
