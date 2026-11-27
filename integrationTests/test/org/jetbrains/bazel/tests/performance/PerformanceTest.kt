package org.jetbrains.bazel.tests.performance

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.extended.metrics.publishing.newIJPerfPublisher
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.testFramework.PerformanceUnitTest
import com.intellij.testFramework.TeamCityLogger
import com.intellij.tools.ide.metrics.collector.OpenTelemetrySpanCollector
import com.intellij.tools.ide.metrics.collector.metrics.MetricsSelectionStrategy
import com.intellij.tools.ide.metrics.collector.metrics.PerformanceMetrics
import com.intellij.tools.ide.metrics.collector.starter.collector.StarterTelemetryJsonMeterCollector
import com.intellij.tools.ide.metrics.collector.starter.publishing.MetricsPublisher
import com.intellij.tools.ide.metrics.collector.starter.publishing.publishOnlyProvidedMetrics
import com.intellij.tools.ide.metrics.collector.telemetry.SpanFilter
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import com.intellij.util.ThrowableRunnable
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.HermeticityGuard
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.getProjectInfoFromSystemProperties
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.openBspToolWindow
import org.jetbrains.bazel.base.waitForBazelSync
import org.jetbrains.bazel.sync.findExecutableInPath
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * ```sh
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.performance.PerformanceTest --test_output=errors --nocache_test_results
 * ```
 */
@PerformanceUnitTest
class PerformanceTest : IdeStarterBaseProjectTest() {

  @TestFactory
  fun `sync performance metrics should be collected during project import`(): List<DynamicTest> {
    val projectName = System.getenv("BAZEL_PERF_PROJECT_NAME") ?: "performance"
    return listOf(
      DynamicTest.dynamicTest("sync performance - $projectName") {
        val context = createContext(projectName, IdeaBazelCases.withProject(getProjectInfoFromSystemProperties()))
          .applyVMOptionsPatch { withXmx(11264) }
        if (System.getProperty("bazel.ide.starter.test.prefetch.dependencies").toBoolean()) {
          prefetchBazelDependencies(context)
        }
        val startResult =
          try {
            context
              .runIdeWithDriver(runTimeout = timeout)
              .useDriverAndCloseIde {
                ideFrame {
                  step("Collect performance metrics during Bazel sync") {
                    execute { startRecordingMaxMemory() }
                    execute { takeScreenshot("startSync") }
                    execute { openBspToolWindow() }
                    execute { takeScreenshot("openBspToolWindow") }
                    execute { waitForBazelSync() }
                    execute { recordMemory("bsp.used.after.sync.mb") }
                    execute { stopRecordingMaxMemory() }
                    execute { waitForSmartMode() }
                    execute { recordMemory("bsp.used.after.indexing.mb") }
                  }
                  waitForIndicators(10.minutes)
                }
              }
          }
          finally {
            copyIdeBazelLogs(context)
          }

        val spans = OpenTelemetrySpanCollector(SpanFilter.nameEquals("bsp.sync.project.ms")).collect(startResult.runContext.logsDir)

        val meters =
          StarterTelemetryJsonMeterCollector(MetricsSelectionStrategy.LATEST) {
            it.name.startsWith("bsp.")
          }.collect(startResult.runContext).map {
            PerformanceMetrics.Metric.newCounter(it.id.name, it.value)
          }

        check(spans.size > 1) { "No spans received" }
        check(meters.size > 1) { "No performance metrics received" }

        MetricsPublisher.newIJPerfPublisher()
          .configure { this.projectName = projectName }
          .publishOnlyProvidedMetrics(startResult, spans + meters)
      }
    )
  }
}

/**
 * Fetches the external repositories of the project before the IDE starts, so that the measured sync does not download them.
 * Bazel runs in the project root, so it reads the generated `user.bazelrc` and fills the output base and the repository cache of the IDE.
 * The prefetch is best effort: after a failure or a timeout, the sync fetches the rest.
 */
