package configurations.intellijBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle

object VerifyPlugin : BaseConfiguration.BaseBuildType(
        name = "[verify] plugin verifier",
        setupSteps = false,
        vcsRoot = BaseConfiguration.IntellijBspVcs,
        steps = {
            gradle {
                name = "run plugin verifier"
                tasks = ":runPluginVerifier"
                jdkHome = "%env.JDK_17_0%"
            }
        },
)