package configurations.intellijBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

object BuildTheProject : BaseConfiguration.BaseBuildType(
        name = "[build] build intellij-bsp",
        setupSteps = false,
        vcsRoot = BaseConfiguration.IntellijBspVcs,
        steps = {
            gradle {
                name = "build plugin"
                tasks = "buildPlugin"
                jdkHome = "%env.JDK_17_0%"
            }
        },
)