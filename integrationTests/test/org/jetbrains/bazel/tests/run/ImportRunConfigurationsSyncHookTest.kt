package org.jetbrains.bazel.tests.run

import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.Finder
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.withRetries
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/run/import:ImportRunConfigurationsSyncHookTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class ImportRunConfigurationsSyncHookTest : IdeStarterBaseProjectTest() {

  @Test
  fun openBazelProject() {
    createContext("importRunConfigurationsSyncHook", IdeaBazelCases.ImportRunConfigurationsSyncHook).runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
      ideFrame {
        syncBazelProject(buildAndSync = true)
        waitForIndicators(5.minutes)

        step("Select Bazel test configuration") {
          // Clicking the run config widget sometimes focuses it without opening the dropdown,
          // especially after focus was elsewhere (editor, build tool window). Retry the click
          // until the popup actually appears. ~9% flake rate observed without retries.
          selectRunConfiguration(currentText = "Remote JVM", targetText = "Bazel test CalculatorTest")
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
          wait(10.seconds)
          val expectedBuildErrors = setOf("Ended with an error.", "Calculator.java", " src/com/example 1 error", "BUILD", "  1 error")
          step("Verify build results tree") { verifyBuildResultsTree(expectedTexts = expectedBuildErrors) }

          step("Run test normally") { x { byAccessibleName("Run 'Bazel test CalculatorTest'") }.click() }
          wait(10.seconds)
          step("Verify build results tree") { verifyBuildResultsTree(expectedTexts = expectedBuildErrors) }

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
          selectRunConfiguration(currentText = "Bazel test CalculatorTest", targetText = "Bazel run :main")
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
          val homeValue = System.getenv("HOME")
          val userProfileValue = System.getenv("USERPROFILE")

          val homeFound = homeValue != null &&
            runCatching { consoleView.waitContainsText("PARENT_ENV_HOME=$homeValue", timeout = 5.seconds) }.isSuccess
          val userProfileFound = userProfileValue != null &&
            runCatching { consoleView.waitContainsText("PARENT_ENV_USERPROFILE=$userProfileValue", timeout = 5.seconds) }.isSuccess

          check(homeFound || userProfileFound) {
            "Parent environment variables not passed: neither HOME nor USERPROFILE found in output"
          }
        }
      }
    }
  }

  private fun Finder.selectRunConfiguration(currentText: String, targetText: String) {
    withRetries(message = "Click run config dropdown '$currentText' and select '$targetText'", times = 3) {
      x { byVisibleText(currentText) }.click()
      popup().waitOneText(targetText, timeout = 5.seconds).click()
    }
  }
}
