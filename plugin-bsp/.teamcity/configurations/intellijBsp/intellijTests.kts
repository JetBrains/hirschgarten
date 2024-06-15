package configurations.intellijBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

open class UnitTests(
    vcsRoot: GitVcsRoot
) : BaseConfiguration.BaseBuildType(
    name = "[tests] unit tests",
    artifactRules = "+:**/build/reports/**/* => reports.zip",
    vcsRoot = vcsRoot,
    steps = {
        gradle {
            this.name = "run unit tests"
            id = "run_unit_tests"
            tasks = "test"
            jdkHome = "%env.JDK_17_0%"
            gradleParams = "-x :performance-testing:test"
        }
    }
)

object GitHub : UnitTests(
    vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : UnitTests(
    vcsRoot = BaseConfiguration.SpaceVcs
)