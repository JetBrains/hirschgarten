package configurations.server

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot


open class Benchmark(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    artifactRules = "+:%system.teamcity.build.checkoutDir%/metrics.txt",
    name = "[benchmark] server 10 targets",
    vcsRoot = vcsRoot,
    setupSteps = true,
    steps = {
      bazel {
        name = "generate 10 project for benchmark"
        id = "generate_project_for_benchmark"
        command = "run"
        targets = "//server/bspcli:generator /tmp/project_10 10"
        arguments = "--announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
        param("toolPath", "/usr/local/bin")
      }
      bazel {
        name = "run benchmark 10 targets"
        id = "run_benchmark"
        command = "run"
        targets = "//server/bspcli:bspcli /tmp/project_10 %system.teamcity.build.checkoutDir%/metrics.txt"
        arguments = "--announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
        param("toolPath", "/usr/local/bin")
      }
    },
  )

object GitHub : Benchmark(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Benchmark(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
