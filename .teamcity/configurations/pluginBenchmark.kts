package configurations

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
      script {
        id = "add_bazelversion"
        scriptContent =
          """
          #!/bin/bash
          set -euxo
          
          echo "${Utils.CommonParams.BazelVersion}" > /home/hirschuser/project_10/.bazelversion
          """.trimIndent()
      }
      bazel {
        val url = "--jvmopt=\"-Dbsp.benchmark.teamcity.url=https://bazel.teamcity.com\""
        val projectNameArg = "--jvmopt=\"-Dbsp.benchmark.project.name=benchmark_10_targets\""
        val reportErrors = "--jvmopt=\"-DDO_NOT_REPORT_ERRORS=true\""
        val projectPath =
          "--jvmopt=\"-Dbsp.benchmark.project.path=/home/hirschuser/project_10\""
        val cachePath = "--jvmopt=\"-Dbsp.benchmark.cache.directory=%system.teamcity.build.tempDir%\""
        val memArg = "--jvmopt=\"-Xmx12g\""
        val sandboxArg = "--sandbox_writable_path=/"
        val actionEnvArg = "--action_env=PATH"

        val sysArgs = "$url $projectNameArg $reportErrors $projectPath $cachePath $memArg $sandboxArg $actionEnvArg"

        name = "run benchmark"
        id = "run_benchmark"
        command = "test"
        targets = "//plugin-bsp/performance-testing"
        arguments = "$sysArgs ${Utils.CommonParams.BazelCiSpecificArgs}"
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
