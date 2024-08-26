package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class UnitTests(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[unit tests] project unit tests",
    artifactRules = "+:%system.teamcity.build.checkoutDir%/bazel-testlogs/** => testlogs.zip",
    steps = {
      bazel {
        name = "bazel test //..."
        command = "test"
        targets = "//..."
        arguments = "--test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
        Utils.DockerParams.get().forEach { (key, value) ->
          param(key, value)
        }
      }
    },
    vcsRoot = vcsRoot,
  )

object GitHub : UnitTests(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : UnitTests(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
