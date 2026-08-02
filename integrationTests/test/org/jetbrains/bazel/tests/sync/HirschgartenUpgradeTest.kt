package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.WaitForException
import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.welcomeScreen
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.checkIdeaLogForExceptions
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.ideStarter.waitForSyncSucceeded
import org.jetbrains.bazel.data.IdeStarterOs
import org.jetbrains.bazel.test.compat.IntegrationTestCompat
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class HirschgartenUpgradeTest : IdeStarterBaseProjectTest() {

  private var projectHome: Path? = null

  @BeforeEach
  fun skipIfCriticalFailed() = Assumptions.assumeFalse(criticalProblemOccurred)

  private fun resolveProjectHome(): Path {
    projectHome?.let { return it }
    val case = IdeaBazelCases.withProject(hirschgartenProjectInfo())
    val context = createContext("hirschgarten-resolve", case)
    return context.resolvedProjectHome.also { projectHome = it }
  }

  private val previousPluginZip: Path?
    get() = System.getenv("BAZEL_PREVIOUS_PLUGIN_ZIP")?.let { Path.of(it) }

  @Test
  @Order(1)
  fun `import hirschgarten and verify target order stability`() {
    try {
      importHirschgartenAndVerifyTargetOrder()
    } catch (e: Throwable) {
      criticalProblemOccurred = true
      throw e
    }
  }

  private fun importHirschgartenAndVerifyTargetOrder() {
    val case = IdeaBazelCases.withProject(hirschgartenProjectInfo())
    val context = createContext("hirschgarten", case)
      .applyVMOptionsPatch { withXmx(11264) }
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(10.minutes)
          waitForSyncSucceeded()

          step("Save target order after initial import") {
            execute { saveTargetOrder() }
            takeScreenshot("afterSaveTargetOrder")
          }
        }

        step("Close project") {
          invokeAction("CloseProject")
          takeScreenshot("afterCloseProject")
        }

        step("Reopen project from welcome screen") {
          welcomeScreen { clickRecentProject("hirschgarten") }
          takeScreenshot("afterReopen")
        }

        ideFrame {
          waitForIndicators(5.minutes)

          step("Assert target order unchanged after reopen") {
            execute { assertTargetOrder() }
            takeScreenshot("afterAssertTargetOrder")
          }
        }

        projectHome = context.resolvedProjectHome

        step("Check IDEA log for exceptions") {
          checkIdeaLogForExceptions(context)
        }
      }
  }

  @Test
  @Order(2)
  //comment the annotation out if you want to test locally
  @EnabledIfEnvironmentVariable(named = "BAZEL_PREVIOUS_PLUGIN_ZIP", matches = ".+")
  fun `plugin upgrade preserves project state`() {
    //replace the plugin zip definition for local tests
    //val pluginZip = Path.of("path/to/the/old/plugin.zip")
    val pluginZip = checkNotNull(previousPluginZip)

    val context = createContext(
      "hirschgarten-upgrade",
      IdeaBazelCases.withProject(
        LocalProjectInfo(
          projectDir = resolveProjectHome(),
          isReusable = true,
          configureProjectBeforeUse = {},
        ),
      ),
      pluginZipOverride = pluginZip,
    )

    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          waitForIndicators(5.minutes)
          takeScreenshot("oldPluginOpened")
        }

        step("Check IDEA log for exceptions (old plugin)") {
          checkIdeaLogForExceptions(context)
        }

        logAllBazelPluginVersions(context)
      }

    context.paths.pluginsDir.toFile().listFiles()
      ?.filter { it.name.contains("bazel", ignoreCase = true) }
      ?.forEach { it.deleteRecursively() }
    IntegrationTestCompat.onPostCreateContext(context)

    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        step("Verify no resync happens after plugin upgrade") {
          ideFrame {
            wait(20.seconds)
            try {
              val buildView = x { byType("com.intellij.build.BuildView") }
              withClue("Unexpected resync triggered after plugin upgrade") {
                buildView
                  .getAllTexts()
                  .any { it.text.contains(BazelPluginBundle.message("console.task.sync.in.progress")) }
                  .shouldBeFalse()
              }
            } catch (e: Exception) {
              withClue("Unknown exception: ${e.message}") {
                e.shouldBeInstanceOf<WaitForException>()
              }
            }
          }
          takeScreenshot("afterNoResyncCheck")
        }

        ideFrame {
          step("Assert target order matches after upgrade") {
            execute { assertTargetOrder() }
            takeScreenshot("afterUpgradeAssertTargetOrder")
          }
        }

        step("Check IDEA log for exceptions (upgraded plugin)") {
          checkIdeaLogForExceptions(context)
        }

        logAllBazelPluginVersions(context)
      }
  }
}

