package org.jetbrains.bsp.performance.testing

import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.runner.Starter
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.metrics.collector.metrics.MetricsSelectionStrategy
import com.intellij.tools.ide.metrics.collector.metrics.PerformanceMetrics
import com.intellij.tools.ide.metrics.collector.starter.collector.StarterTelemetryCsvMeterCollector
import com.intellij.tools.ide.metrics.collector.starter.collector.getMetricsFromSpanAndChildren
import com.intellij.tools.ide.metrics.collector.telemetry.SpanFilter
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.toPath
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class BazelTest {
  @BeforeEach
  fun initDi() {
    di = DI {
      extend(di)
      bindSingleton(tag = "teamcity.uri", overrides = true) {
        val teamcityUrl = System.getProperty("bsp.benchmark.teamcity.url") ?: "https://buildserver.labs.intellij.net"
        URI(teamcityUrl).normalize()
      }
      bindSingleton<CIServer>(overrides = true) { TeamCityCIServer() }
    }
  }

  @Test
  fun openBazelProject() {
    val projectInfo = getProjectInfoFromSystemProperties()
    val projectName = System.getProperty("bsp.benchmark.project.name") ?: "bazel"
    val testCase = TestCase(IdeProductProvider.IC, projectInfo)
      .withBuildNumber(System.getProperty("bsp.benchmark.platform.version"))
    val context = Starter.newContext(projectName, testCase)
      .propagateSystemProperty("idea.diagnostic.opentelemetry.otlp")
    installBazelPlugin(context)

    val commands = CommandChain()
      .startMemoryProfiling()
      .openBspToolWindow()
      .takeScreenshot("startSync")
      .waitForBazelSync()
      .stopMemoryProfiling()
      .exitApp()
    val startResult = context.runIDE(commands = commands)

    val spans = getMetricsFromSpanAndChildren(startResult, SpanFilter.nameEquals("bsp.sync.project.ms"))

    val meters = StarterTelemetryCsvMeterCollector(MetricsSelectionStrategy.LATEST) {
        (name, _) -> name.startsWith("bsp.")
    }.collect(startResult.runContext).map {
      PerformanceMetrics.Metric.newCounter(it.id.name, it.value)
    }

    startResult.publishPerformanceMetrics(metrics = spans + meters)
  }

  private fun getProjectInfoFromSystemProperties(): ProjectInfoSpec {
    val localProjectPath = System.getProperty("bsp.benchmark.project.path")
    if (localProjectPath != null) {
      return LocalProjectInfo(
        projectDir = Path.of(localProjectPath),
        isReusable = true,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )
    }

    val projectUrl = System.getProperty("bsp.benchmark.project.url") ?: "https://github.com/JetBrains/bazel-bsp.git"
    val commitHash = System.getProperty("bsp.benchmark.commit.hash").orEmpty()
    val branchName = System.getProperty("bsp.benchmark.branch.name") ?: "master"
    val projectHomeRelativePath: String? = System.getProperty("bsp.benchmark.project.home.relative.path")

    return GitProjectInfo(
      repositoryUrl = projectUrl,
      commitHash = commitHash,
      branchName = branchName,
      projectHomeRelativePath = { if (projectHomeRelativePath != null) it.resolve(projectHomeRelativePath) else it },
      isReusable = true,
      configureProjectBeforeUse = ::configureProjectBeforeUse,
    )
  }

  private fun configureProjectBeforeUse(context: IDETestContext) {
    runBazelClean(context)
    (context.resolvedProjectHome / ".idea").deleteRecursively()
    (context.resolvedProjectHome / ".bazelbsp").deleteRecursively()
    (context.resolvedProjectHome / "build.gradle").deleteIfExists()
    (context.resolvedProjectHome / "build.gradle.kts").deleteIfExists()
    (context.resolvedProjectHome / "settings.gradle").deleteIfExists()
    (context.resolvedProjectHome / "settings.gradle.kts").deleteIfExists()
    (context.resolvedProjectHome / "gradlew").deleteIfExists()
    (context.resolvedProjectHome / "gradlew.bat").deleteIfExists()
    createProjectViewFile(context)
  }

  private fun runBazelClean(context: IDETestContext) {
    val exitCode = ProcessBuilder("bazel", "clean", "--expunge")
      .directory(context.resolvedProjectHome.toFile())
      .start()
      .waitFor()
    check(exitCode == 0) { "Bazel clean exited with code $exitCode" }
  }

  private fun createProjectViewFile(context: IDETestContext) {
    val projectView = context.resolvedProjectHome / "projectview.bazelproject"
    val targetsList = System.getProperty("bsp.benchmark.target.list")
    if (projectView.exists() && targetsList == null) return
    projectView.writeText("""
      targets:
        ${targetsList ?: "//..."}
    """
      .trimIndent())
  }

  private fun installBazelPlugin(context: IDETestContext) {
    val bspPluginZip = javaClass.classLoader.getResource(System.getProperty("bsp.benchmark.plugin.zip"))!!
    context.pluginConfigurator.installPluginFromPath(bspPluginZip.toURI().toPath())
    context.pluginConfigurator.installPluginFromPluginManager("org.jetbrains.bazel", context.ide, "nightly")
  }

  private fun IDETestContext.propagateSystemProperty(key: String): IDETestContext {
    val value = System.getProperty(key) ?: return this
    applyVMOptionsPatch {
      addSystemProperty(key, value)
    }
    return this
  }
}

private fun <T : CommandChain> T.waitForBazelSync(): T {
  addCommand(CMD_PREFIX + "waitForBazelSync")
  return this
}

private fun <T : CommandChain> T.startMemoryProfiling(): T {
  addCommand(CMD_PREFIX + "startMemoryProfiling")
  return this
}

private fun <T : CommandChain> T.stopMemoryProfiling(): T {
  addCommand(CMD_PREFIX + "stopMemoryProfiling")
  return this
}

private fun <T : CommandChain> T.openBspToolWindow(): T {
  addCommand(CMD_PREFIX + "openBspToolWindow")
  return this
}
