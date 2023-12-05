import configurations.intellijBsp.*
import configurations.*
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.Project


version = "2023.05"

project(IntellijBsp)

object IntellijBsp : Project({

    vcsRoot(BaseConfiguration.IntellijBspVcs)

    // setup pipeline chain for intellij-bsp
    val allSteps = sequential {

        parallel(options = {
            onDependencyFailure = FailureAction.CANCEL
            onDependencyCancel = FailureAction.CANCEL

        }) {
            buildType(IntellijDetekt.IntellijDetekt)
            buildType(IntellijBuild.BuildTheProject)
            buildType(IntellijTests.UnitTests)
            buildType(IntellijPluginVerifier.VerifyPlugin)
        }

        buildType(ResultsAggregator.IntellijBspAggregator) {
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
            RelativeId("BuildBuildIntellijBsp"),
            RelativeId("FormatDetekt"),
            RelativeId("TestsUnitTests"),
            RelativeId("VerifyPluginVerifier"),
            RelativeId("IntellijBspResults")
    )
})
