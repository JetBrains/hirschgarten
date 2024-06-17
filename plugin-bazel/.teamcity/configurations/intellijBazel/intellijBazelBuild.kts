package configurations.intellijBazel

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Build (
    vcsRoot: GitVcsRoot,
): BaseConfiguration.BaseBuildType(
        name = "[build] build intellij-bazel",
        setupSteps = false,
        vcsRoot = vcsRoot,
        steps = {
            gradle {
                name = "build plugin"
                tasks = "buildPlugin"
                jdkHome = "%env.JDK_17_0%"
            }
        },
)

object GitHub : Build(
    vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Build(
    vcsRoot = BaseConfiguration.SpaceVcs
)