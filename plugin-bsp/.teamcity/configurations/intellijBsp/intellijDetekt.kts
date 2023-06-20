package configurations.intellijBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

object IntellijDetekt : BaseConfiguration.BaseBuildType(
    name = "[format] detekt",
    vcsRoot = BaseConfiguration.IntellijBspVcs,
  	artifactRules = "+:%system.teamcity.build.checkoutDir%/**/reports/detekt/* => detekt_report.zip",
    steps = {
        gradle {
            name = "run detekt"
            tasks = "detekt"
            jdkHome = "%env.JDK_17_0%"
        }
    }
)
