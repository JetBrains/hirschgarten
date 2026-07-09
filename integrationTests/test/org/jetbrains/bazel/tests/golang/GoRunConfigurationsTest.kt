package org.jetbrains.bazel.tests.golang

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.debugToolWindow
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.removeAllBreakpoints
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.data.GoLandBazelCases
import org.jetbrains.bazel.tests.combined.IdeStarterCombinedBaseTest
import org.jetbrains.bazel.tests.run.selectRunConfiguration
import org.jetbrains.bazel.tests.ui.clickRunGutterOnLine
import org.jetbrains.bazel.tests.ui.clickTestGutterOnLine
import org.jetbrains.bazel.tests.ui.debuggerFramesUi
import org.jetbrains.bazel.tests.ui.verifyTestStatus
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class GoRunConfigurationsTest : IdeStarterCombinedBaseTest() {
  override fun createContext(): IDETestContext =
    createContext("goRunConfigurationsTest", GoLandBazelCases.GoRunConfigurationsTest)

  @Test
  @Order(1)
  fun `debugging main_binary has correct program arguments and env vars`() {
    withDriver(bgRun) {
      ideFrame {
        step("Select //:main_binary configuration") {
          selectRunConfiguration(targetText = "//:main_binary")
        }

        step("Run with debug") { x { byAccessibleName("Debug '//:main_binary'") }.click() }

        val consoleView = x { byClass("ConsoleViewImpl") }
        step("Wait for run config to finish") {
          consoleView.waitFound(timeout = 3.minutes)
          consoleView.waitContainsText("Execution finished", timeout = 3.minutes)
        }

        step("Check that env vars and program arguments are passed") {
          consoleView.waitContainsText("Program arguments: [example_arg=example_value another_arg=another_value]", timeout = 5.seconds)
          consoleView.waitContainsText("Environment variable EXAMPLE_ENV: EXAMPLE_VALUE", timeout = 5.seconds)
        }
      }
    }
  }

  @Test
  @Order(2)
  fun `run gutters run only the selected Go test and show it in the results tree`() {
    withDriver(bgRun) {
      ideFrame {
        step("Open lib/lib_test.go") {
          execute { openFile("lib/lib_test.go") }
        }

        step("Run tests in package via its run gutter and check results") {
          clickTestGutterOnLine(0)
          verifyTestStatus(
            expectedStatus = listOf("2 tests passed"),
            expectedTree = listOf("goruntest/lib.TestAdd", "TestAdd", "goruntest/lib.TestSubtract", "TestSubtract")
          )
          takeScreenshot("goRunPackageGutterResults")
        }

        step("Run TestAdd via its run gutter and check results") {
          clickTestGutterOnLine(4)
          verifyTestStatus(expectedStatus = listOf("1 test passed"), expectedTree = listOf("goruntest/lib.TestAdd", "TestAdd"))
          takeScreenshot("goRunTestAddGutterResults")
        }

        step("Run TestSubtract via its run gutter and check results") {
          clickTestGutterOnLine(10)
          verifyTestStatus(expectedStatus = listOf("1 test passed"), expectedTree = listOf("goruntest/lib.TestSubtract", "TestSubtract"))
          takeScreenshot("goRunTestSubtractGutterResults")
        }
      }
    }
  }

  @Test
  @Order(3)
  fun `debug gutters stop at breakpoints and debug only the selected tests`() {
    withDriver(bgRun) {
      ideFrame {
        step("Open lib/lib_test.go") {
          execute { openFile("lib/lib_test.go") }
        }
        testDebuggingSingleFunction(name = "TestAdd", gutterLine = 4)
        testDebuggingSingleFunction(name = "TestSubtract", gutterLine = 10)
        step("Set all breaking points") {
          execute { setBreakpoint(line = 6, relativePath = "lib/lib_test.go") }
          execute { setBreakpoint(line = 12, relativePath = "lib/lib_test.go") }
        }
        step("Debug tests in package via its run gutter") {
          clickRunGutterOnLine(0)
          popup().waitOneContainsText("Debug '//lib:lib_test'").click()
        }
        step("Debugger stops at all breakpoints") {
          repeat(2) { i ->
            debuggerFramesUi.waitAnyTexts { it.text.contains("TestAdd") || it.text.contains("TestSubtract") }
            takeScreenshot("goDebugPausedAt$i")
            debugToolWindow().resumeButton.click()
          }
        }
        step("Verify results") {
          verifyTestStatus(
            expectedStatus = listOf("2 tests passed"),
            expectedTree = listOf("TestAdd", "TestSubtract")
          )
        }
      }
    }
  }

  private fun Driver.testDebuggingSingleFunction(
    name: String,
    gutterLine: Int,
    breakpointLine: Int = gutterLine + 2
  ) = ideFrame {
    step("Set breakpoint inside $name") {
      execute { setBreakpoint(line = breakpointLine, relativePath = "lib/lib_test.go") }
    }
    step("Debug $name via its run gutter") {
      clickRunGutterOnLine(gutterLine)
      popup().waitOneContainsText("Debug '//lib:lib_test'").click()
    }
    step("Debugger stops at the breakpoint inside $name and resumes") {
      debuggerFramesUi.waitAnyTexts { it.text.contains(name) }
      takeScreenshot("goDebug${name}PausedAtBreakpoint")
      debugToolWindow().resumeButton.click()
    }
    step("Only $name was debugged and it is shown in the results tree") {
      verifyTestStatus(expectedStatus = listOf("1 test passed"), expectedTree = listOf(name))
      takeScreenshot("goDebug${name}Results")
    }
    execute { removeAllBreakpoints() }
  }
}
