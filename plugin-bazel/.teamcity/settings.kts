import configurations.*
import configurations.intellijBazel.*
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.Project


version = "2023.05"

project(IntellijBazel)

object IntellijBazel : Project({

    vcsRoot(BaseConfiguration.IntellijBazelVcs)

    // setup pipeline chain for intellij-bsp
    val allSteps = sequential {

        parallel(options = {
            onDependencyFailure = FailureAction.CANCEL
            onDependencyCancel = FailureAction.CANCEL

        }) {
            buildType(IntellijBazelBuild.BuildTheProject)
            buildType(IntellijBazelTests.UnitTests)
            buildType(IntellijBazelTests.IdeProbeTests)
        }

        buildType(ResultsAggregator.IntellijBazelAggregator) {
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
                +:pull/*
            """.trimIndent()
        }
    }

    // setup display order for intellij-bsp pipeline
    buildTypesOrderIds = arrayListOf(
            RelativeId("BuildBuildIntellijBazel"),
            RelativeId("TestsIdeProbeTests"),
            RelativeId("TestsUnitTests"),
            RelativeId("IntellijBazelResults")
    )
})
