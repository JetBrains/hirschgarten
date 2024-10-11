package org.jetbrains.bsp.performance.testing

import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.teamcity.TeamCityCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.runner.Starter
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.metrics.collector.metrics.MetricsSelectionStrategy
import com.intellij.tools.ide.metrics.collector.metrics.PerformanceMetrics
import com.intellij.tools.ide.metrics.collector.starter.collector.StarterTelemetryJsonMeterCollector
import com.intellij.tools.ide.metrics.collector.starter.collector.getMetricsFromSpanAndChildren
import com.intellij.tools.ide.metrics.collector.telemetry.SpanFilter
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
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
    di =
      DI {
        extend(di)
        bindSingleton(tag = "teamcity.uri", overrides = true) {
          val teamcityUrl = System.getProperty("bsp.benchmark.teamcity.url") ?: "https://buildserver.labs.intellij.net"
          URI(teamcityUrl).normalize()
        }
        bindSingleton<CIServer>(overrides = true) { TeamCityCIServer() }
        System.getProperty("bsp.benchmark.cache.directory")?.let { intellijBspRoot ->
          bindSingleton<GlobalPaths>(overrides = true) {
            object : GlobalPaths(Path.of(intellijBspRoot)) {}
          }
        }
      }
  }

  @Test
  fun openBazelProject() {
    val projectInfo = getProjectInfoFromSystemProperties()
    val projectName = System.getProperty("bsp.benchmark.project.name") ?: "hirschgarten"
    val testCase =
      TestCase(IdeProductProvider.IC, projectInfo).let { testCase ->
        // TODO replace with .useEAP(buildNumber: String) when it becomes public
        val useEAP = testCase.javaClass.getDeclaredMethod("useEAP", String::class.java)
        useEAP.isAccessible = true
        useEAP.invoke(testCase, System.getProperty("bsp.benchmark.platform.version")) as TestCase<*>
      }
    val context =
      Starter
        .newContext(projectName, testCase)
        .executeRightAfterIdeOpened(true)
        .propagateSystemProperty("idea.diagnostic.opentelemetry.otlp")
        .patchPathVariable()
        .patchSystemProperties()
    installPlugin(context, System.getProperty("bsp.benchmark.bsp.plugin.zip"))
    installPlugin(context, System.getProperty("bsp.benchmark.bazel.plugin.zip"))

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
    val startResult = context.runIDE(commands = commands)

    val spans = getMetricsFromSpanAndChildren(startResult, SpanFilter.nameEquals("bsp.sync.project.ms"))

    val meters =
      StarterTelemetryJsonMeterCollector(MetricsSelectionStrategy.LATEST) {
        it.name.startsWith("bsp.")
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
    val projectUrl = System.getProperty("bsp.benchmark.project.url") ?: "https://github.com/JetBrains/hirschgarten.git"
    val commitHash = System.getProperty("bsp.benchmark.commit.hash").orEmpty()
    val branchName = System.getProperty("bsp.benchmark.branch.name") ?: "main"
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
    val exitCode =
      ProcessBuilder("bazel", "clean", "--expunge")
        .directory(context.resolvedProjectHome.toFile())
        .start()
        .waitFor()
    check(exitCode == 0) { "Bazel clean exited with code $exitCode" }
  }

  private fun createProjectViewFile(context: IDETestContext) {
    val projectView = context.resolvedProjectHome / "projectview.bazelproject"
    val targetsList = System.getProperty("bsp.benchmark.target.list")
    if (projectView.exists() && targetsList == null) return
    projectView.writeText(
      """
      targets:
        ${targetsList ?: "//..."}
      """.trimIndent(),
    )
  }

  fun extractFileFromJar(
    jarFilePath: String,
    fileName: String,
    destFilePath: String,
  ) {
    JarFile(jarFilePath).use { jarFile ->
      val jarEntry = jarFile.getJarEntry(fileName)
      jarFile.getInputStream(jarEntry).use { input ->
        FileOutputStream(destFilePath).use { output ->
          input.copyTo(output)
        }
      }
    }
  }

  private fun installPlugin(context: IDETestContext, pluginZipLocation: String) {
    val zipUrl = javaClass.classLoader.getResource(pluginZipLocation)!!
    // check if the zip is inside a jar
    // if it is, it will look something like this: jar:file:/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/e06fcdf30b05e14cf4fddcd75b67a39c/execroot/_main/bazel-out/k8-fastbuild/bin/performance-testing/performance-testing.jar!/plugin.zip
    // we need to extract it to a temporary file then
    // This happens when the test is run through bazel
    if (zipUrl.protocol == "jar") {
      val jarPath = zipUrl.path.substringAfter("file:").substringBefore("!/")
      // extract the jar (which is a zip) to a temporary directory
      val tempDir = createTempDirectory("bsp-plugin")
      val targetPath = (tempDir / pluginZipLocation).also { it.createParentDirectories() }

      extractFileFromJar(jarPath, pluginZipLocation, targetPath.toString())
      context.pluginConfigurator.installPluginFromPath(targetPath)
      tempDir.deleteRecursively()
    } else {
      context.pluginConfigurator.installPluginFromPath(zipUrl.toURI().toPath())
    }
  }

  private fun IDETestContext.propagateSystemProperty(key: String): IDETestContext {
    val value = System.getProperty(key) ?: return this
    applyVMOptionsPatch {
      addSystemProperty(key, value)
    }
    return this
  }

  /**
   * Bazel adds the current version of itself to the PATH variable that is passed to the test.
   * This causes `.bazelversion` of the test project to be ignored.
   */
  private fun IDETestContext.patchPathVariable(): IDETestContext {
    var path = checkNotNull(System.getenv("PATH")) { "PATH is null" }
    val paths = path.split(File.pathSeparator)
    if (paths[0] == "." && "bazelisk" in paths[1]) {
      path = paths.drop(2).joinToString(File.pathSeparator)
    }
    applyVMOptionsPatch {
      withEnv("PATH", path)
      withEnv("HOME", System.getProperty("user.home"))
    }
    return this
  }

  private fun IDETestContext.patchSystemProperties(): IDETestContext {
    val projectViewPath = System.getProperty("bazel.project.view.file.path")
    applyVMOptionsPatch {
      projectViewPath?.let { addSystemProperty("bazel.project.view.file.path", it) }
    }
    return this
  }
}

private fun <T : CommandChain> T.waitForBazelSync(): T {
  addCommand(CMD_PREFIX + "waitForBazelSync")
  return this
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
