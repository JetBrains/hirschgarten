package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree:runAllTestsActionTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class RunAllTestsActionTest : IdeStarterBaseProjectTest() {

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `run all tests action should execute and show results`(runConfigRunWithBazel: Boolean) {
    createContext("runAllTestsAction", IdeaBazelCases.RunAllTestsAction)
      .setRunConfigRunWithBazel(runConfigRunWithBazel)
      .runIdeWithDriver(runTimeout = timeout) { withScreenRecording() }.useDriverAndCloseIde {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)

        step("Right-click the root project directory") {
          projectView().projectViewTree.rightClickRow(0)
          popupMenu().waitFound()
          takeScreenshot("afterRightClickingProjectRoot")
        }

        step("Click on Run all tests") {
          popupMenu().findMenuItemByText("Run all tests").click()
          waitForIndicators(5.minutes)
        }

        verifyTestStatus(
          listOf("2 tests passed"),
          setOf("AdditionTest", "testAddition", "MultiplicationTest", "testMultiplication"),
        )
      }
    }
  }
}
