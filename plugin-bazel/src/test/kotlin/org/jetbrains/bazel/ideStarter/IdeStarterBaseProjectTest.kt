package org.jetbrains.bazel.ideStarter

import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.teamcity.TeamCityCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.project.ProjectInfoSpec
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

private const val PLATFORM_BUILD_NUMBER_PROPERTY = "bazel.ide.starter.test.platform.build.number"

@OptIn(ExperimentalPathApi::class)
abstract class IdeStarterBaseProjectTest {
  abstract val projectInfo: ProjectInfoSpec

  val projectName: String
    get() = System.getProperty("bazel.ide.starter.test.project.name") ?: "hirschgarten"

  val testCase: TestCase<ProjectInfoSpec>
    get() = TestCase(IdeProductProvider.IC, projectInfo).withBuildNumber(System.getProperty(PLATFORM_BUILD_NUMBER_PROPERTY))

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

  fun IDETestContext.withBazelPluginInstalled(): IDETestContext {
    installPlugin(this, System.getProperty("bazel.ide.starter.test.bazel.plugin.zip"))
    return this
  }

  fun IDETestContext.withBspPluginInstalled(): IDETestContext {
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
    val targets = System.getProperty("bsp.hotswap.target.list")
    val buildFlags = System.getProperty("bsp.hotswap.build.flags")
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

  fun IDETestContext.propagateSystemProperty(key: String): IDETestContext {
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
  fun IDETestContext.patchPathVariable(): IDETestContext {
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
}
