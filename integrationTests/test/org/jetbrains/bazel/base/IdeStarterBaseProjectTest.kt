package org.jetbrains.bazel.ideStarter

import com.intellij.driver.client.Driver
import com.intellij.driver.client.Remote
import com.intellij.driver.client.utility
import com.intellij.driver.sdk.Project
import com.intellij.driver.sdk.VirtualFile
import com.intellij.driver.sdk.openEditor
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.dialog
import com.intellij.driver.sdk.waitForCodeAnalysis
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.teamcity.TeamCityCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.execute
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.runner.Starter
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.assertCaretPosition
import com.intellij.tools.ide.performanceTesting.commands.assertCurrentFile
import com.intellij.tools.ide.performanceTesting.commands.delay
import com.intellij.tools.ide.performanceTesting.commands.goToDeclaration
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.test.compat.IntegrationTestCompat
import org.jetbrains.bazel.testing.IS_IN_IDE_STARTER_TEST
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import com.intellij.ide.starter.process.ProcessKiller.killProcesses
import com.intellij.ide.starter.process.getProcessList
import org.jetbrains.bazel.tests.ui.expandedTree
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPathApi::class)
abstract class IdeStarterBaseProjectTest {
  protected open val timeout: Duration
    get() = (System.getProperty("bazel.ide.starter.test.timeout.seconds")?.toIntOrNull() ?: 1200).seconds

  protected fun createContext(projectName: String, case: TestCase<*>): IDETestContext {
    IntegrationTestCompat.onPreCreateContext()
    val ctx = Starter.newContext(projectName, case)
    IntegrationTestCompat.onPostCreateContext(ctx)

    return ctx
      .executeRightAfterIdeOpened(true)
      .propagateSystemProperty("idea.diagnostic.opentelemetry.otlp")
      .propagateSystemProperty("bazel.project.view.file.path")
      .patchPathVariable()
      .withKotlinPluginK2()
      .addIdeStarterTestMarker()
      .applyVMOptionsPatch {
        addSystemProperty("JETBRAINS_LICENSE_SERVER", "https://flsv1.labs.jb.gg")
        addSystemProperty("idea.trust.disabled", "true")
      }
  }

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

