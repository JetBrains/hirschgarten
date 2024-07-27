package configurations.pluginBazel

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Build(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[build] build Plugin Bazel",
    artifactRules = "+:%system.teamcity.build.checkoutDir%/bazel-bin/intellij-bazel.zip",
    vcsRoot = vcsRoot,
    steps = {
      bazel {
        id = "build_plugin"
        name = "build plugin"
        command = "build"
        targets = "//plugin-bazel/..."
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
