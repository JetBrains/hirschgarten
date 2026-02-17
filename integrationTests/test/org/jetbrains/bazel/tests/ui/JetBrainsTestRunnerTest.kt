package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.assertCaretPosition
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class JetBrainsTestRunnerTest : IdeStarterBaseProjectTest() {

  @Test
  fun `JetBrains test runner should execute tests and show results tree`() {
    createContext("runAllTestsAction", IdeaBazelCases.JetBrainsTestRunner)
      .setRunConfigRunWithBazel(false)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
      ideFrame {
        syncBazelProject(buildAndSync = true)
        waitForIndicators(10.minutes)

        step("open TestKotlin.kt and run TestKotlin.` interesting#test `") {
          execute { openFile("TestKotlin.kt") }
          clickTestGutterOnLine(11)

          verifyTestStatus(
            listOf("1 test passed"),
            listOf("JUnit Jupiter", "TestKotlin", "interesting#test ()"),
          )
        }

        step("run the same again to check that it is NOT cached because we use --script_path in this test") {
          execute { openFile("TestKotlin.kt") }
          clickTestGutterOnLine(11)

          verifyTestStatus(
            listOf("1 test passed"),
            listOf("JUnit Jupiter", "TestKotlin", "interesting#test ()"),
          )
        }

        step("open TestJava.java and run the whole class") {
          execute { openFile("TestJava.java") }
          clickTestGutterOnLine(14, testTimeout = 15.seconds)

          verifyTestStatus(
            listOf("3 tests failed,", " 2 passed"),
            listOf(
              "JUnit Jupiter",
              "TestJava",
              "medium fail 3",
              "testWithClasses(Class)",
              "[1] myClass=class java.lang.String",
              "[2] myClass=interface java.lang.Comparable",
              "Nested group",
              "nested pass 9",
              "nested fail 10",
            ),
          )
        }

        step("Rerun failed tests in TestJava.java") {
          execute { openFile("TestJava.java") }
          x { byAccessibleName("Rerun Failed Tests") }.click()
          wait(15.seconds)
          verifyTestStatus(
            listOf("3 tests failed"),
            listOf(
              "JUnit Jupiter",
              "TestJava",
              "medium fail 3",
              "testWithClasses(Class)",
              "[2] myClass=interface java.lang.Comparable",
              "Nested group",
              "nested fail 10",
            ),
          )
        }

        step("Run parametrized test via gutter") {
          execute { openFile("TestJava.java") }
          clickTestGutterOnLine(17)

          verifyTestStatus(
            listOf("1 test failed,", " 1 passed"),
            listOf(
              "JUnit Jupiter",
              "TestJava",
              "testWithClasses(Class)",
              "[1] myClass=class java.lang.String",
              "[2] myClass=interface java.lang.Comparable",
            ),
          )
        }

        step("Run individual test via right-click on test results tree") {
          val testTreeView = testTreeView()
          testTreeView.rightClickRow { it.startsWith("[1] myClass=class java.lang.String") }
          popup().waitOneText("Run 'testWithClasses'").click()
          wait(10.seconds)
          verifyTestStatus(
            listOf("1 test passed"),
            listOf(
              "JUnit Jupiter",
              "TestJava",
              "testWithClasses(Class)",
              "[1] myClass=class java.lang.String",
            ),
          )
        }

        step("Double-click result in test tree and check cursor position") {
          val testTreeView = testTreeView()
          testTreeView.doubleClickRow { it.startsWith("[1] myClass=class java.lang.String") }
          execute { assertCaretPosition(18, 8) }
          wait(3.seconds)
        }
      }
    }
  }

  @Test
  fun `test results should be cached when running with Bazel`() {
    createContext("runAllTestsAction", IdeaBazelCases.JetBrainsTestRunner)
      // This is required for Bazel test caching!
      .setRunConfigRunWithBazel(true)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(buildAndSync = true)
          waitForIndicators(10.minutes)

          step("open TestKotlin.kt and run TestKotlin.` interesting#test `") {
            execute { openFile("TestKotlin.kt") }
            clickTestGutterOnLine(11)

            verifyTestStatus(
              listOf("1 test passed"),
              listOf("JUnit Jupiter", "TestKotlin", "interesting#test ()"),
            )
          }

          step("run the same again to check that it's cached") {
            execute { openFile("TestKotlin.kt") }
            clickTestGutterOnLine(11)

            verifyTestStatus(
              listOf("1 test passed"),
              listOf("JUnit Jupiter", "TestKotlin", "interesting#test () (cached)"),
            )
          }
        }
      }
  }

  /**
   * [line] can be different depending on e.g. imports folding
   */
  private fun IdeaFrameUI.clickTestGutterOnLine(line: Int, testTimeout: Duration = 10.seconds) {
    val runGutter = editorTabs()
      .gutter()
      .getGutterIcons()
      .filter { it.line == line }
      // Run gutter icons can interfere with annotations, but they are displayed first
      .minByOrNull { it.location.x }
    if (runGutter == null) {
      error("Couldn't find a run gutter on line $line")
    }
    runGutter.click()
    popup().waitOneText { it.text.startsWith("Test ") }.click()
    wait(testTimeout)
  }
}
