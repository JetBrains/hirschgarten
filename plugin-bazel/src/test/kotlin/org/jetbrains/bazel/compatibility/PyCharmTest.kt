package org.jetbrains.bazel.compatibility

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.openFile
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.exitApp
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.navigateToFile
import org.jetbrains.bazel.ideStarter.waitForBazelSync
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/compatibility:PyCharmTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class PyCharmTest : IdeStarterBaseProjectTest() {
  override val projectInfo: ProjectInfoSpec
    get() =
      GitProjectInfo(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "73ad49439188c91342d46f63a32230e5b535dc03",
        branchName = "main",
        projectHomeRelativePath = { it.resolve("simpleMultiLanguageTest") },
        isReusable = false,
        configureProjectBeforeUse = ::configureProjectBeforeUse,
      )

  @Test
  fun openBazelProject() {
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .waitForSmartMode()
        .checkImportedModules()
        .openFile("python/bin.py")

    createContext()
      .runIdeWithDriver(commands = commands, runTimeout = timeout)
      .useDriverAndCloseIde {
        verifyRunLineMarkerText(listOf("Run", "Debug run"))
      }
  }

  @Test
  fun openBazelProjectWithTestFile() {
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .waitForSmartMode()
        .checkImportedModules()
        .openFile("python/test.py")

    createContext()
      .runIdeWithDriver(commands = commands, runTimeout = timeout)
      .useDriverAndCloseIde {
        verifyRunLineMarkerText(listOf("Test", "Debug test", "Run with Coverage"))
      }
  }

  @Test
  fun checkImportStatements() {
    val commands =
      CommandChain()
        .takeScreenshot("startSync")
        .waitForBazelSync()
        .waitForSmartMode()
        .checkImportedModules()
        .openFile("python/bin.py")
        // click on `print("bin!")`
        .navigateToFile(1, 2, "builtins.pyi", 1645, 5)
        .openFile("python/main/main.py")
        // click on `bbb()`
        .navigateToFile(11, 5, "util.py", 3, 5)
        .openFile("python/main/main.py")
        // click on `print(requests.__version__)`
        .navigateToFile(9, 26, "__version__.py", 8, 1)
        .openFile("python/main/main.py")
        // click on `print(np.version.version)`
        .navigateToFile(10, 26, "version.py", 5, 1)
        .openFile("python/libs/my_lib2/util.py")
        // click on `aaa()`
        .navigateToFile(4, 7, "util.py", 1, 5)
        .openFile("python/main/main.py")
        // click on `from urban.cities import print_cities`
        .navigateToFile(6, 34, "cities.py", 4, 5)
        .openFile("python/main/main.py")
        // click on `from artificial.plastics import print_plastics`
        .navigateToFile(7, 37, "plastics.py", 4, 5)
        .exitApp()

    createContext().runIDE(commands = commands, runTimeout = timeout)
  }

  private fun Driver.verifyRunLineMarkerText(expectedTexts: List<String>) {
    ideFrame {
      waitForIndicators(5.minutes)
      val gutterIcons = editorTabs().gutter().getGutterIcons()
      val selectedGutterIcon = gutterIcons.first()
      selectedGutterIcon.click()
      val heavyWeightWindow = popup(xQuery { byClass("HeavyWeightWindow") })
      takeScreenshot("afterClickingOnRunLineMarker")
      val texts = heavyWeightWindow.getAllTexts()
      assert(texts.size == expectedTexts.size)
      expectedTexts.forEach { expected -> assert(texts.any { actual -> actual.text.contains(expected) }) }
    }
  }

  private fun <T : CommandChain> T.checkImportedModules(): T = also { addCommand("${CMD_PREFIX}PyCharmCheckImportedModules") }
}
