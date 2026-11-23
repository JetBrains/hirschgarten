package org.jetbrains.bazel.tests.ui

import com.intellij.driver.client.service
import com.intellij.driver.model.OnDispatcher
import com.intellij.driver.model.RemoteMouseButton
import com.intellij.driver.sdk.FileEditorManager
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.wait
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.data.DevKitBazelCases
import org.jetbrains.bazel.data.simpleBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val JETBRAINS_TEST_RUNNER_PROJECT = simpleBazelProject(
  path = "jetbrainsTestRunner",
)

class JetBrainsTestRunnerTest : IdeStarterBaseProjectTest() {

  @Test
  fun `JetBrains test runner should execute tests and show results tree`() {
    createContext("jetBrainsTestRunner", DevKitBazelCases.withProject(JETBRAINS_TEST_RUNNER_PROJECT))
      .setRunConfigRunWithBazel(false)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
      ideFrame {
        syncBazelProject(buildAndSync = true)
        waitForIndicators(10.minutes)

        step("open TestKotlin.kt and run TestKotlin.` interesting#test `") {
          execute { openFile("custom/TestKotlin.kt") }
          clickTestGutterOnLine(11)

          verifyTestStatus(
            listOf("1 test passed"),
            listOf("TestKotlin", "interesting#test ()"),
          )
        }

        step("run the same again to check that it is NOT cached because we use --script_path in this test") {
          execute { openFile("custom/TestKotlin.kt") }
          clickTestGutterOnLine(11)

          verifyTestStatus(
            listOf("1 test passed"),
            listOf("TestKotlin", "interesting#test ()"),
          )
        }

        step("open TestJava.java and run the whole class") {
          execute { openFile("custom/TestJava.java") }
          clickTestGutterOnLine(14)

          verifyTestStatus(
            listOf("3 tests failed,", " 2 passed"),
            listOf(
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
          execute { openFile("custom/TestJava.java") }
          x { byAccessibleName("Rerun Failed Tests") }.click()
          verifyTestStatus(
            listOf("3 tests failed"),
            listOf(
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
          execute { openFile("custom/TestJava.java") }
          clickTestGutterOnLine(17)

          verifyTestStatus(
            listOf("1 test failed,", " 1 passed"),
            listOf(
              "TestJava",
              "testWithClasses(Class)",
              "[1] myClass=class java.lang.String",
              "[2] myClass=interface java.lang.Comparable",
            ),
          )
        }

        step("Run individual test via right-click on test results tree") {
          val testTreeView = testTreeView()
          testTreeView.rightClickRowAndWaitForMenu("[1] myClass=class java.lang.String", { it.startsWith("[1] myClass=class java.lang.String") }) { popupMenu() }
          popup().waitOneText("Run 'testWithClasses'").click()
          wait(10.seconds)
          verifyTestStatus(
            listOf("1 test passed"),
            listOf(
              "TestJava",
              "testWithClasses(Class)",
              "[1] myClass=class java.lang.String",
            ),
          )
        }

        step("Double-click the test result and check the file and caret") {
          execute { openFile("custom/TestKotlin.kt") }
          val testTreeView = testTreeView()
          val row = testTreeView.waitOneText("[1] myClass=class java.lang.String")
          testTreeView.robot.postGesture(testTreeView.component, row.point, RemoteMouseButton.LEFT, 2)

          val editorManager = driver.service<FileEditorManager>(driver.singleProject())
          waitFor(
            message = "The test result opens TestJava.java at line 18, column 8",
            timeout = 15.seconds,
            errorMessage = { location ->
              val selection = testTreeView.collectSelectedPaths().map { it.path }
              "Double-click '${row.text}'. Selected rows: $selection. Editor: $location"
            },
            getter = {
              driver.withContext(OnDispatcher.EDT) {
                editorManager.getSelectedTextEditor()?.let { editor ->
                  val caret = editor.getCaretModel().getLogicalPosition()
                  EditorLocation(editor.getVirtualFile().getPath(), caret.getLine() + 1, caret.getColumn() + 1)
                }
              }
            },
            checker = { it != null && it.path.endsWith("/custom/TestJava.java") && it.line == 18 && it.column == 8 },
          )
        }

        step("Disabled tests are shown in test tree") {
          execute { openFile("custom/TestDisabled.kt") }
          clickTestGutterOnLine(3)

          verifyTestStatus(
            listOf("1 test passed, ", "1 ignored"),
            listOf(
              "TestDisabled",
              "normalTest()",
              "disabledTest()",
            ),
          )
        }

        step("Disabled test is run when launched explicitly") {
          execute { openFile("custom/TestDisabled.kt") }
          clickTestGutterOnLine(11)

          verifyTestStatus(
            listOf("1 test passed"),
            listOf(
              "TestDisabled",
              "disabledTest()",
            ),
          )
        }
      }
    }
  }

  @Test
  fun `test results should be cached when running with Bazel`() {
    createContext("jetBrainsTestRunnerCached", DevKitBazelCases.withProject(JETBRAINS_TEST_RUNNER_PROJECT))
      // This is required for Bazel test caching!
      .setRunConfigRunWithBazel(true)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(buildAndSync = true)
          waitForIndicators(10.minutes)

          step("open TestKotlin.kt and run TestKotlin.` interesting#test `") {
            execute { openFile("custom/TestKotlin.kt") }
            clickTestGutterOnLine(11)

            verifyTestStatus(
              listOf("1 test passed"),
              listOf("TestKotlin", "interesting#test ()"),
            )
          }

          step("run the same again to check that it's cached") {
            execute { openFile("custom/TestKotlin.kt") }
            clickTestGutterOnLine(11)

            verifyTestStatus(
              listOf("1 test passed"),
              listOf("TestKotlin", "interesting#test () (cached)"),
            )
          }
        }
      }
  }

  @Test
  fun `a regular test target is not run with the JetBrains test runner`() {
    createContext(
      "regularTestRunner",
      DevKitBazelCases.withProject(JETBRAINS_TEST_RUNNER_PROJECT),
    )
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(buildAndSync = true)
          waitForIndicators(10.minutes)

          step("Run the whole RegularTest class") {
            execute { openFile("regular/RegularTest.java") }
            clickTestGutterOnLine(7)

            verifyTestStatus(
              listOf("1 test failed,", " 1 passed"),
              listOf("RegularTest", "regularFail", "regularPass"),
            )
          }

          // Running twice also covers Bazel test caching: the " (cached)" suffix is added by the
          // JetBrains test runner listener only, so a regular target must never be decorated with it.
          repeat(2) { run ->
            step("Run a single RegularTest method via gutter (run #${run + 1})") {
              execute { openFile("regular/RegularTest.java") }
              clickTestGutterOnLine(15)

              verifyTestStatus(
                listOf("1 test passed"),
                listOf("RegularTest", "regularPass"),
              )
            }
          }
        }
      }
  }

  private data class EditorLocation(val path: String, val line: Int, val column: Int)
}
