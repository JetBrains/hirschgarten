package configurations.intellijBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep


open class IntellijTestsBuildType(
        name: String,
        setupSteps: Boolean = false,
        steps: BuildSteps.() -> Unit,
        failureConditions: FailureConditions.() -> Unit = {},
        artifactRules: String = ""
) : BaseConfiguration.BaseBuildType(
    name = "[tests] $name",
    vcsRoot = BaseConfiguration.IntellijBspVcs,
    failureConditions = failureConditions,
    artifactRules = artifactRules,
    steps = steps,
    setupSteps = setupSteps,
    requirements =  {
        contains("cloud.amazon.agent-name-prefix", "Linux-Large")
    }
)

object UnitTests : IntellijTestsBuildType(
    name = "unit tests",
    steps = {
        gradle {
            this.name = "run unit tests"
            tasks = "test"
            gradleParams = "-x :probe:test -Pexclude.integration.test=true"
            jdkHome = "%env.JDK_17_0%"
        }
    }
)