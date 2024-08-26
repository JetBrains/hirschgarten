package configurations.pluginBsp

import configurations.BaseConfiguration
import configurations.Utils
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Benchmark(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[benchmark] Plugin BSP 10 targets",
    vcsRoot = vcsRoot,
    artifactRules = "+:%system.teamcity.build.checkoutDir%/bazel-testlogs/** => testlogs.zip",
    steps = {
      val sysArgs = "--jvmopt=\"-Dbsp.benchmark.project.path=/home/hirschuser/project_10\" --jvmopt=\"-Dbsp.benchmark.teamcity.url=https://bazel.teamcity.com\""
      bazel {
        name = "run benchmark"
        id = "run_benchmark"
        command = "test"
        targets = "//plugin-bsp/performance-testing"
        arguments =
          "--jvmopt=\"-Xmx12g\" $sysArgs --sandbox_writable_path=/ --action_env=PATH --test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
        Utils.DockerParams.get().forEach { (key, value) ->
          param(key, value)
        }
      }
    },
  )

object GitHub : Benchmark(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Benchmark(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
