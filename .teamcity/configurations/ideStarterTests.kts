package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.Requirements
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class IdeStarterTests(
  vcsRoot: GitVcsRoot,
  requirements: (Requirements.() -> Unit)? = null,
) : BaseConfiguration.BaseBuildType(
  name = "[ide-starter] Hotswap test",
  vcsRoot = vcsRoot,
  requirements = requirements,
  artifactRules = Utils.CommonParams.BazelTestlogsArtifactRules,
  steps = {
    bazel {
      val reportErrors = "--jvmopt=\"-DDO_NOT_REPORT_ERRORS=true\""
      val cachePath = "--jvmopt=\"-Dbazel.ide.starter.test.cache.directory=%system.teamcity.build.tempDir%\""
      val memArg = "--jvmopt=\"-Xmx12g\""
      val sandboxArg = "--sandbox_writable_path=/"
      val actionEnvArg = "--action_env=PATH"

      val sysArgs = "$reportErrors $cachePath $memArg $sandboxArg $actionEnvArg"

      this.name = "run hotswap test"
      id = "run_hotswap_test"
      command = "test"
      targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/hotswap"
      arguments = "$sysArgs ${Utils.CommonParams.BazelCiSpecificArgs}"
      toolPath = "/usr/local/bin"
      logging = BazelStep.Verbosity.Diagnostic
      Utils.DockerParams.get().forEach { (key, value) ->
        param(key, value)
      }
    }
    script {
      name = "copy test logs"
      id = "copy_test_logs"
      scriptContent =
        """
          #!/bin/bash
          set -euxo
          
          cp -R /home/teamcity/agent/system/.persistent_cache/bazel/_bazel_hirschuser/*/execroot/_main/bazel-out/k8-fastbuild/testlogs .
          """.trimIndent()
    }
  }
)

// github variant
object HotswapTestGitHub : IdeStarterTests(
  vcsRoot = BaseConfiguration.GitHubVcs,
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
    equals("container.engine.osType", "linux")
  }
)

// space variant
object HotswapTestSpace : IdeStarterTests(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
