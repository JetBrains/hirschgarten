package org.jetbrains.bazel.tests.performance

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.extended.metrics.publishing.newIJPerfPublisher
import com.intellij.ide.starter.junit5.PerformanceTest
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
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.getProjectInfoFromSystemProperties
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.openBspToolWindow
import org.jetbrains.bazel.base.waitForBazelSync
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.performance.PerformanceTest --test_output=errors --nocache_test_results
 * ```
 */
@PerformanceUnitTest
@PerformanceTest
class PerformanceTest : IdeStarterBaseProjectTest() {

  @TestFactory
  fun `sync performance metrics should be collected during project import`(): List<DynamicTest> {
    val projectName = System.getenv("BAZEL_PERF_PROJECT_NAME") ?: "performance"
    return listOf(
      DynamicTest.dynamicTest("sync performance - $projectName") {
        val context = createContext(projectName, IdeaBazelCases.withProject(getProjectInfoFromSystemProperties()))
          .applyVMOptionsPatch { withXmx(11264) }
        val startResult =
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
