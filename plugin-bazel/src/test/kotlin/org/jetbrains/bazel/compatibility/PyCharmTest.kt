package org.jetbrains.bazel.compatibility

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.step
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
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
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
        isReusable = true,
        configureProjectBeforeUse = ::configureProjectBeforeUseWithoutBazelClean,
      )
  // get() =
  //  LocalProjectInfo(
  //    projectDir = Path.of("path/to/simpleBazelProjectsForTesting/simpleMultiLanguageTest"),
  //    isReusable = true,
  //    configureProjectBeforeUse = ::configureProjectBeforeUseWithoutBazelClean,
  //  )

  private val commands =
    CommandChain()
      .takeScreenshot("startSync")
      .waitForBazelSync()
      .waitForSmartMode()
      .checkImportedModules()

  @Test
  fun openBazelProject() {
    createContext()
      .runIdeWithDriver(commands = commands, runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          waitForIndicators(10.minutes)

          step("Open file") {
            execute { openFile("python/bin.py") }
          }

          step("Verify run line marker text") {
            verifyRunLineMarkerText(listOf("Run", "Debug run"))
          }
        }
      }
  }

  @Test
  fun openBazelProjectWithTestFile() {
    createContext()
      .runIdeWithDriver(commands = commands, runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          waitForIndicators(10.minutes)

          step("Open test file") {
            execute { openFile("python/test.py") }
          }

          step("Verify run line marker text") {
            verifyRunLineMarkerText(listOf("Test", "Debug test", "Run with Coverage"))
          }
        }
      }
  }

  @Test
  fun checkImportStatements() {
    createContext()
      .runIdeWithDriver(commands = commands, runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          waitForIndicators(10.minutes)

          step("Open main.py and navigate to bbb") {
            execute { openFile("python/main/main.py") }
            execute { navigateToFile(2, 28, "util.py", 3, 5) }
            takeScreenshot("afterNavigatingToBbb")
          }

          step("Navigate to requests.__version__") {
            execute { openFile("python/main/main.py") }
            execute { navigateToFile(9, 26, "__version__.py", 8, 1) }
            takeScreenshot("afterNavigatingToRequestsVersion")
          }

          step("Navigate to np.version.version") {
            execute { openFile("python/main/main.py") }
            execute { navigateToFile(10, 26, "version.py", 5, 1) }
            takeScreenshot("afterNavigatingToNumpyVersion")
          }

          step("Navigate to aaa from my_lib2") {
            execute { openFile("python/libs/my_lib2/util.py") }
            execute { navigateToFile(1, 27, "util.py", 1, 5) }
            takeScreenshot("afterNavigatingToAaaFromMyLib2")
          }

          step("Navigate to urban.cities") {
            execute { openFile("python/main/main.py") }
            execute { navigateToFile(6, 34, "cities.py", 4, 5) }
            takeScreenshot("afterNavigatingToUrbanCities")
          }

          step("Navigate to artificial.plastics") {
            execute { openFile("python/main/main.py") }
            execute { navigateToFile(7, 37, "plastics.py", 4, 5) }
            takeScreenshot("afterNavigatingToArtificialPlastics")
          }
        }
      }
  }

  private fun Driver.verifyRunLineMarkerText(expectedTexts: List<String>) {
    ideFrame {
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
