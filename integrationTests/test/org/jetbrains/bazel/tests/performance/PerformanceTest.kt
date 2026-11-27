package org.jetbrains.bazel.tests.performance

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.extended.metrics.publishing.newIJPerfPublisher
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.testFramework.PerformanceUnitTest
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
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.HermeticityGuard
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.getProjectInfoFromSystemProperties
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.openBspToolWindow
import org.jetbrains.bazel.base.waitForBazelSync
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.Duration.Companion.minutes

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
