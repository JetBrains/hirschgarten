package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

// Base class for IDE starter tests
open class IdeStarterTest(
  targets: String,
  name: String,
  testSpecificArgs: String = ""
) : BaseConfiguration.BaseBuildType(
  name = "[ide-starter] $name",
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
    equals("container.engine.osType", "linux")
  },
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

// Define all IDE starter test objects
object IdeStarter {
  object HotswapTest : IdeStarterTest(
    name = "Hotswap test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/hotswap"
  )

  object GoLandSyncTest : IdeStarterTest(
    name = "GoLand sync test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/golang/resolve/golandSync"
  )

  object CoroutineDebugTest : IdeStarterTest(
    name = "Coroutine debug test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/kotlin/coroutineDebug"
  )

  object ReopenWithoutResyncTest : IdeStarterTest(
    name = "Reopen without Resync test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/reopen/noResync"
  )

  object ExternalRepoResolveTest : IdeStarterTest(
    name = "External repo resolve test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/references:ExternalRepoResolveTest"
  )

  object JarSourceExcludeTest : IdeStarterTest(
    name = "Compiled source code inside jar exclude test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/java:CompiledSourceCodeInsideJarExcludeTest",
  )

  object BazelProjectModelModifierTest : IdeStarterTest(
    name = "Bazel Project Model Modifier test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/flow/modify:BazelProjectModelModifierTest"
  )

  object BazelCoverageTest : IdeStarterTest(
    name = "Bazel Coverage test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/coverage:BazelCoverageTest"
  )

  object TestResultsTreeTest : IdeStarterTest(
    name = "Test Results Tree test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree"
  )

  object RunLineMarkerTest : IdeStarterTest(
    name = "Run Line Marker test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testRunLineMarker"
  )

  object ImportRunConfigurationsTest : IdeStarterTest(
    name = "Import run configurations test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/import:ImportRunConfigurationsSyncHookTest"
  )

  object NonModuleTargetsTest : IdeStarterTest(
    name = "Sync non module targets test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/nonmodule/sync:NonModuleTargetsTest"
  )

  object RunAllTestsActionTest : IdeStarterTest(
    name = "Run all tests action test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree:runAllTestsActionTest"
  )

  object DisabledKotlinPluginTest : IdeStarterTest(
    name = "Sync project with disabled kotlin plugin test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:DisabledKotlinPluginTest"
  )

  object PyCharmTest : IdeStarterTest(
    name = "Sync project in PyCharm test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:PyCharmTest"
  )

  object PyCharmImportRedCodeTest : IdeStarterTest(
    name = "Check red code in PyCharm test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:SimplePythonTest"
  )

  object FastBuildTest : IdeStarterTest(
    name = "Fast build test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/fastbuild"
  )

  object ProjectViewOpenTest : IdeStarterTest(
    name = "Project view open test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/settings:project_view_open_test"
  )

  object MoveKotlinFileTest : IdeStarterTest(
    name = "Move Kotlin file test",
    targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/kotlin/move:moveKotlinFileTest"
  )
}
