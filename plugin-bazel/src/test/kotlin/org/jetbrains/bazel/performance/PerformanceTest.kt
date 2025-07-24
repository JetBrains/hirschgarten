package org.jetbrains.bazel.performance

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.teamcity.TeamCityCIServer
import com.intellij.ide.starter.ci.teamcity.TeamCityClient
import com.intellij.ide.starter.ci.teamcity.asTeamCity
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.IDEStartResult
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.project.RemoteArchiveProjectInfo
import com.intellij.ide.starter.runner.CurrentTestMethod
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.openapi.util.BuildNumber
import com.intellij.tools.ide.metrics.collector.OpenTelemetrySpanCollector
import com.intellij.tools.ide.metrics.collector.metrics.MetricsSelectionStrategy
import com.intellij.tools.ide.metrics.collector.metrics.PerformanceMetrics
import com.intellij.tools.ide.metrics.collector.publishing.PerformanceMetricsDto
import com.intellij.tools.ide.metrics.collector.starter.collector.StarterTelemetryJsonMeterCollector
import com.intellij.tools.ide.metrics.collector.telemetry.SpanFilter
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.startup.IntellijTelemetryManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance
import java.nio.file.Files
import java.nio.file.Path

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/performance --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class PerformanceTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() = getProjectInfoFromSystemProperties()

  @BeforeEach
  fun setUp() {
    TelemetryManager.provideTelemetryManager(IntellijTelemetryManager)
  }

  @Test
  fun openBazelProject() {
    val commands =
      CommandChain()
        .startRecordingMaxMemory()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .recordMemory("bsp.used.after.sync.mb")
        .openBspToolWindow()
        .takeScreenshot("openBspToolWindow")
        .stopRecordingMaxMemory()
        .waitForSmartMode()
        .recordMemory("bsp.used.after.indexing.mb")
        .exitApp()
    val startResult = createContext().runIDE(commands = commands, runTimeout = timeout)

    val spans = OpenTelemetrySpanCollector(SpanFilter.nameEquals("bsp.sync.project.ms")).collect(startResult.runContext.logsDir)

    val meters =
      StarterTelemetryJsonMeterCollector(MetricsSelectionStrategy.LATEST) {
        it.name.startsWith("bsp.")
      }.collect(startResult.runContext).map {
        PerformanceMetrics.Metric.newCounter(it.id.name, it.value)
      }

    check(spans.size > 1) { "No spans received" }
    check(meters.size > 1) { "No performance metrics received" }

    startResult.publishPerformanceMetrics(metrics = spans + meters)
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

private fun <T : CommandChain> T.openBspToolWindow(): T {
  addCommand(CMD_PREFIX + "openBspToolWindow")
  return this
}

// This is copied from the monorepo, because these functions aren't yet published to Maven.

/**
 * Copies the files to a temp directory to make sure that the artifacts are published by TeamCity.
 * Otherwise, the files might be cleaned before the TeamCity build finishes.
 */
fun IDEStartResult.publishTeamCityArtifacts(
  source: Path,
  artifactPath: String = runContext.contextName,
  artifactName: String = source.fileName.toString(),
  zipContent: Boolean = true,
) {
  TeamCityClient.publishTeamCityArtifacts(
    source = source,
    artifactPath = artifactPath,
    artifactName = artifactName,
    zipContent = zipContent,
  )
}

fun IDEStartResult.publishPerformanceMetrics(
  artifactPath: String = runContext.contextName,
  artifactName: String = "metrics.performance.json",
  projectName: String = runContext.contextName,
  metrics: Collection<PerformanceMetrics.Metric>,
) {
  val metricsSortedByName = metrics.sortedBy { it.id.name }

  val appMetrics =
    PerformanceMetricsDto.create(
      projectName = projectName,
      buildNumber = BuildNumber.fromStringWithProductCode(context.ide.build, context.ide.productCode)!!,
      methodName = getMethodName(),
      projectURL = context.getProjectURL(),
      projectDescription = context.getProjectDescription(),
      metrics = metricsSortedByName,
      generated = CIServer.instance.asTeamCity().buildStartTime,
    )

  val reportFile = Files.createTempFile(runContext.snapshotsDir, artifactName, null)
  jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), appMetrics)
  if (CIServer.instance.asTeamCity().buildId == TeamCityCIServer.LOCAL_RUN_ID) {
    println(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(appMetrics))
  }
  publishTeamCityArtifacts(reportFile, zipContent = false, artifactPath = artifactPath, artifactName = artifactName)
}

private fun getMethodName(): String {
  val method = di.direct.instance<CurrentTestMethod>().get()
  return if (method == null) "" else "${method.clazz}#${method.name}"
}

private fun IDETestContext.getProjectDescription(): String = testCase.projectInfo.getDescription()

private fun IDETestContext.getProjectURL(): String {
  val url =
    when (val projectInfo = this.testCase.projectInfo) {
      is RemoteArchiveProjectInfo -> projectInfo.projectURL
      is GitProjectInfo -> projectInfo.repositoryUrl
      else -> ""
    }
  return url
}
