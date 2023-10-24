package configurations.intellijBazel

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

object BuildTheProject : BaseConfiguration.BaseBuildType(
        name = "[build] build intellij-bazel",
        setupSteps = false,
        vcsRoot = BaseConfiguration.IntellijBazelVcs,
        steps = {
            gradle {
                name = "build plugin"
                tasks = "buildPlugin"
                jdkHome = "%env.JDK_17_0%"
            }
        },
)