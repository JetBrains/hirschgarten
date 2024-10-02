package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Build(vcsRoot: GitVcsRoot) :
    BaseConfiguration.BaseBuildType(
        name = "[build] build project",
        vcsRoot = vcsRoot,
        steps = {
            Utils.CommonParams.CrossBuildPlatforms.forEach { platform ->
                bazel {
                val platformDot = "${platform.take(2)}.${platform.last()}"

                enabled = (platform != "242")
                executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE

                name = "build //..."
                command = "build"
                targets = "//..."
                arguments = "--define=ij_product=intellij-20$platformDot ${Utils.CommonParams.BazelCiSpecificArgs}"
                logging = BazelStep.Verbosity.Diagnostic
                Utils.DockerParams.get().forEach { (key, value) ->
                  param(key, value)
                }
            }
        }
    },
  )

object GitHub : Build(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Build(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
