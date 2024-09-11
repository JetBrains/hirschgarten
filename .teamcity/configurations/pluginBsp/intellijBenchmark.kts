package configurations.pluginBsp

import configurations.BaseConfiguration
import configurations.Utils
import jetbrains.buildServer.configs.kotlin.v2019_2.Requirements
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Benchmark(vcsRoot: GitVcsRoot, requirements: (Requirements.() -> Unit)? = null) :
  BaseConfiguration.BaseBuildType(
    name = "[benchmark] Plugin BSP 10 targets",
    vcsRoot = vcsRoot,
    requirements = requirements,
    artifactRules = Utils.CommonParams.BazelTestlogsArtifactRules,
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
      script {
        id = "simpleRunner"
        scriptContent =
          """
          #!/bin/bash
          set -euxo
          
          cp -R /home/teamcity/agent/system/.persistent_cache/bazel/_bazel_hirschuser/*/execroot/_main/bazel-out/k8-fastbuild/testlogs .
          """.trimIndent()
      }
    },
  )

object GitHub : Benchmark(
  vcsRoot = BaseConfiguration.GitHubVcs,
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
    equals("container.engine.osType", "linux")
  },
)

object Space : Benchmark(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
