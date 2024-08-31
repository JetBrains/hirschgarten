package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class FormatChecker(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[format] check formatting",
    steps = {
      script {
        this.name = "checking formatting with buildifier"
        scriptContent =
          """
          bazel run //tools/format:format.check --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140
          """.trimIndent()
        dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        dockerPull = true
        dockerImage = "registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest"
      }
    },
    vcsRoot = vcsRoot,
  )

object GitHub : FormatChecker(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : FormatChecker(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
