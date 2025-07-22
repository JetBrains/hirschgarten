package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.Requirements
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

// Base class with common parameters
open class BaseBenchmark(
  vcsRoot: GitVcsRoot,
  name: String,
  requirements: (Requirements.() -> Unit)? = null,
  dockerParams: Map<String, String>,
  ) : BaseConfiguration.BaseBuildType(
    name = "[benchmark] $name",
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
        dockerParams.forEach { (key, value) ->
          param(key, value)
        }
      }
      bazel {
        val url = "--jvmopt=\"-Dbazel.ide.starter.test.teamcity.url=https://bazel.teamcity.com\""
        val projectNameArg = "--jvmopt=\"-Dbazel.ide.starter.test.project.name=benchmark_10_targets\""
        val reportErrors = "--jvmopt=\"-DDO_NOT_REPORT_ERRORS=true\""
        val projectPath =
          "--jvmopt=\"-Dbazel.ide.starter.test.project.path=/home/hirschuser/project_10\""
        val cachePath = "--jvmopt=\"-Dbazel.ide.starter.test.cache.directory=%system.teamcity.build.tempDir%\""
        val memArg = "--jvmopt=\"-Xmx12g\""
        val sandboxArg = "--sandbox_writable_path=/"
        val actionEnvArg = "--action_env=PATH"

        val sysArgs = "$url $projectNameArg $reportErrors $projectPath $cachePath $memArg $sandboxArg $actionEnvArg"

        this.name = "run benchmark"
        id = "run_benchmark"
        command = "test"
        targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/performance"
        arguments = "$sysArgs ${Utils.CommonParams.BazelCiSpecificArgs}"
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
        dockerParams.forEach { (key, value) ->
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

// Specific benchmark types
open class BenchmarkWithBazelVersion(
  vcsRoot: GitVcsRoot,
  requirements: (Requirements.() -> Unit)? = null,
) : BaseBenchmark(
  name = "Plugin BSP 10 targets with current Bazel",
  vcsRoot = vcsRoot,
  requirements = requirements,
  dockerParams = Utils.DockerParams.get().toMutableMap().apply {
    set("plugin.docker.run.parameters", get("plugin.docker.run.parameters") + "\n-v %teamcity.build.checkoutDir%/.bazelversion:/home/hirschuser/project_10/.bazelversion")
  }
)

open class BenchmarkDefault(
  vcsRoot: GitVcsRoot,
  requirements: (Requirements.() -> Unit)? = null,
  ) : BaseBenchmark(
  name = "Plugin BSP 10 targets newest available Bazel",
  vcsRoot = vcsRoot,
  requirements = requirements,
  dockerParams = Utils.DockerParams.get()
)

// GitHub variants
object BenchmarkWithVersionGitHub : BenchmarkWithBazelVersion(
  vcsRoot = BaseConfiguration.GitHubVcs,
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
    equals("container.engine.osType", "linux")
  }
)

object BenchmarkDefaultGitHub : BenchmarkDefault(
  vcsRoot = BaseConfiguration.GitHubVcs,
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
    equals("container.engine.osType", "linux")
  }
)