    expandedTree = false
  }

  @AfterEach
  fun tearDown() {
    try {
      // Kill Bazel server Java processes started for the test workspace
      println("Killing Bazel server processes...")
      val processesToKill = getProcessList { p ->
        val hasServerJar = p.arguments.any { arg ->
          arg.contains("A-server.jar") || arg.endsWith("/server.jar") || arg.endsWith("\\server.jar") || arg.endsWith("-server.jar")
        }
        val fromIdeTestsWorkspace = p.arguments.any { arg ->
          arg.startsWith("--workspace_directory=") && (arg.contains("/ide-tests/") || arg.contains("\\ide-tests\\"))
        }
        hasServerJar && fromIdeTestsWorkspace
      }
      if (processesToKill.isNotEmpty()) {
        println("Killing Bazel server processes: [${processesToKill.joinToString(", ")}]")
        killProcesses(processesToKill)
      } else {
        println("No Bazel server processes found to kill")
      }
    } catch (t: Throwable) {
      System.err.println("Failed to find/kill Bazel server processes: ${t.message}")
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

  private fun IDETestContext.addIdeStarterTestMarker(): IDETestContext {
    applyVMOptionsPatch {
      addSystemProperty(IS_IN_IDE_STARTER_TEST, "true")
    }
    return this
  }
}

/**
 * Builds a CommandChain with [builder] and executes it immediately.
 * Replaces clunky construct like `execute(CommandChain().goto(3, 7))`
 * ```
 */
inline fun Driver.execute(builder: CommandChain.() -> Unit) {
  this.execute(CommandChain().apply(builder))
}

fun Driver.syncBazelProject(buildAndSync: Boolean = false) {
  execute(CommandChain().takeScreenshot("startSync"))
  execute(CommandChain().openBspToolWindow())
  execute(CommandChain().takeScreenshot("openBspToolWindow"))
  execute(CommandChain().waitForBazelSync())
  if (buildAndSync) {
    execute(CommandChain().buildAndSync())
  }
  execute(CommandChain().waitForSmartMode())
}

fun Driver.syncBazelProjectCloseDialog() {
  execute(CommandChain().takeScreenshot("startSync"))
  execute(CommandChain().openBspToolWindow())
  // close only the Git confirmation dialog; other popups like "Loading file" are not closable via dispose()
  ideFrame {
    val gitAddDialog = dialog(title = "Add File to Git")
    val dialogFound = runCatching { gitAddDialog.waitFound(timeout = 30.seconds) }.isSuccess

    if (dialogFound) {
      runCatching { gitAddDialog.closeDialog() }
        .onFailure { System.err.println("Failed to close 'Add File to Git' dialog: ${it.message}") }
    }
  }
  execute(CommandChain().takeScreenshot("openBspToolWindow"))
  execute(CommandChain().waitForBazelSync())
  execute(CommandChain().waitForSmartMode())
}

fun <T : CommandChain> T.openBspToolWindow(): T {
  addCommand(CMD_PREFIX + "openBspToolWindow")
  return this
}

fun <T : CommandChain> T.waitForBazelSync(): T {
  addCommand(CMD_PREFIX + "waitForBazelSync")
  return this
}

fun <T : CommandChain> T.buildAndSync(): T {
  addCommand(CMD_PREFIX + "buildAndSync")
  return this
}

fun <T : CommandChain> T.assertFileContentsEqual(expectedRelativePath: String, actualRelativePath: String): T {
  addCommand(CMD_PREFIX + "assertFileContentsEqual $expectedRelativePath $actualRelativePath")
  return this
}

fun <T : CommandChain> T.assertEitherFileContentIsEqual(actualRelativePath: String, vararg expectedRelativePaths: String): T {
  addCommand(CMD_PREFIX + "assertEitherContentsEqual $actualRelativePath ${expectedRelativePaths.joinToString(" ")}")
  return this
}

fun <T : CommandChain> T.navigateToFile(
  caretLine: Int,
  caretColumn: Int,
  expectedFilename: String,
  expectedCaretLine: Int,
  expectedCaretColumn: Int,
): T =
  this
    .goto(caretLine, caretColumn)
    .delay(500)
    .takeScreenshot("Before navigating to $expectedFilename")
    .goToDeclaration()
    .delay(500)
    .assertCurrentFile(expectedFilename)
    .assertCaretPosition(expectedCaretLine, expectedCaretColumn)

fun IDETestContext.withBazelFeatureFlag(flag: String) = this.applyVMOptionsPatch {
  addSystemProperty(flag, "true")
}

/**
 * Should be used instead of [com.intellij.driver.sdk.openFile] because this method doesn't rely on content roots
 */
fun Driver.openFile(relativePath: String, waitForCodeAnalysis: Boolean = true): VirtualFile =
  step("Open file $relativePath") {
    val fileToOpen = findFile(relativePath = relativePath) ?: throw IllegalArgumentException("Fail to find file $relativePath")
    openEditor(fileToOpen, singleProject())
    if (waitForCodeAnalysis) {
      waitForCodeAnalysis(singleProject(), fileToOpen)
    }
    fileToOpen
  }

/**
 * Should be used instead of [com.intellij.driver.sdk.findFile] because this method doesn't rely on content roots
 */
fun Driver.findFile(relativePath: String): VirtualFile? = projectRootDir.findFileByRelativePath(relativePath)

val Driver.projectRootDir: VirtualFile
  get() = utility<BazelProjectPropertiesKt>().getRootDir(singleProject())

@Remote("org.jetbrains.bazel.config.BazelProjectPropertiesKt", plugin = "org.jetbrains.bazel")
interface BazelProjectPropertiesKt {
  fun getRootDir(project: Project): VirtualFile
}