private fun hirschgartenProjectInfo(): ProjectInfoSpec {
  val localProjectPath = System.getProperty("bazel.ide.starter.test.project.path")
  if (localProjectPath != null) {
    return LocalProjectInfo(
      projectDir = Path.of(localProjectPath),
      isReusable = true,
      configureProjectBeforeUse = ::configureHirschgarten,
    )
  }
  val projectUrl = System.getProperty("bazel.ide.starter.test.project.url") ?: "https://github.com/JetBrains/hirschgarten.git"
  val commitHash = System.getProperty("bazel.ide.starter.test.commit.hash").orEmpty()
  val branchName = System.getProperty("bazel.ide.starter.test.branch.name") ?: "252"
  val projectHomeRelativePath: String? = System.getProperty("bazel.ide.starter.test.project.home.relative.path")

  return GitProjectInfo(
    repositoryUrl = projectUrl,
    commitHash = commitHash,
    branchName = branchName,
    projectHomeRelativePath = { if (projectHomeRelativePath != null) it.resolve(projectHomeRelativePath) else it },
    isReusable = true,
    configureProjectBeforeUse = ::configureHirschgarten,
  )
}

private fun configureHirschgarten(context: IDETestContext) {
  val projectHome = context.resolvedProjectHome
  resetTrackedFiles(projectHome)
  BazelProjectConfigurer.configureProjectBeforeUse(context)
  if (IdeStarterOs.current() == IdeStarterOs.WINDOWS) {
    configureHirschgartenForWindows(projectHome)
  } else {
    BazelProjectConfigurer.addHermeticCcToolchain(context)
  }
}

