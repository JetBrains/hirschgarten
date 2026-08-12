package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolWindow
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.simpleBazelProject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes

private val RUN_ALL_TESTS_PROJECT = simpleBazelProject(
  path = "runAllTests",
)

/**
 * ```sh
 * bazel test //plugins/bazel/integrationTests:integrationTests_test --test_env=JB_TEST_FILTER=org.jetbrains.bazel.tests.ui.RunAllTestsActionTest --test_output=errors --nocache_test_results
 * ```
 */
class RunAllTestsActionTest : IdeStarterBaseProjectTest() {

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `run all tests action should execute and show results`(runConfigRunWithBazel: Boolean) {
    createContext("runAllTestsAction-${if (runConfigRunWithBazel) "withBazel" else "withoutBazel"}", IdeaBazelCases.withProject(RUN_ALL_TESTS_PROJECT))
      .setRunConfigRunWithBazel(runConfigRunWithBazel)
      .runIdeWithDriver(runTimeout = timeout).useDriverAndCloseIde {
      ideFrame {
        syncBazelProject()
        waitForIndicators(5.minutes)

        fun verifyTestsInRoot() {
          popupMenu().waitFound()
          takeScreenshot("afterRightClicking")
          popupMenu().findMenuItemByText("Run 'Tests in 'runAllTests''").click()
          waitForIndicators(5.minutes)
          verifyTestStatus(
            listOf("3 tests passed"),
            setOf("AdditionTest", "testAddition", "MultiplicationTest", "testMultiplication", "DivisionTest", "testDivision"),
          )
        }

        fun verifyTestsInMy() {
          takeScreenshot("afterRightClicking")
          popupMenu().findMenuItemByText("Run 'Tests in 'my''").click()
          waitForIndicators(5.minutes)
          verifyTestStatus(
            listOf("3 tests passed"),
            setOf("AdditionTest", "testAddition", "MultiplicationTest", "testMultiplication", "DivisionTest", "testDivision"),
          )
        }

        fun verifyTestsInMyPackage() {
          takeScreenshot("afterRightClicking")
          popupMenu().findMenuItemByText("Run 'Tests in 'package''").click()
          waitForIndicators(5.minutes)
          verifyTestStatus(
            listOf("2 tests passed"),
            setOf("AdditionTest", "testAddition", "MultiplicationTest", "testMultiplication"),
          )
        }

        step("Run all tests in root directory") {
          projectView().projectViewTree.rightClickRow { it.contains("runAllTests") }
          verifyTestsInRoot()
        }

        step("Run all tests in a root/my/package directory") {
          projectView().projectViewTree.run {
            expandPath("runAllTests", "my", fullMatch = false)
            rightClickRow { it.contains("package") }
          }
          verifyTestsInMyPackage()
        }

        step("Run all tests in root/my toolwindow path") {
          toolWindow("Bazel") { tree().rightClickRow { it.contains("my") } }
          verifyTestsInMy()
        }

        step("Run all tests in a root/my/package toolwindow path") {
          toolWindow("Bazel") {
            tree().run {
              expandAll()
              rightClickRow { it.contains("package") }
            }
          }
          verifyTestsInMyPackage()
        }
      }
    }
  }
}
