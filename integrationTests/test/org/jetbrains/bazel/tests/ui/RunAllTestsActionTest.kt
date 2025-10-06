package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.requireProject
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/ui/testResultsTree:runAllTestsActionTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class RunAllTestsActionTest : IdeStarterBaseProjectTest() {

  @Test
  fun openProject() {
    createContext("runAllTestsAction", IdeaBazelCases.RunAllTestsAction).runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
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
          listOf("2 tests passed", "2 tests total"),
          listOf("AdditionTest", "MultiplicationTest"),
        )
      }
    }
  }
}
