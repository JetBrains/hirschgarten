package configurations.intellijBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Detekt (
    vcsRoot: GitVcsRoot
): BaseConfiguration.BaseBuildType(
    name = "[format] detekt",
    vcsRoot = vcsRoot,
  	artifactRules = "+:%system.teamcity.build.checkoutDir%/**/reports/detekt/* => detekt_report.zip",
    steps = {
        gradle {
            id = "run_detekt"
            name = "run detekt"
            tasks = "detekt"
            jdkHome = "%env.JDK_17_0%"
        }
    }
)

object GitHub : Detekt(
    vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Detekt(
    vcsRoot = BaseConfiguration.SpaceVcs
)