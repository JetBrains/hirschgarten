package org.jetbrains.bazel.ideStarter

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
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import org.junit.jupiter.api.BeforeEach
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPathApi::class)
abstract class IdeStarterBaseProjectTest {
  protected abstract val projectInfo: ProjectInfoSpec

  protected open val projectName: String
    get() = System.getProperty("bazel.ide.starter.test.project.name") ?: javaClass.simpleName

  private val testCase: TestCase<ProjectInfoSpec>
    get() = TestCase(IdeProductProvider.IC, projectInfo).withBuildNumber(System.getProperty("bazel.ide.starter.test.platform.build.number"))

  protected open val timeout: Duration
    get() = (System.getProperty("bazel.ide.starter.test.timeout.seconds")?.toIntOrNull() ?: 600).seconds

  protected fun createContext(): IDETestContext =
    Starter
      .newContext(projectName, testCase)
      .executeRightAfterIdeOpened(true)
      .propagateSystemProperty("idea.diagnostic.opentelemetry.otlp")
      .propagateSystemProperty("bazel.project.view.file.path")
      .patchPathVariable()
      .withKotlinPluginK2()
      .withBspPluginInstalled()
      .withBazelPluginInstalled()
  // uncomment for debugging
  //  .applyVMOptionsPatch { debug(8000, suspend = true) }

  @BeforeEach
  fun initialize() {
    di =
      DI {
        extend(di)
        bindSingleton(tag = "teamcity.uri", overrides = true) {
          val teamcityUrl = System.getProperty("bazel.ide.starter.test.teamcity.url") ?: "https://buildserver.labs.intellij.net"
          URI(teamcityUrl).normalize()
        }
        bindSingleton<CIServer>(overrides = true) { TeamCityCIServer() }
        System.getProperty("bazel.ide.starter.test.cache.directory")?.let { root ->
          bindSingleton<GlobalPaths>(overrides = true) {
            object : GlobalPaths(Path.of(root)) {}
          }
        }
      }
  }

  private fun IDETestContext.withBazelPluginInstalled(): IDETestContext {
    installPlugin(this, System.getProperty("bazel.ide.starter.test.bazel.plugin.zip"))
    return this
  }

  private fun IDETestContext.withBspPluginInstalled(): IDETestContext {
    installPlugin(this, System.getProperty("bazel.ide.starter.test.bsp.plugin.zip"))
    return this
  }

  fun configureProjectBeforeUse(context: IDETestContext) {
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
    val targets = System.getProperty("bazel.ide.starter.test.target.list")
    val buildFlags = System.getProperty("bazel.ide.starter.test.build.flags")
    if (projectView.exists() && targets == null && buildFlags == null) return
    projectView.writeText(createTargetsSection(targets) + "\n" + createBuildFlagsSection(buildFlags))
  }

  private fun createTargetsSection(targets: String?) =
    """
    targets:
      ${targets ?: "//..."}
    """.trimIndent()

  private fun createBuildFlagsSection(buildFlags: String?): String {
    if (buildFlags == null) return ""
    return """
      build_flags:
        $buildFlags
      """.trimIndent()
  }

  private fun extractFileFromJar(
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

  protected fun getProjectInfoFromSystemProperties(): ProjectInfoSpec {
    val localProjectPath = System.getProperty("bazel.ide.starter.test.project.path")
    if (localProjectPath != null) {
      return LocalProjectInfo(
        projectDir = Path.of(localProjectPath),
        isReusable = true,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )
    }
    val projectUrl = System.getProperty("bazel.ide.starter.test.project.url") ?: "https://github.com/JetBrains/hirschgarten.git"
    val commitHash = System.getProperty("bazel.ide.starter.test.commit.hash").orEmpty()
    val branchName = System.getProperty("bazel.ide.starter.test.branch.name") ?: "main"
    val projectHomeRelativePath: String? = System.getProperty("bazel.ide.starter.test.project.home.relative.path")

    return GitProjectInfo(
      repositoryUrl = projectUrl,
      commitHash = commitHash,
      branchName = branchName,
      projectHomeRelativePath = { if (projectHomeRelativePath != null) it.resolve(projectHomeRelativePath) else it },
      isReusable = true,
      configureProjectBeforeUse = ::configureProjectBeforeUse,
    )
  }
}

fun <T : CommandChain> T.waitForBazelSync(): T {
  addCommand(CMD_PREFIX + "waitForBazelSync")
  return this
}

fun <T : CommandChain> T.buildAndSync(): T {
  addCommand(CMD_PREFIX + "buildAndSync")
  return this
}
