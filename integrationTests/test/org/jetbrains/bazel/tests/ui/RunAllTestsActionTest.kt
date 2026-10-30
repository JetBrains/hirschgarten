package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.editor
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolWindow
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.ui.components.elements.tree
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.simpleBazelProject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
        waitForSyncSucceeded()

        // The lookup must use the scope of the IDE frame, because the popup is not a child of the tree.
        val menu = { popupMenu() }

        fun verifyTestsInRoot() {
          takeScreenshot("afterRightClicking")
          popupMenu().findMenuItemByText("Run 'Tests in 'runAllTests''").click()
          verifyTestStatus(
            listOf("3 tests passed"),
            setOf("AdditionTest", "testAddition", "MultiplicationTest", "testMultiplication", "DivisionTest", "testDivision"),
          )
          verifyTestOutput()
        }

        fun verifyTestsInMy() {
          takeScreenshot("afterRightClicking")
          popupMenu().findMenuItemByText("Run 'Tests in 'my''").click()
          verifyTestStatus(
            listOf("3 tests passed"),
            setOf("AdditionTest", "testAddition", "MultiplicationTest", "testMultiplication", "DivisionTest", "testDivision"),
          )
          verifyTestOutput()
        }

        fun verifyTestsInMyPackage() {
          takeScreenshot("afterRightClicking")
          popupMenu().findMenuItemByText("Run 'Tests in 'package''").click()
          verifyTestStatus(
            listOf("2 tests passed"),
            setOf("AdditionTest", "testAddition", "MultiplicationTest", "testMultiplication"),
          )
          verifyTestOutput()
        }

        step("Run all tests in root directory") {
          projectView().projectViewTree.rightClickRowAndWaitForMenu("runAllTests", { it.substringBefore(' ') == "runAllTests" }, menu)
          verifyTestsInRoot()
        }

        step("Run all tests in a root/my/package directory") {
          projectView().projectViewTree.run {
            expandPath("runAllTests", "my", fullMatch = false)
            rightClickRowAndWaitForMenu("package", { it.contains("package") }, menu)
          }
          verifyTestsInMyPackage()
        }

        step("Run all tests in root/my toolwindow path") {
          toolWindow("Bazel") { tree().rightClickRowAndWaitForMenu("my", { it.contains("my") }, menu) }
          verifyTestsInMy()
        }

        step("Run all tests in a root/my/package toolwindow path") {
          toolWindow("Bazel") {
            tree().run {
              expandAll()
              rightClickRowAndWaitForMenu("package", { it.contains("package") }, menu)
            }
          }
          verifyTestsInMyPackage()
        }
      }
    }
  }

  private fun IdeaFrameUI.verifyTestOutput() {
    step("Verify stdout and stderr in the test results console") {
      testTreeView().clickRow { it.startsWith("Test Results") }
      val testResults = x { byType("com.intellij.execution.testframework.sm.runner.ui.SMTestRunnerResultsForm") }
      val console = testResults.x { byType("com.intellij.execution.impl.ConsoleViewImpl") }
      val expectedOutput = listOf("AdditionTest stdout", "AdditionTest stderr")
      waitFor(
        message = "The test results console contains stdout and stderr",
        timeout = 30.seconds,
        getter = { console.editor().text },
        checker = { text -> expectedOutput.all { it in text } },
      )
    }
  }
}
