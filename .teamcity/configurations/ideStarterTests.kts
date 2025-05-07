package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

sealed class IdeStarterTests(
  vcsRoot: GitVcsRoot,
  targets: String,
  name: String,
  testSpecificArgs: String = ""
) : BaseConfiguration.BaseBuildType(
  name = "[ide-starter] $name",
  vcsRoot = vcsRoot,
  requirements = if (vcsRoot == BaseConfiguration.GitHubVcs) {
    {
      endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
      equals("container.engine.osType", "linux")
    }
  } else null,
  artifactRules = Utils.CommonParams.BazelTestlogsArtifactRules,
  steps = {
    bazel {
      val reportErrors = "--jvmopt=\"-DDO_NOT_REPORT_ERRORS=true\""
      val cachePath = "--jvmopt=\"-Dbazel.ide.starter.test.cache.directory=%system.teamcity.build.tempDir%\""
      val memArg = "--jvmopt=\"-Xmx12g\""
      val sandboxArg = "--sandbox_writable_path=/"
      val actionEnvArg = "--action_env=PATH"

      val sysArgs = "$reportErrors $cachePath $memArg $sandboxArg $actionEnvArg"

      this.name = "run $targets"
      id = "run_${targets.replace(":", "_")}"
      command = "test"
      this.targets = targets
      arguments = "$sysArgs ${Utils.CommonParams.BazelCiSpecificArgs} $testSpecificArgs"
      toolPath = "/usr/local/bin"
      logging = BazelStep.Verbosity.Diagnostic
      Utils.DockerParams.get().forEach { (key, value) ->
        param(key, value)
      }
    }
    script {
      this.name = "copy test logs"
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

sealed class HotswapTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Hotswap test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/hotswap"
)

sealed class ReopenWithoutResyncTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Reopen without Resync test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/reopen/noResync"
)

sealed class ExternalRepoResolveTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "External repo resolve test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/references:ExternalRepoResolveTest"
)

sealed class JarSourceExcludeTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Compiled source code inside jar exclude test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/java:CompiledSourceCodeInsideJarExcludeTest",
)

sealed class BazelProjectModelModifierTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Bazel Project Model Modifier test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/flow/modify:BazelProjectModelModifierTest"
)

sealed class BazelCoverageTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Bazel Coverage test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/coverage:BazelCoverageTest"
)

sealed class TestResultsTreeTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Test Results Tree test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree"
)

sealed class RunLineMarkerTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Run Line Marker test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testRunLineMarker"
)

sealed class ImportRunConfigurationsTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Import run configurations test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/import:ImportRunConfigurationsSyncHookTest"
)

sealed class NonModuleTargetsTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Sync non module targets test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/nonmodule/sync:NonModuleTargetsTest"
)

sealed class RunAllTestsActionTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Run all tests action test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree:runAllTestsActionTest"
)

sealed class DisabledKotlinPluginTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Sync project with disabled kotlin plugin test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:DisabledKotlinPluginTest"
)

sealed class PyCharmPluginTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Sync project in PyCharm test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:PyCharmTest"
)

sealed class GoLandPluginTest(
  vcsRoot: GitVcsRoot,
) : IdeStarterTests(
  name = "Sync project in GoLand test",
  vcsRoot = vcsRoot,
  targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:GoLandTest"
)

object HotswapTestGitHub : HotswapTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object HotswapTestSpace : HotswapTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object ReopenWithoutResyncTestGitHub : ReopenWithoutResyncTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object ReopenWithoutResyncTestSpace : ReopenWithoutResyncTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object ExternalRepoResolveTestGitHub : ExternalRepoResolveTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object ExternalRepoResolveTestSpace : ExternalRepoResolveTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object JarSourceExcludeTestGitHub : JarSourceExcludeTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object JarSourceExcludeTestSpace : JarSourceExcludeTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object BazelProjectModelModifierTestGitHub : BazelProjectModelModifierTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object BazelProjectModelModifierTestSpace : BazelProjectModelModifierTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object BazelCoverageTestGitHub : BazelCoverageTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object BazelCoverageTestSpace : BazelCoverageTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object TestResultsTreeTestGitHub : TestResultsTreeTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object TestResultsTreeTestSpace : TestResultsTreeTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object RunLineMarkerTestGitHub : RunLineMarkerTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object RunLineMarkerTestSpace : RunLineMarkerTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object ImportRunConfigurationsTestGitHub : ImportRunConfigurationsTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object ImportRunConfigurationsTestSpace : ImportRunConfigurationsTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object NonModuleTargetsTestGitHub : NonModuleTargetsTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object NonModuleTargetsTestSpace : NonModuleTargetsTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object RunAllTestsActionTestGitHub : RunAllTestsActionTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object RunAllTestsActionTestSpace : RunAllTestsActionTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object DisabledKotlinPluginTestGitHub : DisabledKotlinPluginTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object DisabledKotlinPluginTestSpace : DisabledKotlinPluginTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object PyCharmTestGitHub : PyCharmPluginTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object PyCharmTestSpace : PyCharmPluginTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)

object GoLandTestGitHub : GoLandPluginTest(
  vcsRoot = BaseConfiguration.GitHubVcs
)

object GoLandTestSpace : GoLandPluginTest(
  vcsRoot = BaseConfiguration.SpaceVcs
)