private fun prefetchBazelDependencies(context: IDETestContext) {
  TeamCityLogger.block("Prefetch Bazel dependencies", ThrowableRunnable {
    val environment = System.getenv() + context.ide.vmOptions.environmentVariables
    val bazel = findBazelLauncher(environment)
    if (bazel == null) {
      println("Bazel prefetch skipped: no Bazel launcher found")
      return@ThrowableRunnable
    }
    val threads = System.getProperty("bazel.ide.starter.test.prefetch.loading.phase.threads")?.toIntOrNull() ?: 16
    val timeout = (System.getProperty("bazel.ide.starter.test.prefetch.timeout.seconds")?.toIntOrNull() ?: 3600).seconds
    val projectRoot = with(BazelProjectConfigurer) { context.resolvedBazelProjectHome }
    try {
      val log = context.paths.testHome.resolve("log").createDirectories().resolve("bazel-prefetch.log")
      val (exitCode, duration) = measureTimedValue {
        runBazel(
          bazel, projectRoot, environment, log, timeout,
          "fetch", "--all", "--keep_going", "--loading_phase_threads=$threads", "--http_max_parallel_downloads=$threads",
          "--curses=no", "--color=no", "--show_progress_rate_limit=30",
        )
      }
      println("Bazel prefetch with $bazel ${exitCode?.let { "exited with code $it" } ?: "timed out"} after $duration, output: $log")
      // The measured sync starts a new Bazel server, as it does without the prefetch
      if (runBazel(bazel, projectRoot, environment, log, 5.minutes, "shutdown") == null) {
        println("Bazel shutdown after the prefetch timed out")
      }
    }
    catch (e: IOException) {
      println("Bazel prefetch failed: $e")
    }
  })
}

/**
 * The Bazel that the IDE finds on its `PATH`, else the Bazelisk that `bazel.cmd` of the monorepo downloads.
 * The `PATH` of the test has no Bazel on the Linux agents, and the Bazelisk honors the `.bazelversion` of the project.
 */
private fun findBazelLauncher(environment: Map<String, String>): Path? =
  environment["PATH"]?.let { findExecutableInPath(it, "bazel", "bazelisk") }
  ?: Path.of(System.getProperty("user.home"), ".cache", "JetBrains", "monorepo-bazelisk", "bazel").takeIf { it.isExecutable() }

/**
 * Copies the server logs of the Bazel that the IDE started into the IDE log dir.
 * The teardown deletes the output base before TeamCity publishes the artifacts.
 */
private fun copyIdeBazelLogs(context: IDETestContext) {
  val projectRoot = with(BazelProjectConfigurer) { context.resolvedBazelProjectHome }
  val outputBase = HermeticityGuard.outputBase(projectRoot)?.let { Path.of(it) } ?: return
  try {
    val target = context.paths.testHome.resolve("log").resolve("ide-bazel").createDirectories()
    val logs = listOf("java.log", "command.log").map(outputBase::resolve).filter { it.exists() }.map { it.toRealPath() } +
               outputBase.listDirectoryEntries("command*.profile.gz")
    logs.forEach { it.copyTo(target.resolve(it.name), overwrite = true) }
  }
  catch (e: IOException) {
    println("IDE Bazel logs not copied from $outputBase: $e")
  }
}

/** Returns the exit code, or `null` when Bazel ran longer than [timeout] and was stopped. */
private fun runBazel(
  bazel: Path,
  projectRoot: Path,
  environmentVariables: Map<String, String>,
  log: Path,
  timeout: Duration,
  vararg args: String,
): Int? {
  val process = ProcessBuilder(bazel.toString(), *args)
    .directory(projectRoot.toFile())
    .redirectErrorStream(true)
    .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
    .apply { environment().putAll(environmentVariables) }
    .start()
  if (process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)) return process.exitValue()
  process.destroy()
  if (!process.waitFor(1, TimeUnit.MINUTES)) process.destroyForcibly().waitFor()
  return null
}

private fun <T : CommandChain> T.startRecordingMaxMemory(): T {
  addCommand(CMD_PREFIX + "startRecordingMaxMemory")
  return this
}

private fun <T : CommandChain> T.stopRecordingMaxMemory(): T {
  addCommand(CMD_PREFIX + "stopRecordingMaxMemory")
  return this
}

private fun <T : CommandChain> T.recordMemory(gaugeName: String): T {
  addCommand(CMD_PREFIX + "recordMemory", gaugeName)
  return this
}
