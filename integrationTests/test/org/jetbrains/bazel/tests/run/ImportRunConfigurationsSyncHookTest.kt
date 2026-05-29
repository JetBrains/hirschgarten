package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.getNotifications
import com.intellij.driver.sdk.setRegistry
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.GutterUiComponent
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.dialogs.editRunConfigurationsDialog
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.popups.runConfigurationsList
import com.intellij.driver.sdk.ui.components.common.popups.runConfigurationsPopup
import com.intellij.driver.sdk.ui.components.elements.actionButton
import com.intellij.driver.sdk.ui.components.elements.list
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.pasteText
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.waitFor
import com.intellij.driver.sdk.withRetries
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.assertCurrentFile
import com.intellij.tools.ide.performanceTesting.commands.delay
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.jetbrains.bazel.tests.combined.IdeStarterCombinedBaseTest
import org.jetbrains.bazel.tests.ui.clickRunGutterOnLine
import org.jetbrains.bazel.tests.ui.verifyAvailableRunGutterActions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import java.awt.Point
import java.awt.event.KeyEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ImportRunConfigurationsSyncHookTest : IdeStarterCombinedBaseTest() {
  override fun createContext(): IDETestContext =
    createContext("importRunConfigurationsSyncHook", IdeaBazelCases.ImportRunConfigurationsSyncHook)

  @Test
  @Order(1)
  fun `imported run configurations should execute and show build diagnostics`() {
    withDriver(bgRun) {
      ideFrame {
        syncBazelProject(buildAndSync = true)
        waitForIndicators(5.minutes)

        step("Select Bazel test configuration") {
          // Clicking the run config widget sometimes focuses it without opening the dropdown,
          // especially after focus was elsewhere (editor, build tool window). Retry the click
          // until the popup actually appears. ~9% flake rate observed without retries.
          selectRunConfiguration(targetText = "Bazel test CalculatorTest")
        }

        step("Check build diagnostics with run configs") {
          step("Break compilation intentionally") {
            openFile("src/com/example/Calculator.java")
            codeEditor {
              // Erase the semicolon ;
              goToPosition(5, 22)
              keyboard { key(KeyEvent.VK_BACK_SPACE) }
            }
          }
          step("Run test in Debug") { x { byAccessibleName("Debug 'Bazel test CalculatorTest'") }.click() }
          val expectedBuildErrors = setOf("Ended with an error.", "Calculator.java", " src/com/example 1 error", "BUILD", "  1 error")
          step("Verify build results tree") { waitForBuildResultsTree(expectedTexts = expectedBuildErrors) }

          step("Run test normally") { x { byAccessibleName("Run 'Bazel test CalculatorTest'") }.click() }
          step("Verify build results tree") { waitForBuildResultsTree(expectedTexts = expectedBuildErrors) }

          step("Fix compilation") {
            openFile("src/com/example/Calculator.java")
            codeEditor {
              // Return back the semicolon ;
              goToPosition(5, 22)
              keyboard { key(KeyEvent.VK_SEMICOLON) }
            }
          }
        }

        step("Select Bazel run configuration") {
          selectRunConfiguration("Bazel run :main")
        }

        step("Execute the run configuration") { x { byAccessibleName("Run 'Bazel run :main'") }.click() }
        val consoleView = x { byClass("ConsoleViewImpl") }
        step("Wait for run config to finish") {
          consoleView.shouldBe { present() }
          consoleView.waitContainsText("2 + 2 = 4", timeout = 3.minutes)
        }

        step("Check that \$PROJECT_DIR$ macro is expanded correctly") {
          val moduleBazelPath = checkNotNull(singleProject().getBasePath()) + "/MODULE.bazel"
          consoleView.waitContainsText("MODULE.bazel from envs: $moduleBazelPath", timeout = 5.seconds)
          consoleView.waitContainsText("Args: [-moduleBazelLocation=$moduleBazelPath", timeout = 5.seconds)
        }

        step("Check that parent environment variables are passed to run targets (BAZEL-2761)") {
          // IDE receives HOME from System.getProperty("user.home") via IdeStarterBaseProjectTest.patchPathVariable()
          val homeValue = System.getProperty("user.home")
          consoleView.waitContainsText("PARENT_ENV_HOME=$homeValue", timeout = 5.seconds)
        }
      }
    }
  }

  @Test
  @Order(2)
  fun `creating new run configuration shows target-specific UI`() {
    withDriver(bgRun) {
      ideFrame {
        runConfigurationsPopup {
          list().clickItem("Edit Configurations", fullMatch = false)
        }

        editRunConfigurationsDialog {
          waitFound()
          addNewRunConfiguration("Bazel", "New run config")

          Thread.sleep(3000)
          val jvmSpecificRunConfigText = "Debug port"
          waitNoTexts(jvmSpecificRunConfigText)

          val targetsField = x { byClass("EditorComponentImpl") }.waitFound()
          targetsField.click()
          keyboard { typeText("//:main", 0) }

          waitContainsText(jvmSpecificRunConfigText)
        }
      }
    }
  }

  @Test
  @Order(Int.MAX_VALUE)
  fun `check that running with profiler works`() {
    withDriver(bgRun) {
      ideFrame {
        step("Select Bazel run configuration") {
          selectRunConfiguration("Bazel run :main")
        }

        fun waitForProfilerDataReadyBubbleAppearAndClose() =
          step("Wait for 'Profiler data is ready' bubble and then wait for it to disappear") {
            val text = "Profiler data is ready"
            waitOneContainsText(text, timeout = 1.minutes)
            getNotifications().forEach {
              it.hideBalloon()
            }
            // Wait here for the popup to close in case if we run the profiler again and rely on the bubble for completion
            waitFor(
              message = "Should not have $text",
              timeout = 1.minutes,
              getter = { getAllTexts() },
              checker = { it.none { it.text.contains(text) } },
            )
          }

        step("Check that running with IntelliJ Profiler works") {
          setRegistry("linux.kernel.variables.validate.every.time", false)
          openFile("src/com/example/Main.java")
          codeEditor {
            goToPosition(4, 1)
            pasteText("import java.util.Random;\n")
            goToPosition(12, 86)
            pasteText(
              "\n" + """
              long start = System.currentTimeMillis();
              double result = 0.0;
              Random random = new Random();
              while (System.currentTimeMillis() < start + 10000) {
                  for (int i = 0; i < 1000; i++) {
                      result += random.nextDouble();
                  }
              }
              System.out.println("The result is: " + result);
              """.trimIndent() + "\n",
            )
          }

          val moreActionsButton = actionButton { byAccessibleName("More Actions") }
          moreActionsButton.click()
          popup().waitOneText("Profile 'Bazel run :main' with 'IntelliJ Profiler'").click()
          waitContainsText("Stop Recording and Show Results")  // check that the "Performance" UI tab appears
          waitContainsText("CPU")  // check that the "Performance" UI tab appears
          val consoleView = x { byClass("ConsoleViewImpl") }
          consoleView.waitContainsText("The result is", timeout = 3.minutes)
          waitForProfilerDataReadyBubbleAppearAndClose()

          takeScreenshot("beforeClickingOnGutter")
          editorTabs().gutter().clickLine(18)  // result += random.nextDouble();
          execute {
            delay(500)
            assertCurrentFile("Random.java")  // Clicking on the line tooltip will navigate to the profiler hot path
          }

          step("Check that run gutters exist") {
            openFile("src/com/example/Main.java")
            clickRunGutterOnLine(6)
            verifyAvailableRunGutterActions(listOf("Run '//:main'", "Debug '//:main'", "Profile '//:main' with 'IntelliJ Profiler'"))
          }
        }
        step("Check that running test by gutter with IntelliJ Profiler works") {
          openFile("src/com/example/CalculatorTest.java")
          codeEditor {
            goToPosition(4, 1)
            pasteText("import java.util.Random;")
            goToPosition(9, 28)
            pasteText(
              "\n" + """
              long start = System.currentTimeMillis();
              double result = 0.0;
              Random random = new Random();
              while (System.currentTimeMillis() < start + 5000) {
                  for (int i = 0; i < 1000; i++) {
                      result += random.nextDouble();
                  }
              }
              assertEquals(0, result == 0.0 ? 1 : 0);
              """.trimIndent() + "\n",
            )
          }
          clickRunGutterOnLine(8)
          popup().waitOneText("Profile '//:calculator_test' with 'IntelliJ Profiler'").click()
          waitForProfilerDataReadyBubbleAppearAndClose()
          takeScreenshot("beforeClickingOnGutter")
          editorTabs().gutter().clickLine(15)  // result += random.nextDouble();
          execute {
            delay(500)
            assertCurrentFile("Random.java")  // Clicking on the line tooltip will navigate to the profiler hot path
          }
        }
      }
    }
  }

  private fun GutterUiComponent.clickLine(line: Int) {
    click(pointAtLine(line))
  }

  private fun GutterUiComponent.pointAtLine(line: Int): Point =
    Point(iconAreaOffset - 20, waitOneText(line.toString()).point.y)

  private fun IdeaFrameUI.selectRunConfiguration(targetText: String) {
    withRetries(message = "Select run configuration '$targetText'", times = 3) {
      runConfigurationsPopup {
        runConfigurationsList {
          clickItem(targetText, fullMatch = false)
        }
      }
    }
  }
}
