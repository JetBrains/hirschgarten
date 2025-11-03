package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.VcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

/**
 * Data class for benchmark test definitions
 */
data class BenchmarkDef(
  val name: String,
  val useCurrentBazelVersion: Boolean = false,
  val testSpecificArgs: String = ""
)

/**
 * Benchmark test configuration for the Bazel plugin.
 * 
 * @param benchmarkDef The benchmark definition containing test parameters
 */
class PluginBenchmarkTest(
  private val benchmarkDef: BenchmarkDef,
  customVcsRoot: VcsRoot? = VcsRootHirschgarten,
) : BaseBuildType(
  name = "[benchmark] ${benchmarkDef.name}",
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
    equals("container.engine.osType", "linux")
  },
  artifactRules = CommonParams.BazelTestlogsArtifactRules,
  customVcsRoot = customVcsRoot,
  steps = {
    val dockerParams = if (benchmarkDef.useCurrentBazelVersion) {
      DockerParams.get().toMutableMap().apply {
        set("plugin.docker.run.parameters",
            get("plugin.docker.run.parameters") + "\n-v %teamcity.build.checkoutDir%/.bazelversion:/home/hirschuser/project_10/.bazelversion")
      }
    } else {
      DockerParams.get()
    }

    bazel {
      val url = "--jvmopt=\"-Dbazel.ide.starter.test.teamcity.url=https://bazel.teamcity.com\""
      val projectNameArg = "--jvmopt=\"-Dbazel.ide.starter.test.project.name=benchmark_10_targets\""
      val reportErrors = "--jvmopt=\"-DDO_NOT_REPORT_ERRORS=true\""
      val projectPath = "--jvmopt=\"-Dbazel.ide.starter.test.project.path=/home/hirschuser/project_10\""
      val cachePath = "--jvmopt=\"-Dbazel.ide.starter.test.cache.directory=%system.teamcity.build.tempDir%\""
      val memArg = "--jvmopt=\"-Xmx12g\""
      val sandboxArg = "--sandbox_writable_path=/"
      val actionEnvArg = "--action_env=PATH"

      val sysArgs = "$url $projectNameArg $reportErrors $projectPath $cachePath $memArg $sandboxArg $actionEnvArg"

      this.name = "run benchmark"
      id = "run_benchmark"
      command = "test"
      targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/performance"
      arguments = "$sysArgs ${CommonParams.BazelCiSpecificArgs} ${benchmarkDef.testSpecificArgs}"
      toolPath = "/usr/local/bin"
      logging = BazelStep.Verbosity.Diagnostic
      dockerParams.forEach { (key, value) ->
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

/**
 * Factory for creating plugin benchmark test build types.
 */
object PluginBenchmarkFactory {
  // Define the available benchmark tests
  private val benchmarkTests = listOf(
    BenchmarkDef("Plugin BSP 10 targets newest available Bazel"),
    BenchmarkDef("Plugin BSP 10 targets with current Bazel", useCurrentBazelVersion = true)
  )
  
  /** All benchmark test build types. */
  val AllBenchmarkTests: List<BaseBuildType> by lazy { createBenchmarkTests() }
  val AllBenchmarkTestsSpace: List<BaseBuildType> by lazy { createBenchmarkTestsSpace() }
  
  private fun createBenchmarkTests(): List<BaseBuildType> =
    benchmarkTests.map { benchmarkDef ->
      PluginBenchmarkTest(benchmarkDef)
    }

  private fun createBenchmarkTestsSpace(): List<BaseBuildType> =
    benchmarkTests.map { benchmarkDef ->
      PluginBenchmarkTest(benchmarkDef, customVcsRoot = VcsRootHirschgartenSpace)
    }
}