internal fun configureHirschgartenForWindows(projectHome: Path, gitBash: Path = findGitBashOnWindows()) {
  val moduleFile = projectHome / "MODULE.bazel"
  val windowsModule = moduleFile.readText()
  check(Regex("""module_name\s*=\s*"rules_apple"""").containsMatchIn(windowsModule)) {
    "Hirschgarten no longer pins rules_apple; verify its transitive version on Windows"
  }
  moduleFile.writeText(windowsModule.trimEnd() + WINDOWS_TOOLCHAIN_UTILS_OVERRIDE)
  (projectHome / WINDOWS_TOOLCHAIN_UTILS_PATCH_FILE).writeText(WINDOWS_TOOLCHAIN_UTILS_PATCH)

  val projectViewTemplate = (projectHome / "tools" / "intellij" / ".managed.bazelproject").readText()
  check(projectViewTemplate.lineSequence().count { it.trim() == HIRSCHGARTEN_FORMAT_TARGET } == 1) {
    "Hirschgarten's managed project view must contain exactly one $HIRSCHGARTEN_FORMAT_TARGET target"
  }
  check(projectViewTemplate.lineSequence().count { it.trim() == DERIVE_TARGETS_FROM_DIRECTORIES } == 1) {
    "Hirschgarten's managed project view must derive targets from its directories"
  }
  val windowsProjectView = projectViewTemplate
    .lineSequence()
    .filterNot { it.trim() == HIRSCHGARTEN_FORMAT_TARGET }
    .map { if (it.trim() == DERIVE_TARGETS_FROM_DIRECTORIES) "derive_targets_from_directories: false" else it }
    .joinToString("\n")
  (projectHome / "projectview.bazelproject").writeText(windowsProjectView.trimEnd() + "\n")

  val bazelRcFile = projectHome / ".bazelrc"
  bazelRcFile.writeText(
    bazelRcFile.readText().trimEnd() + WINDOWS_BAZEL_SETTINGS.replace("%git_bash%", gitBash.toString().replace('\\', '/')),
  )
  (projectHome / "MODULE.bazel.lock").deleteIfExists()
}

private fun findGitBashOnWindows(): Path {
  val pathCandidates = System.getenv("PATH").orEmpty()
    .split(File.pathSeparator)
    .filter { it.contains("Git", ignoreCase = true) }
    .flatMap { entry ->
      val directory = Path.of(entry)
      listOf(directory / "bash.exe", directory.parent?.resolve("bin")?.resolve("bash.exe"))
    }
    .filterNotNull()
  return (listOf(Path.of("C:/Program Files/Git/bin/bash.exe")) + pathCandidates)
    .firstOrNull { it.isRegularFile() }
    ?: error("Git Bash is required to import Hirschgarten on Windows")
}

internal const val HIRSCHGARTEN_FORMAT_TARGET = "//tools/format"

private const val DERIVE_TARGETS_FROM_DIRECTORIES = "derive_targets_from_directories: true"

internal const val WINDOWS_TOOLCHAIN_UTILS_PATCH_FILE = "toolchain_utils_windows.patch"

private const val WINDOWS_BAZEL_SETTINGS = """

common --legacy_external_runfiles
common --shell_executable='%git_bash%'
"""

private const val WINDOWS_TOOLCHAIN_UTILS_OVERRIDE = """

single_version_override(
    module_name = "rules_buf",
    version = "0.5.2",
)

single_version_override(
    module_name = "toolchain_utils",
    patch_strip = 1,
    patches = ["//:$WINDOWS_TOOLCHAIN_UTILS_PATCH_FILE"],
)
"""

internal const val WINDOWS_TOOLCHAIN_UTILS_PATCH = """diff --git a/toolchain/local/triplet/libc.bzl b/toolchain/local/triplet/libc.bzl
--- a/toolchain/local/triplet/libc.bzl
+++ b/toolchain/local/triplet/libc.bzl
@@ -56 +56 @@ def _powershell(rctx, path):
-    result = rctx.execute([path, "-Command", "Get-Package -Name 'Universal CRT Redistributable'| Format-Wide -Property Version"])
+    result = rctx.execute([path, "-Command", "${'$'}WarningPreference = 'SilentlyContinue'; Get-Package -Name 'Universal CRT Redistributable' | Format-Wide -Property Version"])
"""

private fun resetTrackedFiles(projectHome: Path) {
  val result = ProcessBuilder("git", "checkout", "--", ".bazelrc", "MODULE.bazel")
    .directory(projectHome.toFile())
    .redirectErrorStream(true)
    .start()
    .waitFor()
  if (result != 0) {
    System.err.println("git checkout failed (exit $result) — .bazelrc/MODULE.bazel may have accumulated changes")
  }
}

private val BAZEL_PLUGIN_VERSION_REGEX = Regex("""Bazel \(([^)]+)\)""")

private fun logAllBazelPluginVersions(context: IDETestContext) {
  val logFile = context.paths.testHome.resolve("log").resolve("idea.log")
  if (!logFile.toFile().exists()) {
    println("[Plugin versions] idea.log not found")
    return
  }
  val versions = logFile.toFile().useLines { lines ->
    lines.filter { it.contains("Loaded custom plugins:") }
      .mapNotNull { line -> BAZEL_PLUGIN_VERSION_REGEX.find(line)?.groupValues?.get(1) }
      .toList()
  }
  println("=== Bazel plugin versions loaded across IDE runs (${versions.size} found) ===")
  versions.forEachIndexed { i, v -> println("  IDE run ${i + 1}: Bazel ($v)") }
}

private fun <T : CommandChain> T.saveTargetOrder(): T {
  addCommand(CMD_PREFIX + "saveTargetOrder")
  return this
}

private fun <T : CommandChain> T.assertTargetOrder(): T {
  addCommand(CMD_PREFIX + "assertTargetOrder")
  return this
}
