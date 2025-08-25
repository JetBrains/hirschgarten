package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

/**
 * Base class for IDE starter tests.
 * 
 * @param testName The name of the test, used in the build name
 * @param targets The Bazel targets to test
 * @param testSpecificArgs Additional arguments specific to this test
 */
class IdeStarterTest(
  private val testName: String,
  private val targets: String,
  private val testSpecificArgs: String = ""
) : BaseBuildType(
  name = "[ide-starter] $testName",
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
    equals("container.engine.osType", "linux")
  },
  artifactRules = CommonParams.BazelTestlogsArtifactRules,
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
      arguments = "$sysArgs ${CommonParams.BazelCiSpecificArgs} $testSpecificArgs"
      toolPath = "/usr/local/bin"
      logging = BazelStep.Verbosity.Diagnostic
      DockerParams.get().forEach { (key, value) ->
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

/**
 * Data class for test definitions
 */
data class TestDef(
  val name: String,
  val target: String,
  val testSpecificArgs: String = ""
)

/**
 * Factory for creating IDE-starter test build types.
 */
object IdeStarterTestFactory {
  /** All IDE-starter test build types. */
  val AllIdeStarterTests: List<BaseBuildType> by lazy { createIdeStarterTests() }

  private fun createIdeStarterTests(): List<BaseBuildType> =
    ideStarterTests.map { testDef ->
      IdeStarterTest(testDef.name, testDef.target, testDef.testSpecificArgs)
    }

  // Define the available IDE-starter tests
  private val ideStarterTests = listOf(
    TestDef("Hotswap test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/hotswap"),
    TestDef("GoLand sync test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/golang/resolve/golandSync"),
    TestDef("Coroutine debug test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/kotlin/coroutineDebug"),
    TestDef("Reopen without Resync test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/reopen/noResync"),
    TestDef("External repo resolve test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/references:ExternalRepoResolveTest"),
    TestDef("Compiled source code inside jar exclude test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/java:CompiledSourceCodeInsideJarExcludeTest"),
    TestDef("Bazel Project Model Modifier test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/flow/modify:BazelProjectModelModifierTest"),
    TestDef("Bazel Coverage test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/coverage:BazelCoverageTest"),
    TestDef("Test Results Tree test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree"),
    TestDef("Run Line Marker test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testRunLineMarker"),
    TestDef("Import run configurations test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/import:ImportRunConfigurationsSyncHookTest"),
    TestDef("Sync non module targets test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/nonmodule/sync:NonModuleTargetsTest"),
    TestDef("Run all tests action test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree:runAllTestsActionTest"),
    TestDef("Sync project with disabled kotlin plugin test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:DisabledKotlinPluginTest"),
    TestDef("Sync project in PyCharm test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:PyCharmTest"),
    TestDef("Check red code in PyCharm test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:SimplePythonTest"),
    TestDef("Fast build test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/fastbuild"),
    TestDef("Project view open test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/settings:project_view_open_test"),
    TestDef("Move Kotlin file test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/kotlin/move:moveKotlinFileTest"),
    TestDef("Recover bazelbsp", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/sync/recoverDotBazelBsp"),
    TestDef("Update Bazel version test", "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/server/connection/bazelVersionUpdate")
  )
}
