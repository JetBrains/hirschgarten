package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Build(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[build] build project",
    vcsRoot = vcsRoot,
    steps = {
      bazel {
        name = "build //..."
        command = "build"
        targets = "//..."
        arguments = "--announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
        Utils.DockerParams.get().forEach { (key, value) ->
          param(key, value)
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
