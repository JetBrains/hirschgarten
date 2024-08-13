package configurations.server

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Build(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[build] server build",
    setupSteps = true,
    vcsRoot = vcsRoot,
    steps = {
      bazel {
        name = "build //..."
        command = "build"
        targets = "//server/..."
        arguments = "--announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
        param("toolPath", "/usr/local/bin")
      }
    },
  )

object GitHub : Build(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Build(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
