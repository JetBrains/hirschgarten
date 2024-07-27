package configurations.server

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class PluginRun(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[e2e tests] server plugin run",
    vcsRoot = vcsRoot,
    artifactRules = "+:%system.teamcity.build.checkoutDir%/bazel-testlogs/** => testlogs.zip",
    steps = {
      val sysArgs = "--jvmopt=\"-Dbsp.benchmark.project.path=%system.teamcity.build.tempDir%/project_10\" --jvmopt=\"-Dbsp.benchmark.teamcity.url=https://bazel.teamcity.com\""
      script {
        this.name = "install xvfb and generate project for benchmark"
        id = "install_xvfb_and_generate_project_for_benchmark"
        scriptContent =
          """
          #!/bin/bash
          set -euxo pipefail
          
          sudo apt-get update
          sudo apt-get install -y xvfb
          
          bazel run //server/bspcli:generator -- %system.teamcity.build.tempDir%/project_10 10 --targetssize 1
          """.trimIndent()
      }
      bazel {
        name = "run plugin"
        id = "run_plugin"
        command = "test"
        targets = "//plugin-bsp/performance-testing"
        arguments ="--jvmopt=\"-Dbsp.benchmark.cache.directory=%system.teamcity.build.tempDir%\"  --jvmopt=\"-Xmx12g\" $sysArgs --sandbox_writable_path=/ --action_env=PATH --test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
        param("toolPath", "/usr/local/bin")
      }
    },
  )

object GitHub : PluginRun(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : PluginRun(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
